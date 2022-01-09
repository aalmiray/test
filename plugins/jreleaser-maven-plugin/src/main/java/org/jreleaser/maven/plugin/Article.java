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
package org.jreleaser.maven.plugin;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Andres Almiray
 * @since 0.6.0
 */
public class Article extends AbstractAnnouncer {
    private final Set<Artifact> files = new LinkedHashSet<>();
    private final CommitAuthor commitAuthor = new CommitAuthor();
    private final Tap repository = new Tap();

    private String templateDirectory;

    void setAll(Article article) {
        super.setAll(article);
        setFiles(article.files);
        setCommitAuthor(article.commitAuthor);
        setRepository(article.repository);
    }

    public Set<Artifact> getFiles() {
        return files;
    }

    public void setFiles(Set<Artifact> files) {
        this.files.clear();
        this.files.addAll(files);
    }

    public CommitAuthor getCommitAuthor() {
        return commitAuthor;
    }

    public void setCommitAuthor(CommitAuthor commitAuthor) {
        this.commitAuthor.setAll(commitAuthor);
    }

    public Tap getRepository() {
        return repository;
    }

    public void setRepository(Tap repository) {
        this.repository.setAll(repository);
    }

    public String getTemplateDirectory() {
        return templateDirectory;
    }

    public void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory = templateDirectory;
    }

    @Override
    public boolean isSet() {
        return super.isSet() ||
            !files.isEmpty() ||
            commitAuthor.isSet() ||
            repository.isSet();
    }
}
