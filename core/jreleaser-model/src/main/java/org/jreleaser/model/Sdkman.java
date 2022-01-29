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
package org.jreleaser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jreleaser.util.Env;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.jreleaser.model.Distribution.DistributionType.JAVA_BINARY;
import static org.jreleaser.model.Distribution.DistributionType.JLINK;
import static org.jreleaser.model.Distribution.DistributionType.NATIVE_IMAGE;
import static org.jreleaser.util.CollectionUtils.newSet;
import static org.jreleaser.util.Constants.HIDE;
import static org.jreleaser.util.Constants.UNSET;
import static org.jreleaser.util.FileType.ZIP;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isFalse;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.6.0
 */
public class Sdkman extends AbstractPackager implements TimeoutAware {
    public static final String SDKMAN_CONSUMER_KEY = "SDKMAN_CONSUMER_KEY";
    public static final String SDKMAN_CONSUMER_TOKEN = "SDKMAN_CONSUMER_TOKEN";
    public static final String TYPE = "sdkman";
    public static final String SKIP_SDKMAN = "skipSdkman";

    private static final Map<Distribution.DistributionType, Set<String>> SUPPORTED = new LinkedHashMap<>();

    static {
        Set<String> extensions = newSet(ZIP.extension());
        SUPPORTED.put(JAVA_BINARY, extensions);
        SUPPORTED.put(JLINK, extensions);
        SUPPORTED.put(NATIVE_IMAGE, extensions);
    }

    protected Command command;
    private String candidate;
    private String releaseNotesUrl;
    private String consumerKey;
    private String consumerToken;
    private int connectTimeout;
    private int readTimeout;
    @JsonIgnore
    private boolean published;

    public Sdkman() {
        super(TYPE);
    }

    void setAll(Sdkman sdkman) {
        super.setAll(sdkman);
        this.candidate = sdkman.candidate;
        this.releaseNotesUrl = sdkman.releaseNotesUrl;
        this.command = sdkman.command;
        this.consumerKey = sdkman.consumerKey;
        this.consumerToken = sdkman.consumerToken;
        this.connectTimeout = sdkman.connectTimeout;
        this.readTimeout = sdkman.readTimeout;
        this.published = sdkman.published;
    }

    public String getResolvedConsumerKey() {
        return Env.resolve(SDKMAN_CONSUMER_KEY, consumerKey);
    }

    public String getResolvedConsumerToken() {
        return Env.resolve(SDKMAN_CONSUMER_TOKEN, consumerToken);
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getReleaseNotesUrl() {
        return releaseNotesUrl;
    }

    public void setReleaseNotesUrl(String releaseNotesUrl) {
        this.releaseNotesUrl = releaseNotesUrl;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setCommand(String str) {
        this.command = Command.of(str);
    }

    public boolean isCommandSet() {
        return command != null;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerToken() {
        return consumerToken;
    }

    public void setConsumerToken(String consumerToken) {
        this.consumerToken = consumerToken;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    protected void asMap(boolean full, Map<String, Object> props) {
        props.put("candidate", candidate);
        props.put("command", command);
        props.put("releaseNotesUrl", releaseNotesUrl);
        props.put("connectTimeout", connectTimeout);
        props.put("readTimeout", readTimeout);
        props.put("consumerKey", isNotBlank(getResolvedConsumerKey()) ? HIDE : UNSET);
        props.put("consumerToken", isNotBlank(getResolvedConsumerToken()) ? HIDE : UNSET);
    }

    @Override
    public boolean supportsPlatform(String platform) {
        return true;
    }

    @Override
    public boolean supportsDistribution(Distribution distribution) {
        return SUPPORTED.containsKey(distribution.getType());
    }

    @Override
    public Set<String> getSupportedExtensions(Distribution distribution) {
        return SUPPORTED.getOrDefault(distribution.getType(), Collections.emptySet());
    }

    @Override
    protected boolean isNotSkipped(Artifact artifact) {
        return isFalse(artifact.getExtraProperties().get(SKIP_SDKMAN));
    }

    public enum Command {
        MAJOR,
        MINOR;

        public String toString() {
            return name().toLowerCase();
        }

        public static Command of(String str) {
            if (isBlank(str)) return null;
            return Command.valueOf(str.toUpperCase().trim());
        }
    }
}
