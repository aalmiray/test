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
import org.gradle.api.Action
import org.gradle.api.internal.provider.Providers
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jreleaser.gradle.plugin.dsl.Chocolatey
import org.jreleaser.gradle.plugin.dsl.CommitAuthor
import org.jreleaser.gradle.plugin.dsl.Tap
import org.kordamp.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ChocolateyImpl extends AbstractRepositoryTool implements Chocolatey {
    final Property<String> packageName
    final Property<String> username
    final Property<String> apiKey
    final Property<String> title
    final Property<String> iconUrl
    final Property<String> source
    final Property<Boolean> remoteBuild
    final CommitAuthorImpl commitAuthor
    final TapImpl bucket

    @Inject
    ChocolateyImpl(ObjectFactory objects) {
        super(objects)
        packageName = objects.property(String).convention(Providers.notDefined())
        username = objects.property(String).convention(Providers.notDefined())
        apiKey = objects.property(String).convention(Providers.notDefined())
        title = objects.property(String).convention(Providers.notDefined())
        iconUrl = objects.property(String).convention(Providers.notDefined())
        source = objects.property(String).convention(Providers.notDefined())
        remoteBuild = objects.property(Boolean).convention(Providers.notDefined())
        bucket = objects.newInstance(TapImpl, objects)
        commitAuthor = objects.newInstance(CommitAuthorImpl, objects)
    }

    @Override
    @Internal
    boolean isSet() {
        super.isSet() ||
            packageName.present ||
            username.present ||
            apiKey.present ||
            title.present ||
            iconUrl.present ||
            source.present ||
            remoteBuild.present ||
            bucket.isSet() ||
            commitAuthor.isSet()
    }

    @Override
    void bucket(Action<? super Tap> action) {
        action.execute(bucket)
    }

    @Override
    void commitAuthor(Action<? super CommitAuthor> action) {
        action.execute(commitAuthor)
    }

    @Override
    void bucket(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Tap) Closure<Void> action) {
        ConfigureUtil.configure(action, bucket)
    }

    @Override
    void commitAuthor(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CommitAuthor) Closure<Void> action) {
        ConfigureUtil.configure(action, commitAuthor)
    }

    org.jreleaser.model.Chocolatey toModel() {
        org.jreleaser.model.Chocolatey tool = new org.jreleaser.model.Chocolatey()
        fillToolProperties(tool)
        fillTemplateToolProperties(tool)
        if (bucket.isSet()) tool.bucket = bucket.toChocolateyBucket()
        if (commitAuthor.isSet()) tool.commitAuthor = commitAuthor.toModel()
        if (packageName.present) tool.packageName = packageName.get()
        if (username.present) tool.username = username.get()
        if (apiKey.present) tool.apiKey = apiKey.get()
        if (title.present) tool.title = title.get()
        if (iconUrl.present) tool.iconUrl = iconUrl.get()
        if (source.present) tool.source = source.get()
        tool.remoteBuild = remoteBuild.getOrElse(false)
        tool
    }
}
