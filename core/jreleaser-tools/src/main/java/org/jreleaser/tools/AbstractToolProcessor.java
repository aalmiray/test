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
package org.jreleaser.tools;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Artifact;
import org.jreleaser.model.Distribution;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.Tool;
import org.jreleaser.model.tool.spi.ToolProcessingException;
import org.jreleaser.model.tool.spi.ToolProcessor;
import org.jreleaser.model.util.Artifacts;
import org.jreleaser.util.Algorithm;
import org.jreleaser.util.FileType;
import org.jreleaser.util.FileUtils;
import org.jreleaser.util.command.Command;
import org.jreleaser.util.command.CommandException;
import org.jreleaser.util.command.CommandExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.jreleaser.util.Constants.KEY_ARTIFACT_ARCH;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_FILE;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_FILE_EXTENSION;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_FILE_FORMAT;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_FILE_NAME;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_NAME;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_OS;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_PLATFORM;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_PLATFORM_REPLACED;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_SIZE;
import static org.jreleaser.util.Constants.KEY_ARTIFACT_VERSION;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_ARCH;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_FILE;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_FILE_EXTENSION;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_FILE_FORMAT;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_FILE_NAME;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_NAME;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_OS;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_PLATFORM;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_PLATFORM_REPLACED;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_SIZE;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_ARTIFACT_VERSION;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_PACKAGE_DIRECTORY;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_PREPARE_DIRECTORY;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_SHA_256;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_SIZE;
import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_URL;
import static org.jreleaser.util.Constants.KEY_REVERSE_REPO_HOST;
import static org.jreleaser.util.MustacheUtils.applyTemplate;
import static org.jreleaser.util.MustacheUtils.applyTemplates;
import static org.jreleaser.util.StringUtils.capitalize;
import static org.jreleaser.util.StringUtils.getFilename;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@org.jreleaser.infra.nativeimage.annotations.NativeImage
abstract class AbstractToolProcessor<T extends Tool> implements ToolProcessor<T> {
    protected final JReleaserContext context;
    protected T tool;

    protected AbstractToolProcessor(JReleaserContext context) {
        this.context = context;
    }

    @Override
    public T getTool() {
        return tool;
    }

    @Override
    public void setTool(T tool) {
        this.tool = tool;
    }

    @Override
    public String getToolName() {
        return tool.getName();
    }

    @Override
    public boolean supportsDistribution(Distribution distribution) {
        return true;
    }

