/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.sdk.sdkman;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Artifact;
import org.jreleaser.model.Distribution;
import org.jreleaser.model.JReleaserCommand;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.Sdkman;
import org.jreleaser.model.announcer.spi.AnnounceException;
import org.jreleaser.model.announcer.spi.Announcer;
import org.jreleaser.util.Constants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jreleaser.util.Constants.MAGIC_SET;
import static org.jreleaser.util.MustacheUtils.applyTemplate;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;
import static org.jreleaser.util.StringUtils.isTrue;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@org.jreleaser.infra.nativeimage.annotations.NativeImage
public class SdkmanAnnouncer implements Announcer {
    private final JReleaserContext context;

    SdkmanAnnouncer(JReleaserContext context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return org.jreleaser.model.SdkmanAnnouncer.NAME;
    }

    @Override
    public boolean isEnabled() {
        return context.getModel().getAnnounce().getSdkman().isEnabled();
    }

    @Override
    public void announce() throws AnnounceException {
        Map<String, Distribution> distributions = context.getModel().getActiveDistributions().stream()
            .filter(context::isDistributionIncluded)
            .filter(d -> d.getSdkman().isEnabled())
            .filter(d -> !JReleaserCommand.supportsPublish(context.getCommand()) || d.getSdkman().isPublished())
            .collect(Collectors.toMap(distribution -> {
                Sdkman sdkman = distribution.getSdkman();
                return isNotBlank(sdkman.getCandidate()) ? sdkman.getCandidate().trim() : context.getModel().getProject().getName();
            }, distribution -> distribution));

        Boolean set = (Boolean) context.getModel().getAnnounce().getSdkman().getExtraProperties().remove(MAGIC_SET);
        if (distributions.isEmpty()) {
            if (set == null || !set) {
                announceProject();
            } else {
                context.getLogger().debug(RB.$("announcers.announcer.disabled"));
            }
            return;
        }

        boolean failures = false;
        for (Map.Entry<String, Distribution> e : distributions.entrySet()) {
            String candidate = e.getKey();
            Distribution distribution = e.getValue();

            Sdkman sdkman = distribution.getSdkman();
            Map<String, Object> props = context.props();
            props.putAll(distribution.props());
            String releaseNotesUrl = applyTemplate(sdkman.getReleaseNotesUrl(), props);
            String command = sdkman.getCommand().name().toLowerCase();

            context.getLogger().info(RB.$("sdkman.release.announce"), command, candidate);
            try {
                AnnounceSdkmanCommand.builder(context.getLogger())
                    .connectTimeout(sdkman.getConnectTimeout())
                    .readTimeout(sdkman.getReadTimeout())
                    .consumerKey(context.isDryrun() ? "**UNDEFINED**" : sdkman.getResolvedConsumerKey())
                    .consumerToken(context.isDryrun() ? "**UNDEFINED**" : sdkman.getResolvedConsumerToken())
                    .candidate(candidate)
                    .version(context.getModel().getProject().getVersion())
                    .releaseNotesUrl(releaseNotesUrl)
                    .dryrun(context.isDryrun())
                    .build()
                    .execute();
            } catch (SdkmanException x) {
                context.getLogger().warn(x.getMessage().trim());
                failures = true;
            }
        }

        if (failures) {
            throw new AnnounceException(RB.$("ERROR_sdkman_announce"));
        }
    }

