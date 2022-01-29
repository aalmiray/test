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

import java.util.ArrayList;
import java.util.List;

import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.9.1
 */
public class Spec extends AbstractRepositoryPackager {
    private final List<String> requires = new ArrayList<>();
    private final Tap repository = new Tap();

    private String packageName;
    private String release;

    void setAll(Spec spec) {
        super.setAll(spec);
        this.packageName = spec.packageName;
        this.release = spec.release;
        setRepository(spec.repository);
        setRequires(spec.requires);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public Tap getRepository() {
        return repository;
    }

    public void setRepository(Tap repository) {
        this.repository.setAll(repository);
    }

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires.clear();
        this.requires.addAll(requires);
    }

    @Override
    public boolean isSet() {
        return super.isSet() ||
            isNotBlank(packageName) ||
            isNotBlank(release) ||
            !requires.isEmpty() ||
            repository.isSet();
    }
}
