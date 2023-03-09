/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The JReleaser authors.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jreleaser.model.api.JReleaserContext.Mode;
import org.jreleaser.workflow.Workflows;

/**
 * Calculate the changelog.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@Mojo(name = "changelog")
public class JReleaserChangelogMojo extends AbstractJReleaserMojo {
    /**
     * Skip execution.
     */
    @Parameter(property = "jreleaser.changelog.skip")
    private boolean skip;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        Workflows.changelog(createContext()).execute();
    }

    @Override
    protected Mode getMode() {
        return Mode.CHANGELOG;
    }

    @Override
    protected boolean isSkip() {
        return skip;
    }
}
