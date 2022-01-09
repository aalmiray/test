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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jreleaser.gradle.plugin.dsl.Artifactory
import org.jreleaser.gradle.plugin.dsl.Http
import org.jreleaser.gradle.plugin.dsl.S3
import org.jreleaser.gradle.plugin.dsl.Upload
import org.kordamp.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 *
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
class UploadImpl implements Upload {
    final Property<Boolean> enabled
    final NamedDomainObjectContainer<Artifactory> artifactory
    final NamedDomainObjectContainer<Http> http
    final NamedDomainObjectContainer<S3> s3

    @Inject
    UploadImpl(ObjectFactory objects) {
        enabled = objects.property(Boolean).convention(true)

        artifactory = objects.domainObjectContainer(Artifactory, new NamedDomainObjectFactory<Artifactory>() {
            @Override
            Artifactory create(String name) {
                ArtifactoryImpl a = objects.newInstance(ArtifactoryImpl, objects)
                a.name = name
                return a
            }
        })

        http = objects.domainObjectContainer(Http, new NamedDomainObjectFactory<Http>() {
            @Override
            Http create(String name) {
                HttpImpl h = objects.newInstance(HttpImpl, objects)
                h.name = name
                return h
            }
        })

        s3 = objects.domainObjectContainer(S3, new NamedDomainObjectFactory<S3>() {
            @Override
            S3 create(String name) {
                S3Impl s = objects.newInstance(S3Impl, objects)
                s.name = name
                return s
            }
        })
    }

    @Override
    void artifactory(Action<? super NamedDomainObjectContainer<Artifactory>> action) {
        action.execute(artifactory)
    }

    @Override
    void http(Action<? super NamedDomainObjectContainer<Http>> action) {
        action.execute(http)
    }

    @Override
    void s3(Action<? super NamedDomainObjectContainer<S3>> action) {
        action.execute(s3)
    }

    @Override
    void artifactory(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = NamedDomainObjectContainer) Closure<Void> action) {
        ConfigureUtil.configure(action, artifactory)
    }

    @Override
    void http(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = NamedDomainObjectContainer) Closure<Void> action) {
        ConfigureUtil.configure(action, http)
    }

    @Override
    void s3(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = NamedDomainObjectContainer) Closure<Void> action) {
        ConfigureUtil.configure(action, s3)
    }

    @CompileDynamic
    org.jreleaser.model.Upload toModel() {
        org.jreleaser.model.Upload upload = new org.jreleaser.model.Upload()

        artifactory.each { upload.addArtifactory(((ArtifactoryImpl) it).toModel()) }
        http.each { upload.addHttp(((HttpImpl) it).toModel()) }
        s3.each { upload.addS3(((S3Impl) it).toModel()) }

        upload
    }
}
