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
import org.gradle.api.tasks.Internal
import org.jreleaser.gradle.plugin.dsl.DockerSpec

import javax.inject.Inject

import static org.jreleaser.util.StringUtils.isNotBlank

/**
 *
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
class DockerSpecImpl extends AbstractDockerConfiguration implements DockerSpec {
    String name
    final MapProperty<String, Object> matchers

    @Inject
    DockerSpecImpl(ObjectFactory objects) {
        super(objects)

        matchers = objects.mapProperty(String, Object).convention(Providers.notDefined())
    }

    @Override
    @Deprecated
    void addMatcher(String key, Object value) {
        println('spec.addMatcher() has been deprecated since 1.0.0-M2 and will be removed in the future. Use spec.matcher() instead')
        matcher(key, value)
    }

    @Override
    void matcher(String key, Object value) {
        if (isNotBlank(key) && null != value) {
            matchers.put(key.trim(), value)
        }
    }

    @Override
    @Internal
    boolean isSet() {
        super.isSet() ||
            !matchers.present
    }

    org.jreleaser.model.DockerSpec toModel() {
        org.jreleaser.model.DockerSpec spec = new org.jreleaser.model.DockerSpec()
        spec.name = name
        toModel(spec)
        if (matchers.present) spec.matchers.putAll(matchers.get())
        spec
    }
}
