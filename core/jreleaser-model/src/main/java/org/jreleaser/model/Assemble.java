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

import org.jreleaser.util.JReleaserException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jreleaser.util.StringUtils.isBlank;

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
public class Assemble implements Domain, EnabledAware {
    private final Map<String, Archive> archive = new LinkedHashMap<>();
    private final Map<String, Jlink> jlink = new LinkedHashMap<>();
    private final Map<String, Jpackage> jpackage = new LinkedHashMap<>();
    private final Map<String, NativeImage> nativeImage = new LinkedHashMap<>();
    private Boolean enabled;

    void setAll(Assemble assemble) {
        this.enabled = assemble.enabled;
        setArchive(assemble.archive);
        setJlink(assemble.jlink);
        setJpackage(assemble.jpackage);
        setNativeImage(assemble.nativeImage);
    }

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    @Override
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabledSet() {
        return enabled != null;
    }

    public List<Archive> getActiveArchives() {
        return archive.values().stream()
            .filter(Archive::isEnabled)
            .collect(Collectors.toList());
    }

    public Map<String, Archive> getArchive() {
        return archive;
    }

    public void setArchive(Map<String, Archive> archive) {
        this.archive.clear();
        this.archive.putAll(archive);
    }

    public void addArchive(Archive archive) {
        this.archive.put(archive.getName(), archive);
    }

    public List<Jlink> getActiveJlinks() {
        return jlink.values().stream()
            .filter(Jlink::isEnabled)
            .collect(Collectors.toList());
    }

    public Map<String, Jlink> getJlink() {
        return jlink;
    }

    public void setJlink(Map<String, Jlink> jlink) {
        this.jlink.clear();
        this.jlink.putAll(jlink);
    }

    public void addJlink(Jlink jlink) {
        this.jlink.put(jlink.getName(), jlink);
    }

    public Jlink findJlink(String name) {
        if (isBlank(name)) {
            throw new JReleaserException("Jlink name must not be blank");
        }

        if (jlink.containsKey(name)) {
            return jlink.get(name);
        }

        throw new JReleaserException("Jlink '" + name + "' not found");
    }

    public List<Jpackage> getActiveJpackages() {
        return jpackage.values().stream()
        .filter(Jpackage::isEnabled)
        .collect(Collectors.toList());
    }
    
    public Map<String, Jpackage> getJpackage() {
        return jpackage;
    }
    
    public void setJpackage(Map<String, Jpackage> jpackage) {
        this.jpackage.clear();
        this.jpackage.putAll(jpackage);
    }
    
    public void addJpackage(Jpackage jpackage) {
        this.jpackage.put(jpackage.getName(), jpackage);
    }
    
    public List<NativeImage> getActiveNativeImages() {
        return nativeImage.values().stream()
            .filter(NativeImage::isEnabled)
            .collect(Collectors.toList());
    }

    public Map<String, NativeImage> getNativeImage() {
        return nativeImage;
    }

    public void setNativeImage(Map<String, NativeImage> nativeImage) {
        this.nativeImage.clear();
        this.nativeImage.putAll(nativeImage);
    }

    public void addNativeImage(NativeImage nativeImage) {
        this.nativeImage.put(nativeImage.getName(), nativeImage);
    }

    @Override
    public Map<String, Object> asMap(boolean full) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", isEnabled());

        List<Map<String, Object>> archive = this.archive.values()
            .stream()
            .filter(d -> full || d.isEnabled())
            .map(d -> d.asMap(full))
            .collect(Collectors.toList());
        if (!archive.isEmpty()) map.put("archive", archive);

        List<Map<String, Object>> jlink = this.jlink.values()
            .stream()
            .filter(d -> full || d.isEnabled())
            .map(d -> d.asMap(full))
            .collect(Collectors.toList());
        if (!jlink.isEmpty()) map.put("jlink", jlink);

        List<Map<String, Object>> jpackage = this.jpackage.values()
            .stream()
            .filter(d -> full || d.isEnabled())
            .map(d -> d.asMap(full))
            .collect(Collectors.toList());
        if (!jpackage.isEmpty()) map.put("jpackage", jpackage);

        List<Map<String, Object>> nativeImage = this.nativeImage.values()
            .stream()
            .filter(d -> full || d.isEnabled())
            .map(d -> d.asMap(full))
            .collect(Collectors.toList());
        if (!nativeImage.isEmpty()) map.put("nativeImage", nativeImage);

        return map;
    }

    public <A extends Assembler> Map<String, A> findAssemblersByType(String assemblerName) {
        switch (assemblerName) {
            case Archive.TYPE:
                return (Map<String, A>) archive;
            case Jlink.TYPE:
                return (Map<String, A>) jlink;
            case Jpackage.TYPE:
                return (Map<String, A>) jpackage;
            case NativeImage.TYPE:
                return (Map<String, A>) nativeImage;
        }

        return Collections.emptyMap();
    }

    public <A extends Assembler> Collection<A> findAllAssemblers() {
        List<A> assemblers = new ArrayList<>();
        assemblers.addAll((List<A>) getActiveArchives());
        assemblers.addAll((List<A>) getActiveJlinks());
        assemblers.addAll((List<A>) getActiveJpackages());
        assemblers.addAll((List<A>) getActiveNativeImages());
        return assemblers;
    }

    public static Set<String> supportedAssemblers() {
        Set<String> set = new LinkedHashSet<>();
        set.add(Archive.TYPE);
        set.add(Jlink.TYPE);
        set.add(Jpackage.TYPE);
        set.add(NativeImage.TYPE);
        return Collections.unmodifiableSet(set);
    }
}
