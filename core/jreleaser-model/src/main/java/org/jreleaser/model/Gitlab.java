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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.jreleaser.util.Constants.KEY_IDENTIFIER;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public class Gitlab extends GitService {
    public static final String NAME = "gitlab";
    public static final String SKIP_GITLAB_LINKS = "skipGitlabLinks";

    private final Map<String, String> uploadLinks = new LinkedHashMap<>();
    private String identifier;

    public Gitlab() {
        super(NAME, true);
        setHost("gitlab.com");
        setRepoUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}");
        setRepoCloneUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}.git");
        setCommitUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}/-/commits");
        setSrcUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}/-/blob/{{repoBranch}}");
        setDownloadUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}/-/releases/{{tagName}}/downloads/{{artifactFile}}");
        setReleaseNotesUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}/-/releases/{{tagName}}");
        setLatestReleaseUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}/-/releases/{{tagName}}");
        setIssueTrackerUrl("https://{{repoHost}}/{{repoOwner}}/{{repoName}}/-/issues");
    }

    void setAll(Gitlab service) {
        super.setAll(service);
        this.identifier = service.identifier;
        setUploadLinks(service.uploadLinks);
    }

    @Override
    public String getReverseRepoHost() {
        return "com.gitlab";
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> getUploadLinks() {
        return uploadLinks;
    }

    public void setUploadLinks(Map<String, String> uploadLinks) {
        this.uploadLinks.clear();
        this.uploadLinks.putAll(uploadLinks);
    }

    @Override
    public Map<String, Object> asMap(boolean full) {
        Map<String, Object> map = super.asMap(full);
        map.put("identifier", identifier);
        map.put("uploadLinks", uploadLinks);
        return map;
    }

    @Override
    public Map<String, Object> props(JReleaserModel model) {
        Map<String, Object> props = super.props(model);
        props.put(KEY_IDENTIFIER, identifier);

        return props;
    }
}
