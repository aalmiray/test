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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jreleaser.model.api.JReleaserContext.Mode;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.workflow.Workflows;

/**
 * Assemble all distributions.
 *
 * @author Andres Almiray
 * @since 0.2.0
 */
@Mojo(name = "assemble")
public class JReleaserAssembleMojo extends AbstractPlatformAwareJReleaserMojo {
    /**
     * Include an assembler.
     */
    @Parameter(property = "jreleaser.assemblers")
    private String[] includedAssemblers;

    /**
     * Exclude an assembler.
     */
    @Parameter(property = "jreleaser.excluded.assemblers")
    private String[] excludedAssemblers;

    /**
     * Include a distribution.
     */
    @Parameter(property = "jreleaser.distributions")
    private String[] includedDistributions;

    /**
     * Exclude a distribution.
     */
    @Parameter(property = "jreleaser.excluded.distributions")
    private String[] excludedDistributions;

    /**
     * Skip execution.
     */
    @Parameter(property = "jreleaser.assemble.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Banner.display(project, getLog());
        if (skip) {
            getLog().info("Execution has been explicitly skipped.");
            return;
        }

        JReleaserContext context = createContext();
        context.setIncludedAssemblers(collectEntries(includedAssemblers, true));
        context.setIncludedDistributions(collectEntries(includedDistributions));
        context.setExcludedAssemblers(collectEntries(excludedAssemblers, true));
        context.setExcludedDistributions(collectEntries(excludedDistributions));
        Workflows.assemble(context).execute();
    }

    @Override
    protected Mode getMode() {
        return Mode.ASSEMBLE;
    }
}