    private void announceProject() throws AnnounceException {
        org.jreleaser.model.SdkmanAnnouncer sdkman = context.getModel().getAnnounce().getSdkman();

        Map<String, String> platforms = new LinkedHashMap<>();
        // collect artifacts by supported SDKMAN! platform
        for (Distribution distribution : context.getModel().getActiveDistributions()) {
            if (!context.isDistributionIncluded(distribution) ||
                !isDistributionSupported(distribution)) {
                continue;
            }
            for (Artifact artifact : distribution.getArtifacts()) {
                if (!artifact.isActive()) continue;
                // only zips are supported
                if (!artifact.getPath().endsWith(".zip")) {
                    context.getLogger().debug(RB.$("sdkman.no.artifacts.match"),
                        artifact.getEffectivePath(context, distribution).getFileName());
                    continue;
                }

                if (isTrue(artifact.getExtraProperties().get("skipSdkman"))) {
                    context.getLogger().debug(RB.$("sdkman.artifact.explicit.skip"),
                        artifact.getEffectivePath(context, distribution).getFileName());
                    continue;
                }

                String platform = mapPlatform(artifact.getPlatform());
                String url = artifactUrl(distribution, artifact);
                if (platforms.containsKey(platform)) {
                    context.getLogger().warn(RB.$("sdkman.platform.replacement"), platform, url, platforms.get(platform));
                }
                platforms.put(platform, url);
            }
        }

        if (platforms.isEmpty()) {
            context.getLogger().warn(RB.$("sdkman.no.suitable.artifacts"));
            return;
        }

        try {
            String candidate = isNotBlank(sdkman.getCandidate()) ? sdkman.getCandidate().trim() : context.getModel().getProject().getName();
            String releaseNotesUrl = applyTemplate(sdkman.getReleaseNotesUrl(), context.props());

            if (sdkman.isMajor()) {
                context.getLogger().info(RB.$("sdkman.release.announce.major"), candidate);
                MajorReleaseSdkmanCommand.builder(context.getLogger())
                    .connectTimeout(sdkman.getConnectTimeout())
                    .readTimeout(sdkman.getReadTimeout())
                    .consumerKey(context.isDryrun() ? "**UNDEFINED**" : sdkman.getResolvedConsumerKey())
                    .consumerToken(context.isDryrun() ? "**UNDEFINED**" : sdkman.getResolvedConsumerToken())
                    .candidate(candidate)
                    .version(context.getModel().getProject().getVersion())
                    .platforms(platforms)
                    .releaseNotesUrl(releaseNotesUrl)
                    .dryrun(context.isDryrun())
                    .build()
                    .execute();
            } else {
                context.getLogger().info(RB.$("sdkman.release.announce.minor"), candidate);
                MinorReleaseSdkmanCommand.builder(context.getLogger())
                    .connectTimeout(sdkman.getConnectTimeout())
                    .readTimeout(sdkman.getReadTimeout())
                    .consumerKey(context.isDryrun() ? "**UNDEFINED**" : sdkman.getResolvedConsumerKey())
                    .consumerToken(context.isDryrun() ? "**UNDEFINED**" : sdkman.getResolvedConsumerToken())
                    .candidate(candidate)
                    .version(context.getModel().getProject().getVersion())
                    .platforms(platforms)
                    .releaseNotesUrl(releaseNotesUrl)
                    .dryrun(context.isDryrun())
                    .build()
                    .execute();
            }
        } catch (SdkmanException e) {
            throw new AnnounceException(e);
        }
    }

    private boolean isDistributionSupported(Distribution distribution) {
        return (distribution.getType() == Distribution.DistributionType.JAVA_BINARY ||
            distribution.getType() == Distribution.DistributionType.JLINK ||
            distribution.getType() == Distribution.DistributionType.NATIVE_IMAGE) &&
            !isTrue(distribution.getExtraProperties().get("skipSdkman"));
    }

    private String mapPlatform(String platform) {
        /*
           SDKMAN! supports the following platform mappings
           - LINUX_64
           - LINUX_32
           - LINUX_ARM32
           - LINUX_ARM64
           - MAC_OSX
           - MAC_ARM64
           - WINDOWS_64
           - UNIVERSAL
         */

        if (isBlank(platform)) {
            return "UNIVERSAL";
        }
        if (platform.contains("mac") || platform.contains("osx")) {
            return platform.contains("aarch_64") ? "MAC_ARM64" : "MAC_OSX";
        } else if (platform.contains("win")) {
            return "WINDOWS_64";
        } else if (platform.contains("linux")) {
            if (platform.contains("x86_32")) return "LINUX_32";
            if (platform.contains("x86_64")) return "LINUX_64";
            if (platform.contains("arm_32")) return "LINUX_ARM32";
            if (platform.contains("aarch_64")) return "LINUX_ARM64";
            return "LINUX_32";
        }

        return null;
    }

    private String artifactUrl(Distribution distribution, Artifact artifact) {
        Map<String, Object> newProps = context.props();
        newProps.put(Constants.KEY_ARTIFACT_FILE, artifact.getEffectivePath(context, distribution).getFileName().toString());
        return applyTemplate(context.getModel().getRelease().getGitService().getDownloadUrl(), newProps, "downloadUrl");
    }
}