    @Override
    public void prepareDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        try {
            String distributionName = distribution.getName();
            context.getLogger().debug(RB.$("tool.create.properties"), distributionName, getToolName());
            Map<String, Object> newProps = fillProps(distribution, props);
            if (newProps.isEmpty()) {
                context.getLogger().warn(RB.$("tool.skip.distribution"), distributionName);
                return;
            }

            doPrepareDistribution(distribution, newProps);
        } catch (RuntimeException e) {
            throw new ToolProcessingException(e);
        }
    }

    protected abstract void doPrepareDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException;

    @Override
    public void packageDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        try {
            String distributionName = distribution.getName();
            context.getLogger().debug(RB.$("tool.create.properties"), distributionName, getToolName());
            Map<String, Object> newProps = fillProps(distribution, props);
            if (newProps.isEmpty()) {
                context.getLogger().warn(RB.$("tool.skip.distribution"), distributionName);
                return;
            }

            doPackageDistribution(distribution, newProps);
        } catch (IllegalArgumentException e) {
            throw new ToolProcessingException(e);
        }
    }

    @Override
    public void publishDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        if (context.getModel().getProject().isSnapshot() && !tool.isSnapshotSupported()) {
            context.getLogger().info(RB.$("tool.publish.snapshot.not.supported"));
            return;
        }

        try {
            String distributionName = distribution.getName();
            context.getLogger().debug(RB.$("tool.create.properties"), distributionName, getToolName());
            Map<String, Object> newProps = fillProps(distribution, props);
            if (newProps.isEmpty()) {
                context.getLogger().warn(RB.$("tool.skip.distribution"), distributionName);
                return;
            }

            doPublishDistribution(distribution, newProps);
        } catch (IllegalArgumentException e) {
            throw new ToolProcessingException(e);
        }
    }

    protected abstract void doPackageDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException;

    protected abstract void doPublishDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException;

    protected Map<String, Object> fillProps(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        Map<String, Object> newProps = new LinkedHashMap<>(props);
        context.getLogger().debug(RB.$("tool.fill.distribution.properties"));
        fillDistributionProperties(newProps, distribution);
        context.getLogger().debug(RB.$("tool.fill.git.properties"));
        context.getModel().getRelease().getGitService().fillProps(newProps, context.getModel());
        context.getLogger().debug(RB.$("tool.fill.artifact.properties"));
        if (!verifyAndAddArtifacts(newProps, distribution)) {
            // we can't continue with this tool
            return Collections.emptyMap();
        }
        context.getLogger().debug(RB.$("tool.fill.tool.properties"));
        fillToolProperties(newProps, distribution);
        applyTemplates(newProps, tool.getResolvedExtraProperties());
        if (isBlank(context.getModel().getRelease().getGitService().getReverseRepoHost())) {
            newProps.put(KEY_REVERSE_REPO_HOST,
                tool.getExtraProperties().get(KEY_REVERSE_REPO_HOST));
        }
        return newProps;
    }

    protected void fillDistributionProperties(Map<String, Object> props, Distribution distribution) {
        props.putAll(distribution.props());
    }

    protected abstract void fillToolProperties(Map<String, Object> props, Distribution distribution) throws ToolProcessingException;

    protected void executeCommand(Path directory, Command command) throws ToolProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommand(directory, command);
            if (exitValue != 0) {
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new ToolProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void executeCommand(Command command) throws ToolProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommand(command);
            if (exitValue != 0) {
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new ToolProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void executeCommandCapturing(Command command, OutputStream out) throws ToolProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommandCapturing(command, out);
            if (exitValue != 0) {
                context.getLogger().error(out.toString().trim());
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new ToolProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void executeCommandWithInput(Command command, InputStream in) throws ToolProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommandWithInput(command, in);
            if (exitValue != 0) {
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new ToolProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void copyPreparedFiles(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        Path prepareDirectory = getPrepareDirectory(props);
        Path packageDirectory = getPackageDirectory(props);
        copyFiles(prepareDirectory, packageDirectory);
    }

    protected void copyFiles(Path src, Path dest) throws ToolProcessingException {
        try {
            if (!Files.exists(dest)) {
                Files.createDirectories(dest);
            }

            if (!FileUtils.copyFilesRecursive(context.getLogger(), src, dest)) {
                throw new ToolProcessingException(RB.$("ERROR_copy_files_from_to",
                    context.relativizeToBasedir(src),
                    context.relativizeToBasedir(dest)));
            }
        } catch (IOException e) {
            throw new ToolProcessingException(RB.$("ERROR_unexpected_copy_files_from_to",
                context.relativizeToBasedir(src),
                context.relativizeToBasedir(dest)), e);
        }
    }

    protected boolean verifyAndAddArtifacts(Map<String, Object> props,
                                            Distribution distribution) throws ToolProcessingException {
        return verifyAndAddArtifacts(props, distribution, collectArtifacts(distribution));
    }

    protected boolean verifyAndAddArtifacts(Map<String, Object> props,
                                            Distribution distribution,
                                            List<Artifact> artifacts) throws ToolProcessingException {
        List<Artifact> activeArtifacts = artifacts.stream()
            .filter(Artifact::isActive)
            .collect(Collectors.toList());

        if (activeArtifacts.size() == 0) {
            // we can't proceed
            context.getLogger().warn(RB.$("tool.no.matching.artifacts"),
                distribution.getName(), capitalize(tool.getName()));
            return false;
        }

        for (int i = 0; i < activeArtifacts.size(); i++) {
            Artifact artifact = activeArtifacts.get(i);
            String platform = artifact.getPlatform();
            String artifactPlatform = isNotBlank(platform) ? capitalize(platform) : "";
            String platformReplaced = distribution.getPlatform().applyReplacements(platform);
            String artifactPlatformReplaced = isNotBlank(platformReplaced) ? capitalize(platformReplaced) : "";
            // add extra properties without clobbering existing keys
            Map<String, Object> artifactProps = artifact.getResolvedExtraProperties("artifact" + artifactPlatform);
            artifactProps.keySet().stream()
                .filter(k -> !props.containsKey(k))
                .forEach(k -> props.put(k, artifactProps.get(k)));

            Path artifactPath = artifact.getEffectivePath(context, distribution);

            long artifactSize = 0;
            try {
                artifactSize = Files.size(artifactPath);
            } catch (IOException ignored) {
                // this would be strange
                context.getLogger().trace(ignored);
            }

            String artifactFile = artifact.getEffectivePath().getFileName().toString();
            String artifactFileName = getFilename(artifactFile, FileType.getSupportedExtensions());
            String artifactFileExtension = artifactFile.substring(artifactFileName.length());
            String artifactFileFormat = artifactFileExtension.substring(1);

            String artifactName = "";
            String artifactVersion = "";
            String projectVersion = context.getModel().getProject().getEffectiveVersion();
            if (isNotBlank(projectVersion) && artifactFileName.contains(projectVersion)) {
                artifactName = artifactFileName.substring(0, artifactFileName.indexOf(projectVersion));
                if (artifactName.endsWith("-")) {
                    artifactName = artifactName.substring(0, artifactName.length() - 1);
                }
                artifactVersion = projectVersion;
            }
            projectVersion = context.getModel().getProject().getVersion();
            if (isBlank(artifactName) && isNotBlank(projectVersion) && artifactFileName.contains(projectVersion)) {
                artifactName = artifactFileName.substring(0, artifactFileName.indexOf(projectVersion));
                if (artifactName.endsWith("-")) {
                    artifactName = artifactName.substring(0, artifactName.length() - 1);
                }
                artifactVersion = projectVersion;
            }

            String artifactOs = "";
            String artifactArch = "";
            if (isNotBlank(platform)) {
                if (platform.contains("-")) {
                    String[] parts = platform.split("-");
                    artifactOs = parts[0];
                    artifactArch = parts[1];
                }
            }

            safePut(props, "artifact" + artifactPlatform + "Name", artifactName);
            safePut(props, "artifact" + artifactPlatform + "Version", artifactVersion);
            safePut(props, "artifact" + artifactPlatform + "Os", artifactOs);
            safePut(props, "artifact" + artifactPlatform + "Arch", artifactArch);
            safePut(props, "artifact" + artifactPlatform + "File", artifactFile);
            safePut(props, "artifact" + artifactPlatform + "Size", artifactSize);
            safePut(props, "artifact" + artifactPlatform + "FileName", artifactFileName);
            safePut(props, "artifact" + artifactPlatform + "FileExtension", artifactFileExtension);
            safePut(props, "artifact" + artifactPlatform + "FileFormat", artifactFileFormat);

            safePut(props, "artifact" + artifactPlatformReplaced + "Name", artifactName);
            safePut(props, "artifact" + artifactPlatformReplaced + "Version", artifactVersion);
            safePut(props, "artifact" + artifactPlatformReplaced + "Os", artifactOs);
            safePut(props, "artifact" + artifactPlatformReplaced + "Arch", artifactArch);
            safePut(props, "artifact" + artifactPlatformReplaced + "File", artifactFile);
            safePut(props, "artifact" + artifactPlatformReplaced + "Size", artifactSize);
            safePut(props, "artifact" + artifactPlatformReplaced + "FileName", artifactFileName);
            safePut(props, "artifact" + artifactPlatformReplaced + "FileExtension", artifactFileExtension);
            safePut(props, "artifact" + artifactPlatformReplaced + "FileFormat", artifactFileFormat);

            for (Algorithm algorithm : context.getModel().getChecksum().getAlgorithms()) {
                safePut(props, "artifact" + artifactPlatform + "Checksum" + capitalize(algorithm.formatted()), artifact.getHash(algorithm));
                safePut(props, "artifact" + artifactPlatformReplaced + "Checksum" + capitalize(algorithm.formatted()), artifact.getHash(algorithm));
            }
            Map<String, Object> newProps = new LinkedHashMap<>(props);
            Artifacts.artifactProps(artifact, newProps);
            String artifactUrl = applyTemplate(context.getModel().getRelease().getGitService().getDownloadUrl(), newProps);
            safePut(props, "artifact" + artifactPlatform + "Url", artifactUrl);
            safePut(props, "artifact" + artifactPlatformReplaced + "Url", artifactUrl);
            props.putAll(context.getModel().getUpload()
                .resolveDownloadUrls(context, distribution, artifact, "artifact" + artifactPlatform));
            props.putAll(context.getModel().getUpload()
                .resolveDownloadUrls(context, distribution, artifact, "artifact" + artifactPlatformReplaced));

            if (0 == i) {
                props.putAll(context.getModel().getUpload()
                    .resolveDownloadUrls(context, distribution, artifact, "distribution"));
                safePut(props, KEY_DISTRIBUTION_ARTIFACT, artifact);
                safePut(props, KEY_DISTRIBUTION_URL, artifactUrl);
                safePut(props, KEY_DISTRIBUTION_SIZE, artifactSize);
                safePut(props, KEY_DISTRIBUTION_SHA_256, artifact.getHash(Algorithm.SHA_256));
                for (Algorithm algorithm : context.getModel().getChecksum().getAlgorithms()) {
                    safePut(props, "distributionChecksum" + capitalize(algorithm.formatted()), artifact.getHash(algorithm));
                }

                safePut(props, KEY_DISTRIBUTION_ARTIFACT_PLATFORM, platform);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_PLATFORM_REPLACED, platformReplaced);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_NAME, artifactName);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_VERSION, artifactVersion);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_OS, artifactOs);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_ARCH, artifactArch);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_SIZE, artifactSize);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_FILE, artifactFile);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_FILE_NAME, artifactFileName);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_FILE_EXTENSION, artifactFileExtension);
                safePut(props, KEY_DISTRIBUTION_ARTIFACT_FILE_FORMAT, artifactFileFormat);

                safePut(props, KEY_ARTIFACT_PLATFORM, platform);
                safePut(props, KEY_ARTIFACT_PLATFORM_REPLACED, platformReplaced);
                safePut(props, KEY_ARTIFACT_NAME, artifactName);
                safePut(props, KEY_ARTIFACT_VERSION, artifactVersion);
                safePut(props, KEY_ARTIFACT_OS, artifactOs);
                safePut(props, KEY_ARTIFACT_ARCH, artifactArch);
                safePut(props, KEY_ARTIFACT_SIZE, artifactSize);
                safePut(props, KEY_ARTIFACT_FILE, artifactFile);
                safePut(props, KEY_ARTIFACT_FILE_NAME, artifactFileName);
                safePut(props, KEY_ARTIFACT_FILE_EXTENSION, artifactFileExtension);
                safePut(props, KEY_ARTIFACT_FILE_FORMAT, artifactFileFormat);

                // add extra properties without clobbering existing keys
                Map<String, Object> aprops = artifact.getResolvedExtraProperties();
                applyTemplates(aprops, aprops);
                aprops.keySet().stream()
                    .filter(k -> !props.containsKey(k))
                    .forEach(k -> props.put(k, aprops.get(k)));
            }
        }

        return true;
    }

    protected List<Artifact> collectArtifacts(Distribution distribution) {
        List<String> fileExtensions = new ArrayList<>(tool.getSupportedExtensions());

        return distribution.getArtifacts().stream()
            .filter(Artifact::isActive)
            .filter(artifact -> fileExtensions.stream().anyMatch(ext -> artifact.getPath().endsWith(ext)))
            .filter(artifact -> tool.supportsPlatform(artifact.getPlatform()))
            .filter(artifact -> !isSkipped(artifact))
            // sort by platform, then by extension
            .sorted(Artifact.comparatorByPlatform().thenComparingInt(artifact -> {
                String ext = FileType.getFileNameExtension(artifact.getPath());
                return fileExtensions.indexOf(ext);
            }))
            .collect(Collectors.toList());
    }

    protected boolean isSkipped(Artifact artifact) {
        return false;
    }

    protected void info(ByteArrayOutputStream out) {
        log(out, context.getLogger()::info);
    }

    protected void error(ByteArrayOutputStream err) {
        log(err, context.getLogger()::error);
    }

    private void log(ByteArrayOutputStream stream, Consumer<? super String> consumer) {
        String str = stream.toString();
        if (isBlank(str)) return;

        Arrays.stream(str.split(System.lineSeparator()))
            .forEach(consumer);
    }

    protected Path getPrepareDirectory(Map<String, Object> props) {
        return (Path) props.get(KEY_DISTRIBUTION_PREPARE_DIRECTORY);
    }

    protected Path getPackageDirectory(Map<String, Object> props) {
        return (Path) props.get(KEY_DISTRIBUTION_PACKAGE_DIRECTORY);
    }

    protected void safePut(Map<String, Object> dest, String key, Object value) {
        if (value instanceof CharSequence && isNotBlank(String.valueOf(value))) {
            dest.put(key, value);
        } else if (value != null) {
            dest.put(key, value);
        }
    }
}
