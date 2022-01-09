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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Andres Almiray
 * @since 0.6.0
 */
public class Article extends AbstractAnnouncer implements CommitAuthorAware {
    public static final String NAME = "article";
    private final Set<Artifact> files = new LinkedHashSet<>();
    private final CommitAuthor commitAuthor = new CommitAuthor();
    private final Repository repository = new Repository();

    private String templateDirectory;

    public Article() {
        super(NAME);
    }

    void setAll(Article article) {
        super.setAll(article);
        this.templateDirectory = article.templateDirectory;
        setFiles(article.files);
        setCommitAuthor(article.commitAuthor);
        setRepository(article.repository);
    }

    public Set<Artifact> getFiles() {
        return Artifact.sortArtifacts(files);
    }

    public void setFiles(Set<Artifact> files) {
        this.files.clear();
        this.files.addAll(files);
    }

    public void addFiles(Set<Artifact> files) {
        this.files.addAll(files);
    }

    public void addFile(Artifact artifact) {
        if (null != artifact) {
            this.files.add(artifact);
        }
    }

    @Override
    public CommitAuthor getCommitAuthor() {
        return commitAuthor;
    }

    @Override
    public void setCommitAuthor(CommitAuthor commitAuthor) {
        this.commitAuthor.setAll(commitAuthor);
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository.setAll(repository);
    }

    public String getTemplateDirectory() {
        return templateDirectory;
    }

    public void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory = templateDirectory;
    }

    @Override
    protected void asMap(Map<String, Object> props, boolean full) {
        props.put("commitAuthor", commitAuthor.asMap(full));
        props.put("repository", repository.asMap(full));

        Map<String, Map<String, Object>> mappedArtifacts = new LinkedHashMap<>();
        int i = 0;
        for (Artifact artifact : getFiles()) {
            mappedArtifacts.put("files " + (i++), artifact.asMap(full));
        }
        props.put("files", mappedArtifacts);
        props.put("templateDirectory", templateDirectory);
    }
}
