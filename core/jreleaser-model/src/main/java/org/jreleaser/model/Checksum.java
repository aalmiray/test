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

import org.jreleaser.util.Algorithm;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.jreleaser.util.MustacheUtils.applyTemplate;

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
public class Checksum implements Domain {
    public static final String KEY_SKIP_CHECKSUM = "skipChecksum";
    public static final String INDIVIDUAL_CHECKSUM = "individualChecksum";

    private final Set<Algorithm> algorithms = new LinkedHashSet<>();
    private Boolean individual;
    private String name;
    private Boolean files;

    void setAll(Checksum checksum) {
        this.name = checksum.name;
        this.individual = checksum.individual;
        this.files = checksum.files;
        setAlgorithms(checksum.algorithms);
    }

    public String getResolvedName(JReleaserContext context) {
        Map<String, Object> props = context.props();
        context.getModel().getRelease().getGitService().fillProps(props, context.getModel());
        return applyTemplate(name, props);
    }

    public String getResolvedName(JReleaserContext context, Algorithm algorithm) {
        String resolvedName = context.getModel().getChecksum().getResolvedName(context);
        int pos = resolvedName.lastIndexOf(".");
        if (pos != -1) {
            resolvedName = resolvedName.substring(0, pos) + "_" + algorithm.formatted() + resolvedName.substring(pos);
        } else {
            resolvedName += "." + algorithm.formatted();
        }

        return resolvedName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isIndividual() {
        return individual != null && individual;
    }

    public void setIndividual(Boolean individual) {
        this.individual = individual;
    }

    public boolean isIndividualSet() {
        return individual != null;
    }

    public Set<Algorithm> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(Set<Algorithm> algorithms) {
        this.algorithms.clear();
        this.algorithms.addAll(algorithms);
    }

    public Boolean isFiles() {
        return files == null || files;
    }

    public boolean isFilesSet() {
        return files != null;
    }

    public void setFiles(Boolean files) {
        this.files = files;
    }

    @Override
    public Map<String, Object> asMap(boolean full) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", name);
        props.put("individual", isIndividual());
        props.put("algorithms", algorithms);
        props.put("files", isFiles());
        return props;
    }
}
