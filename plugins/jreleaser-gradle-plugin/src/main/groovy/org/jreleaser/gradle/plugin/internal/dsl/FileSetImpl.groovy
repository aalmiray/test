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
package org.jreleaser.gradle.plugin.internal.dsl

import groovy.transform.CompileStatic
import org.gradle.api.internal.provider.Providers
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jreleaser.gradle.plugin.dsl.FileSet

import javax.inject.Inject

import static org.jreleaser.util.StringUtils.isNotBlank

/**
 *
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
class FileSetImpl implements FileSet {
    String name
    final Property<String> input
    final Property<String> output
    final Property<Boolean> failOnMissingInput
    final SetProperty<String> includes
    final SetProperty<String> excludes
    final MapProperty<String, Object> extraProperties

    @Inject
    FileSetImpl(ObjectFactory objects) {
        input = objects.property(String).convention(Providers.notDefined())
        output = objects.property(String).convention(Providers.notDefined())
        failOnMissingInput = objects.property(Boolean).convention(Providers.notDefined())
        includes = objects.setProperty(String).convention(Providers.notDefined())
        excludes = objects.setProperty(String).convention(Providers.notDefined())
        extraProperties = objects.mapProperty(String, Object).convention(Providers.notDefined())
    }

    void include(String str) {
        if (isNotBlank(str)) {
            includes.add(str.trim())
        }
    }

    void exclude(String str) {
        if (isNotBlank(str)) {
            excludes.add(str.trim())
        }
    }

    org.jreleaser.model.FileSet toModel() {
        org.jreleaser.model.FileSet fileSet = new org.jreleaser.model.FileSet()
        if (input.present) fileSet.input = input.get()
        if (output.present) fileSet.output = output.get()
        if (failOnMissingInput.present) fileSet.failOnMissingInput = failOnMissingInput.get()
        fileSet.includes = (Set<String>) includes.getOrElse([] as Set<String>)
        fileSet.includes = (Set<String>) includes.getOrElse([] as Set<String>)
        if (extraProperties.present) fileSet.extraProperties.putAll(extraProperties.get())
        fileSet
    }
}
