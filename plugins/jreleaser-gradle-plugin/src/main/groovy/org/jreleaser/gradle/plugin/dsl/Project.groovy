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
package org.jreleaser.gradle.plugin.dsl

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
interface Project extends ExtraProperties {
    Property<String> getName()

    Property<String> getVersion()

    Property<String> getVersionPattern()

    Property<String> getDescription()

    Property<String> getLongDescription()

    Property<String> getWebsite()

    Property<String> getLicense()

    Property<String> getLicenseUrl()

    Property<String> getCopyright()

    Property<String> getVendor()

    Property<String> getDocsUrl()

    ListProperty<String> getAuthors()

    ListProperty<String> getTags()

    void author(String name)

    void tag(String tag)

    Java getJava()

    Snapshot getSnapshot()

    void java(Action<? super Java> action)

    void java(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Java) Closure<Void> action)

    void snapshot(Action<? super Snapshot> action)

    void snapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Snapshot) Closure<Void> action)

    interface Snapshot {
        Property<String> getPattern()

        Property<String> getLabel()

        Property<Boolean> getFullChangelog()
    }
}