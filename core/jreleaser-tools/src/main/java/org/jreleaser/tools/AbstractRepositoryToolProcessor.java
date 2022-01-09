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
package org.jreleaser.tools;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jreleaser.bundle.RB;
import org.jreleaser.model.Distribution;
import org.jreleaser.model.GitService;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.RepositoryTap;
import org.jreleaser.model.RepositoryTool;
import org.jreleaser.model.releaser.spi.Repository;
import org.jreleaser.model.tool.spi.ToolProcessingException;
import org.jreleaser.sdk.git.JReleaserGpgSigner;
import org.jreleaser.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

import static org.jreleaser.util.Constants.KEY_DISTRIBUTION_PACKAGE_DIRECTORY;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
abstract class AbstractRepositoryToolProcessor<T extends RepositoryTool> extends AbstractTemplateToolProcessor<T> {
    protected AbstractRepositoryToolProcessor(JReleaserContext context) {
        super(context);
    }

    protected void doPublishDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        RepositoryTap tap = tool.getRepositoryTap();
        if (!tap.isEnabled()) {
            context.getLogger().info(RB.$("repository.disabled"), tap.getCanonicalRepoName());
            return;
        }

        context.getLogger().info(RB.$("repository.setup"), tap.getCanonicalRepoName());
        if (context.isDryrun()) {
            return;
        }

        GitService gitService = context.getModel().getRelease().getGitService();

        try {
            // get the repository
            context.getLogger().debug(RB.$("repository.locate"), tap.getCanonicalRepoName());
            Repository repository = context.getReleaser().maybeCreateRepository(
                tap.getOwner(),
                tap.getResolvedName(),
                resolveGitToken(gitService));

            UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
                resolveGitUsername(gitService),
                resolveGitToken(gitService));

            // clone the repository
            context.getLogger().debug(RB.$("repository.clone"), repository.getHttpUrl());
            Path directory = Files.createTempDirectory("jreleaser-" + tap.getResolvedName());

            Git git = Git.cloneRepository()
                .setCredentialsProvider(credentialsProvider)
                .setBranch(tap.getBranch())
                .setDirectory(directory.toFile())
                .setURI(repository.getHttpUrl())
                .call();

            prepareWorkingCopy(props, directory, distribution);

            // add everything
            git.add()
                .addFilepattern(".")
                .call();

            props.putAll(distribution.props());
            context.getModel().getRelease().getGitService().fillProps(props, context.getModel());

            // setup commit
            context.getLogger().debug(RB.$("repository.commit.setup"));
            CommitCommand commitCommand = git.commit()
                .setAll(true)
                .setMessage(tool.getRepositoryTap().getResolvedCommitMessage(props))
                .setAuthor(tool.getCommitAuthor().getName(), tool.getCommitAuthor().getEmail());
            commitCommand.setCredentialsProvider(credentialsProvider);

            boolean signingEnabled = gitService.isSign();
            String signingKey = "**********";
            JReleaserGpgSigner signer = new JReleaserGpgSigner(context, signingEnabled);

            commitCommand = commitCommand
                .setSign(signingEnabled)
                .setSigningKey(signingKey)
                .setGpgSigner(signer);

            commitCommand.call();

            String tagName = tap.getResolvedTagName(props);
            context.getLogger().debug(RB.$("git.releaser.repository.tag"), tagName);
            git.tag()
                .setSigned(signingEnabled)
                .setSigningKey(signingKey)
                .setGpgSigner(signer)
                .setName(tagName)
                .setForceUpdate(true)
                .call();

            context.getLogger().info(RB.$("repository.push"), tap.getCanonicalRepoName());
            // push commit
            context.getLogger().debug(RB.$("repository.commit.push"));
            git.push()
                .setDryRun(false)
                .setPushAll()
                .setCredentialsProvider(credentialsProvider)
                .setPushTags()
                .call();
        } catch (Exception e) {
            throw new ToolProcessingException(RB.$("ERROR_unexpected_repository_update", tap.getCanonicalRepoName()), e);
        }
    }

    protected void prepareWorkingCopy(Map<String, Object> props, Path directory, Distribution distribution) throws ToolProcessingException, IOException {
        Path packageDirectory = (Path) props.get(KEY_DISTRIBUTION_PACKAGE_DIRECTORY);
        prepareWorkingCopy(packageDirectory, directory);
    }

    protected void prepareWorkingCopy(Path source, Path destination) throws IOException {
        context.getLogger().debug(RB.$("repository.copy.files"), context.relativizeToBasedir(source));

        if (!FileUtils.copyFilesRecursive(context.getLogger(), source, destination)) {
            throw new IOException(RB.$("ERROR_repository_copy_files",
                context.relativizeToBasedir(source)));
        }
    }

    protected void prepareWorkingCopy(Path source, Path destination, Predicate<Path> filter) throws IOException {
        context.getLogger().debug(RB.$("repository.copy.files"), context.relativizeToBasedir(source));

        if (!FileUtils.copyFilesRecursive(context.getLogger(), source, destination, filter)) {
            throw new IOException(RB.$("ERROR_repository_copy_files",
                context.relativizeToBasedir(source)));
        }
    }

    protected String resolveGitUsername(GitService gitService) {
        String username = tool.getRepositoryTap().getResolvedUsername(gitService);
        return isNotBlank(username) ? username : gitService.getResolvedUsername();
    }

    protected String resolveGitToken(GitService gitService) {
        String token = tool.getRepositoryTap().getResolvedToken(gitService);
        return isNotBlank(token) ? token : gitService.getResolvedToken();
    }
}
