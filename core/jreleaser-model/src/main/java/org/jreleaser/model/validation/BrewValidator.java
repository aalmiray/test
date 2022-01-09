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
package org.jreleaser.model.validation;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Artifact;
import org.jreleaser.model.Brew;
import org.jreleaser.model.Distribution;
import org.jreleaser.model.GitService;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.JReleaserModel;
import org.jreleaser.util.Errors;
import org.jreleaser.util.PlatformUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static org.jreleaser.model.Brew.SKIP_BREW;
import static org.jreleaser.model.validation.DistributionsValidator.validateArtifactPlatforms;
import static org.jreleaser.model.validation.ExtraPropertiesValidator.mergeExtraProperties;
import static org.jreleaser.model.validation.TemplateValidator.validateTemplate;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;
import static org.jreleaser.util.StringUtils.isTrue;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public abstract class BrewValidator extends Validator {
    public static void validateBrew(JReleaserContext context, Distribution distribution, Brew tool, Errors errors) {
        JReleaserModel model = context.getModel();
        Brew parentTool = model.getPackagers().getBrew();

        if (!tool.isActiveSet() && parentTool.isActiveSet()) {
            tool.setActive(parentTool.getActive());
        }
        if (!tool.resolveEnabled(context.getModel().getProject(), distribution)) {
            tool.disable();
            tool.getCask().disable();
            return;
        }
        GitService service = model.getRelease().getGitService();
        if (!service.isReleaseSupported()) {
            tool.disable();
            tool.getCask().disable();
            return;
        }

        context.getLogger().debug("distribution.{}.brew", distribution.getName());

        validateCommitAuthor(tool, parentTool);
        Brew.HomebrewTap tap = tool.getTap();
        tap.resolveEnabled(model.getProject());
        validateTap(context, distribution, tap, parentTool.getTap(), "brew.tap");
        validateTemplate(context, distribution, tool, parentTool, errors);
        mergeExtraProperties(tool, parentTool);
        validateContinueOnError(tool, parentTool);

        List<Brew.Dependency> dependencies = new ArrayList<>(parentTool.getDependenciesAsList());
        dependencies.addAll(tool.getDependenciesAsList());
        tool.setDependenciesAsList(dependencies);

        if (isBlank(tool.getFormulaName())) {
            tool.setFormulaName(distribution.getName());
        }

        if (!tool.isMultiPlatformSet() && parentTool.isMultiPlatformSet()) {
            tool.setMultiPlatform(parentTool.isMultiPlatform());
        }
        if (tool.isMultiPlatform() &&
            (distribution.getType() == Distribution.DistributionType.SINGLE_JAR ||
                distribution.getType() == Distribution.DistributionType.JAVA_BINARY ||
                distribution.getType() == Distribution.DistributionType.NATIVE_PACKAGE)) {
            tool.setMultiPlatform(false);
        }
        if (tool.isMultiPlatform()) {
            tool.getCask().disable();
        }

        validateCask(context, distribution, tool, parentTool, errors);

        if (!tool.getCask().isEnabled()) {
            validateArtifactPlatforms(context, distribution, tool, errors);
        }
    }

    private static void validateCask(JReleaserContext context, Distribution distribution, Brew tool, Brew parentTool, Errors errors) {
        if (distribution.getType() == Distribution.DistributionType.SINGLE_JAR) {
            tool.getCask().disable();
            return;
        }

        Brew.Cask cask = tool.getCask();
        Brew.Cask parentCask = parentTool.getCask();

        if (!cask.isEnabledSet() && parentCask.isEnabledSet()) {
            cask.setEnabled(parentCask.isEnabled());
        }

        if (cask.isEnabledSet() && !cask.isEnabled()) {
            return;
        }

        context.getLogger().debug("distribution.{}.brew.cask", distribution.getName());

        // look for a .dmg, .pkg. or .zip
        int dmgFound = 0;
        int pkgFound = 0;
        int zipFound = 0;
        String pkgName = "";
        for (Artifact artifact : distribution.getArtifacts()) {
            if (!artifact.isActive() || !PlatformUtils.isMac(artifact.getPlatform())) continue;
            if (artifact.getPath().endsWith(".dmg") && !isTrue(artifact.getExtraProperties().get(SKIP_BREW))) {
                dmgFound++;
            } else if (artifact.getPath().endsWith(".pkg") && !isTrue(artifact.getExtraProperties().get(SKIP_BREW))) {
                pkgFound++;
                pkgName = artifact.getEffectivePath(context).getFileName().toString();
            } else if (artifact.getPath().endsWith(".zip") && !isTrue(artifact.getExtraProperties().get(SKIP_BREW))) {
                zipFound++;
            }
        }


        if (dmgFound == 0 && pkgFound == 0 && zipFound == 0) {
            // no artifacts found, disable cask
            cask.disable();
            return;
        } else if (dmgFound > 1) {
            errors.configuration(RB.$("validation_brew_multiple_artifact", "distribution." + distribution.getName() + ".brew", ".dmg"));
            cask.disable();
            return;
        } else if (pkgFound > 1) {
            errors.configuration(RB.$("validation_brew_multiple_artifact", "distribution." + distribution.getName() + ".brew", ".pkg"));
            cask.disable();
            return;
        } else if (zipFound > 1) {
            errors.configuration(RB.$("validation_brew_multiple_artifact", "distribution." + distribution.getName() + ".brew", ".zip"));
            cask.disable();
            return;
        } else if (dmgFound + pkgFound + zipFound > 1) {
            errors.configuration(RB.$("validation_brew_single_artifact", "distribution." + distribution.getName() + ".brew"));
            cask.disable();
            return;
        }

        if (zipFound == 1 && !cask.isEnabled()) {
            // zips should only be packaged into Casks when explicitly stated
            // https://github.com/jreleaser/jreleaser/issues/337
            return;
        }

        cask.enable();

        if (isBlank(cask.getPkgName()) && isNotBlank(pkgName)) {
            cask.setPkgName(pkgName);
        }

        if (isNotBlank(cask.getPkgName())) {
            if (!cask.getPkgName().endsWith(".pkg")) {
                cask.setPkgName(cask.getPkgName() + ".pkg");
            }
        } else if (isBlank(cask.getAppName())) {
            cask.setAppName(tool.getResolvedFormulaName(context) + ".app");
        } else if (!cask.getAppName().endsWith(".app")) {
            cask.setAppName(cask.getAppName() + ".app");
        }
        if (zipFound > 0) {
            cask.setAppName("");
            cask.setPkgName("");
        }

        if (isBlank(cask.getName())) {
            cask.setName(tool.getResolvedFormulaName(context).toLowerCase());
        }
        if (isBlank(cask.getDisplayName())) {
            cask.setDisplayName(tool.getResolvedFormulaName(context));
        }
    }

    public static void postValidateBrew(JReleaserContext context, Errors errors) {
        Map<String, List<Distribution>> map = context.getModel().getDistributions().values().stream()
            .filter(d -> d.isEnabled() && d.getBrew().isEnabled())
            .collect(groupingBy(d -> d.getBrew().getResolvedFormulaName(context)));

        map.forEach((formulaName, distributions) -> {
            if (distributions.size() > 1) {
                errors.configuration(RB.$("validation_brew_duplicate_definition", "brew.formulaName '" + formulaName + "'",
                    distributions.stream().map(Distribution::getName).collect(Collectors.joining(", "))));
            }
        });

        map = context.getModel().getDistributions().values().stream()
            .filter(d -> d.isEnabled() && d.getBrew().getCask().isEnabled())
            .collect(groupingBy(d -> d.getBrew().getCask().getResolvedCaskName(context)));

        map.forEach((caskName, distributions) -> {
            if (distributions.size() > 1) {
                errors.configuration(RB.$("validation_brew_duplicate_definition", "brew.cask.name '" + caskName + "'",
                    distributions.stream().map(Distribution::getName).collect(Collectors.joining(", "))));
            }
        });
    }
}
