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
package org.jreleaser.maven.plugin.internal;

import org.jreleaser.maven.plugin.Announce;
import org.jreleaser.maven.plugin.Announcer;
import org.jreleaser.maven.plugin.Archive;
import org.jreleaser.maven.plugin.Article;
import org.jreleaser.maven.plugin.Artifact;
import org.jreleaser.maven.plugin.Artifactory;
import org.jreleaser.maven.plugin.Assemble;
import org.jreleaser.maven.plugin.Assembler;
import org.jreleaser.maven.plugin.Brew;
import org.jreleaser.maven.plugin.Bucket;
import org.jreleaser.maven.plugin.Changelog;
import org.jreleaser.maven.plugin.Checksum;
import org.jreleaser.maven.plugin.Chocolatey;
import org.jreleaser.maven.plugin.Codeberg;
import org.jreleaser.maven.plugin.CommitAuthor;
import org.jreleaser.maven.plugin.Discord;
import org.jreleaser.maven.plugin.Discussions;
import org.jreleaser.maven.plugin.Distribution;
import org.jreleaser.maven.plugin.Docker;
import org.jreleaser.maven.plugin.DockerConfiguration;
import org.jreleaser.maven.plugin.DockerSpec;
import org.jreleaser.maven.plugin.Environment;
import org.jreleaser.maven.plugin.FileSet;
import org.jreleaser.maven.plugin.FileType;
import org.jreleaser.maven.plugin.Files;
import org.jreleaser.maven.plugin.GenericGit;
import org.jreleaser.maven.plugin.GitService;
import org.jreleaser.maven.plugin.Gitea;
import org.jreleaser.maven.plugin.Github;
import org.jreleaser.maven.plugin.Gitlab;
import org.jreleaser.maven.plugin.Gitter;
import org.jreleaser.maven.plugin.Glob;
import org.jreleaser.maven.plugin.Gofish;
import org.jreleaser.maven.plugin.GoogleChat;
import org.jreleaser.maven.plugin.Http;
import org.jreleaser.maven.plugin.Java;
import org.jreleaser.maven.plugin.JavaAssembler;
import org.jreleaser.maven.plugin.Jbang;
import org.jreleaser.maven.plugin.Jlink;
import org.jreleaser.maven.plugin.Jpackage;
import org.jreleaser.maven.plugin.Jreleaser;
import org.jreleaser.maven.plugin.Macports;
import org.jreleaser.maven.plugin.Mail;
import org.jreleaser.maven.plugin.Mastodon;
import org.jreleaser.maven.plugin.Mattermost;
import org.jreleaser.maven.plugin.NativeImage;
import org.jreleaser.maven.plugin.Packager;
import org.jreleaser.maven.plugin.Packagers;
import org.jreleaser.maven.plugin.Platform;
import org.jreleaser.maven.plugin.Project;
import org.jreleaser.maven.plugin.Registry;
import org.jreleaser.maven.plugin.Release;
import org.jreleaser.maven.plugin.S3;
import org.jreleaser.maven.plugin.Scoop;
import org.jreleaser.maven.plugin.Sdkman;
import org.jreleaser.maven.plugin.SdkmanAnnouncer;
import org.jreleaser.maven.plugin.Signing;
import org.jreleaser.maven.plugin.Slack;
import org.jreleaser.maven.plugin.Snap;
import org.jreleaser.maven.plugin.Spec;
import org.jreleaser.maven.plugin.Tap;
import org.jreleaser.maven.plugin.Teams;
import org.jreleaser.maven.plugin.Telegram;
import org.jreleaser.maven.plugin.Twitter;
import org.jreleaser.maven.plugin.Upload;
import org.jreleaser.maven.plugin.Uploader;
import org.jreleaser.maven.plugin.Webhook;
import org.jreleaser.maven.plugin.Zulip;
import org.jreleaser.model.HttpUploader;
import org.jreleaser.model.JReleaserModel;
import org.jreleaser.model.Repository;
import org.jreleaser.model.RepositoryTap;
import org.jreleaser.model.UpdateSection;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public final class JReleaserModelConverter {
    private static final Pattern ESCAPED_ENTITY = Pattern.compile(".*&#(?:[xX][a-fA-F0-9]+|[0-9]+);.*");

    private JReleaserModelConverter() {
        // noop
    }

    private static String tr(String str) {
        if (isNotBlank(str) && ESCAPED_ENTITY.matcher(str).matches()) {
            return Parser.unescapeEntities(str, true);
        }
        return str;
    }

    private static List<String> tr(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, tr(list.get(i)));
        }
        return list;
    }

    private static Set<String> tr(Set<String> input) {
        Set<String> set = new LinkedHashSet<>();
        for (String s : input) {
            set.add(tr(s));
        }
        return set;
    }

    public static JReleaserModel convert(Jreleaser jreleaser) {
        JReleaserModel jreleaserModel = new JReleaserModel();
        jreleaserModel.setEnvironment(convertEnvironment(jreleaser.getEnvironment()));
        jreleaserModel.setProject(convertProject(jreleaser.getProject()));
        jreleaserModel.setPlatform(convertPlatform(jreleaser.getPlatform()));
        jreleaserModel.setRelease(convertRelease(jreleaser.getRelease()));
        jreleaserModel.setUpload(convertUpload(jreleaser.getUpload()));
        jreleaserModel.setPackagers(convertPackagers(jreleaser.getPackagers()));
        jreleaserModel.setAnnounce(convertAnnounce(jreleaser.getAnnounce()));
        jreleaserModel.setAssemble(convertAssemble(jreleaser.getAssemble()));
        jreleaserModel.setChecksum(convertChecksum(jreleaser.getChecksum()));
        jreleaserModel.setSigning(convertSigning(jreleaser.getSigning()));
        jreleaserModel.setFiles(convertFiles(jreleaser.getFiles()));
        jreleaserModel.setDistributions(convertDistributions(jreleaser.getDistributions()));
        return jreleaserModel;
    }

    private static org.jreleaser.model.Environment convertEnvironment(Environment environment) {
        org.jreleaser.model.Environment e = new org.jreleaser.model.Environment();
        e.setVariables(environment.getVariables());
        e.setProperties(environment.getProperties());
        return e;
    }

    private static org.jreleaser.model.Project convertProject(Project project) {
        org.jreleaser.model.Project p = new org.jreleaser.model.Project();
        p.setName(tr(project.getName()));
        p.setVersion(tr(project.getVersion()));
        p.setVersionPattern(tr(project.getVersionPattern()));
        p.setSnapshot(convertSnapshot(project.getSnapshot()));
        p.setDescription(tr(project.getDescription()));
        p.setLongDescription(tr(project.getLongDescription()));
        p.setWebsite(tr(project.getWebsite()));
        p.setLicense(tr(project.getLicense()));
        p.setLicenseUrl(tr(project.getLicenseUrl()));
        p.setCopyright(tr(project.getCopyright()));
        p.setVendor(tr(project.getVendor()));
        p.setDocsUrl(tr(project.getDocsUrl()));
        p.setTags(tr(project.getTags()));
        p.setAuthors(tr(project.getAuthors()));
        p.setExtraProperties(project.getExtraProperties());
        p.setJava(convertJava(project.getJava()));
        return p;
    }

    private static org.jreleaser.model.Platform convertPlatform(Platform platform) {
        org.jreleaser.model.Platform p = new org.jreleaser.model.Platform();
        p.setReplacements(platform.getReplacements());
        return p;
    }

    private static org.jreleaser.model.Project.Snapshot convertSnapshot(Project.Snapshot snapshot) {
        org.jreleaser.model.Project.Snapshot s = new org.jreleaser.model.Project.Snapshot();
        s.setPattern(tr(snapshot.getPattern()));
        s.setLabel(tr(snapshot.getLabel()));
        if (snapshot.isFullChangelogSet()) s.setFullChangelog(snapshot.isFullChangelog());
        return s;
    }

    private static org.jreleaser.model.Java convertJava(Java java) {
        org.jreleaser.model.Java j = new org.jreleaser.model.Java();
        j.setEnabled(true);
        j.setGroupId(tr(java.getGroupId()));
        j.setArtifactId(tr(java.getArtifactId()));
        j.setVersion(tr(java.getVersion()));
        j.setMainModule(tr(java.getMainModule()));
        j.setMainClass(tr(java.getMainClass()));
        if (java.isMultiProjectSet()) j.setMultiProject(java.isMultiProject());
        return j;
    }

    private static org.jreleaser.model.Release convertRelease(Release release) {
        org.jreleaser.model.Release r = new org.jreleaser.model.Release();
        r.setGithub(convertGithub(release.getGithub()));
        r.setGitlab(convertGitlab(release.getGitlab()));
        r.setGitea(convertGitea(release.getGitea()));
        r.setCodeberg(convertCodeberg(release.getCodeberg()));
        r.setGeneric(convertGeneric(release.getGeneric()));
        return r;
    }

    private static org.jreleaser.model.Github convertGithub(Github github) {
        if (null == github) return null;
        org.jreleaser.model.Github g = new org.jreleaser.model.Github();
        convertGitService(github, g);
        g.setDraft(github.isDraft());
        g.setPrerelease(convertPrerelease(github.getPrerelease()));
        g.setDiscussionCategoryName(tr(github.getDiscussionCategoryName()));
        return g;
    }

    private static org.jreleaser.model.Gitlab convertGitlab(Gitlab gitlab) {
        if (null == gitlab) return null;
        org.jreleaser.model.Gitlab g = new org.jreleaser.model.Gitlab();
        convertGitService(gitlab, g);
        g.setIdentifier(tr(gitlab.getIdentifier()));
        g.setUploadLinks(gitlab.getUploadLinks());
        return g;
    }

    private static org.jreleaser.model.Gitea convertGitea(Gitea gitea) {
        if (null == gitea) return null;
        org.jreleaser.model.Gitea g = new org.jreleaser.model.Gitea();
        convertGitService(gitea, g);
        g.setDraft(gitea.isDraft());
        g.setPrerelease(convertPrerelease(gitea.getPrerelease()));
        return g;
    }

    private static org.jreleaser.model.Codeberg convertCodeberg(Codeberg codeberg) {
        if (null == codeberg) return null;
        org.jreleaser.model.Codeberg g = new org.jreleaser.model.Codeberg();
        convertGitService(codeberg, g);
        g.setDraft(codeberg.isDraft());
        g.setPrerelease(convertPrerelease(codeberg.getPrerelease()));
        return g;
    }

    private static org.jreleaser.model.GitService.Prerelease convertPrerelease(GitService.Prerelease prerelease) {
        org.jreleaser.model.GitService.Prerelease s = new org.jreleaser.model.GitService.Prerelease();
        s.setPattern(tr(prerelease.getPattern()));
        s.setEnabled(prerelease.getEnabled());
        return s;
    }

    private static org.jreleaser.model.GenericGit convertGeneric(GenericGit generic) {
        if (null == generic) return null;
        org.jreleaser.model.GenericGit g = new org.jreleaser.model.GenericGit();
        convertGitService(generic, g);
        return g;
    }

    private static void convertGitService(GitService service, org.jreleaser.model.GitService s) {
        if (isNotBlank(service.getHost())) s.setHost(tr(service.getHost()));
        if (isNotBlank(service.getOwner())) s.setOwner(tr(service.getOwner()));
        if (isNotBlank(service.getName())) s.setName(tr(service.getName()));
        if (isNotBlank(service.getRepoUrl())) s.setRepoUrl(tr(service.getRepoUrl()));
        if (isNotBlank(service.getRepoCloneUrl())) s.setRepoCloneUrl(tr(service.getRepoCloneUrl()));
        if (isNotBlank(service.getCommitUrl())) s.setCommitUrl(tr(service.getCommitUrl()));
        if (isNotBlank(service.getSrcUrl())) s.setCommitUrl(tr(service.getSrcUrl()));
        if (isNotBlank(service.getDownloadUrl())) s.setDownloadUrl(tr(service.getDownloadUrl()));
        if (isNotBlank(service.getReleaseNotesUrl())) s.setReleaseNotesUrl(tr(service.getReleaseNotesUrl()));
        if (isNotBlank(service.getLatestReleaseUrl())) s.setLatestReleaseUrl(tr(service.getLatestReleaseUrl()));
        if (isNotBlank(service.getIssueTrackerUrl())) s.setIssueTrackerUrl(tr(service.getIssueTrackerUrl()));
        if (isNotBlank(service.getUsername())) s.setUsername(tr(service.getUsername()));
        if (isNotBlank(service.getToken())) s.setToken(tr(service.getToken()));
        if (isNotBlank(service.getTagName())) s.setTagName(tr(service.getTagName()));
        if (isNotBlank(service.getPreviousTagName())) s.setPreviousTagName(tr(service.getPreviousTagName()));
        if (isNotBlank(service.getReleaseName())) s.setReleaseName(tr(service.getReleaseName()));
        if (isNotBlank(service.getBranch())) s.setBranch(tr(service.getBranch()));
        s.setCommitAuthor(convertCommitAuthor(service.getCommitAuthor()));
        if (service.isSignSet()) s.setSign(service.isSign());
        if (service.isSkipTagSet()) s.setSkipTag(service.isSkipTag());
        if (service.isSkipReleaseSet()) s.setSkipRelease(service.isSkipRelease());
        if (service.isOverwriteSet()) s.setOverwrite(service.isOverwrite());
        s.setUpdate(convertUpdate(service.getUpdate()));
        if (isNotBlank(service.getApiEndpoint())) s.setApiEndpoint(tr(service.getApiEndpoint()));
        s.setChangelog(convertChangelog(service.getChangelog()));
        s.setMilestone(convertMilestone(service.getMilestone()));
        s.setConnectTimeout(service.getConnectTimeout());
        s.setReadTimeout(service.getReadTimeout());
        if (service.isArtifactsSet()) s.setArtifacts(service.isArtifacts());
        if (service.isFilesSet()) s.setFiles(service.isFiles());
        if (service.isChecksumsSet()) s.setChecksums(service.isChecksums());
        if (service.isSignaturesSet()) s.setSignatures(service.isSignatures());
        s.setUploadAssets(tr(service.resolveUploadAssets()));
    }

    private static org.jreleaser.model.GitService.Update convertUpdate(GitService.Update update) {
        org.jreleaser.model.GitService.Update u = new org.jreleaser.model.GitService.Update();
        if (update.isEnabledSet()) u.setEnabled(update.isEnabled());
        u.setSections(convertUpdateSections(update.getSections()));
        return u;
    }

    private static Set<org.jreleaser.model.UpdateSection> convertUpdateSections(Set<UpdateSection> updateSections) {
        return updateSections.stream()
            .map(s -> org.jreleaser.model.UpdateSection.of(s.name()))
            .collect(Collectors.toSet());
    }

    private static org.jreleaser.model.CommitAuthor convertCommitAuthor(CommitAuthor commitAuthor) {
        org.jreleaser.model.CommitAuthor ca = new org.jreleaser.model.CommitAuthor();
        ca.setName(tr(commitAuthor.getName()));
        ca.setEmail(tr(commitAuthor.getEmail()));
        return ca;
    }

    private static org.jreleaser.model.Changelog convertChangelog(Changelog changelog) {
        org.jreleaser.model.Changelog c = new org.jreleaser.model.Changelog();
        if (changelog.isEnabledSet()) c.setEnabled(changelog.isEnabled());
        if (changelog.isLinksSet()) c.setLinks(changelog.isLinks());
        c.setSort(changelog.getSort().name());
        c.setExternal(tr(changelog.getExternal()));
        c.setFormatted(tr(changelog.resolveFormatted()));
        c.getIncludeLabels().addAll(tr(changelog.getIncludeLabels()));
        c.getExcludeLabels().addAll(tr(changelog.getExcludeLabels()));
        c.setFormat(tr(changelog.getFormat()));
        c.setContent(tr(changelog.getContent()));
        c.setContentTemplate(tr(changelog.getContentTemplate()));
        c.setPreset(tr(changelog.getPreset()));
        c.setCategories(convertCategories(changelog.getCategories()));
        c.setLabelers(convertLabelers(changelog.getLabelers()));
        c.setReplacers(convertReplacers(changelog.getReplacers()));
        c.setHide(convertHide(changelog.getHide()));
        c.setContributors(convertContributors(changelog.getContributors()));
        return c;
    }

    private static org.jreleaser.model.Changelog.Hide convertHide(Changelog.Hide hide) {
        org.jreleaser.model.Changelog.Hide h = new org.jreleaser.model.Changelog.Hide();
        h.setUncategorized(hide.isUncategorized());
        h.setCategories(tr(hide.getCategories()));
        h.setContributors(tr(hide.getContributors()));
        return h;
    }

    private static org.jreleaser.model.Changelog.Contributors convertContributors(Changelog.Contributors contributors) {
        org.jreleaser.model.Changelog.Contributors c = new org.jreleaser.model.Changelog.Contributors();
        if (contributors.isEnabledSet()) c.setEnabled(contributors.isEnabled());
        c.setFormat(tr(contributors.getFormat()));
        return c;
    }

    private static Set<org.jreleaser.model.Changelog.Category> convertCategories(Collection<Changelog.Category> categories) {
        Set<org.jreleaser.model.Changelog.Category> set = new LinkedHashSet<>();
        for (Changelog.Category category : categories) {
            org.jreleaser.model.Changelog.Category c = new org.jreleaser.model.Changelog.Category();
            c.setKey(tr(category.getKey()));
            c.setTitle(tr(category.getTitle()));
            c.setLabels(category.getLabels());
            c.setFormat(tr(category.getFormat()));
            c.setOrder(category.getOrder());
            set.add(c);
        }
        return set;
    }

    private static Set<org.jreleaser.model.Changelog.Labeler> convertLabelers(Collection<Changelog.Labeler> labelers) {
        Set<org.jreleaser.model.Changelog.Labeler> set = new LinkedHashSet<>();
        for (Changelog.Labeler labeler : labelers) {
            org.jreleaser.model.Changelog.Labeler l = new org.jreleaser.model.Changelog.Labeler();
            l.setLabel(tr(labeler.getLabel()));
            l.setTitle(tr(labeler.getTitle()));
            l.setBody(tr(labeler.getBody()));
            l.setOrder(labeler.getOrder());
            set.add(l);
        }
        return set;
    }

    private static List<org.jreleaser.model.Changelog.Replacer> convertReplacers(List<Changelog.Replacer> replacers) {
        List<org.jreleaser.model.Changelog.Replacer> set = new ArrayList<>();
        for (Changelog.Replacer replacer : replacers) {
            org.jreleaser.model.Changelog.Replacer r = new org.jreleaser.model.Changelog.Replacer();
            r.setSearch(tr(replacer.getSearch()));
            r.setReplace(tr(replacer.getReplace()));
            set.add(r);
        }
        return set;
    }

    private static org.jreleaser.model.GitService.Milestone convertMilestone(GitService.Milestone milestone) {
        org.jreleaser.model.GitService.Milestone m = new org.jreleaser.model.GitService.Milestone();
        m.setClose(milestone.isClose());
        if (isNotBlank(milestone.getName())) m.setName(tr(milestone.getName()));
        return m;
    }

    private static org.jreleaser.model.Upload convertUpload(Upload upload) {
        org.jreleaser.model.Upload u = new org.jreleaser.model.Upload();
        if (upload.isEnabledSet()) u.setEnabled(upload.isEnabled());
        u.setArtifactory(convertArtifactory(upload.getArtifactory()));
        u.setHttp(convertHttp(upload.getHttp()));
        u.setS3(convertS3(upload.getS3()));
        return u;
    }

    private static Map<String, org.jreleaser.model.Artifactory> convertArtifactory(Map<String, Artifactory> artifactory) {
        Map<String, org.jreleaser.model.Artifactory> map = new LinkedHashMap<>();
        for (Map.Entry<String, Artifactory> e : artifactory.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertArtifactory(e.getValue()));
        }
        return map;
    }

    private static org.jreleaser.model.Artifactory convertArtifactory(Artifactory artifactory) {
        org.jreleaser.model.Artifactory a = new org.jreleaser.model.Artifactory();
        convertUploader(artifactory, a);
        a.setHost(tr(artifactory.getHost()));
        a.setUsername(tr(artifactory.getUsername()));
        a.setPassword(tr(artifactory.getPassword()));
        a.setAuthorization(tr(artifactory.resolveAuthorization().name()));
        a.setRepositories(convertRepositories(artifactory.getRepositories()));
        return a;
    }

    private static List<org.jreleaser.model.Artifactory.ArtifactoryRepository> convertRepositories(List<Artifactory.ArtifactoryRepository> repositories) {
        List<org.jreleaser.model.Artifactory.ArtifactoryRepository> list = new ArrayList<>();
        for (Artifactory.ArtifactoryRepository repository : repositories) {
            if (repository.isSet()) {
                list.add(convertRepository(repository));
            }
        }
        return list;
    }

    private static org.jreleaser.model.Artifactory.ArtifactoryRepository convertRepository(Artifactory.ArtifactoryRepository repository) {
        org.jreleaser.model.Artifactory.ArtifactoryRepository r = new org.jreleaser.model.Artifactory.ArtifactoryRepository();
        r.setActive(repository.resolveActive());
        r.setPath(tr(repository.getPath()));
        for (FileType fileType : repository.getFileTypes()) {
            r.addFileType(org.jreleaser.util.FileType.of(fileType.name()));
        }
        return r;
    }

    private static void convertUploader(Uploader from, org.jreleaser.model.Uploader into) {
        into.setName(tr(from.getName()));
        into.setActive(from.resolveActive());
        into.setExtraProperties(from.getExtraProperties());
        into.setConnectTimeout(from.getConnectTimeout());
        into.setReadTimeout(from.getReadTimeout());
        if (from.isArtifactsSet()) into.setArtifacts(from.isArtifacts());
        if (from.isFilesSet()) into.setFiles(from.isFiles());
        if (from.isSignaturesSet()) into.setSignatures(from.isSignatures());
        if (from.isChecksumsSet()) into.setChecksums(from.isChecksums());
        if (from instanceof HttpUploader) {
            convertHttpUploader((HttpUploader) from, (org.jreleaser.model.HttpUploader) into);
        }
    }

    private static void convertHttpUploader(HttpUploader from, org.jreleaser.model.HttpUploader into) {
        into.setUploadUrl(tr(from.getUploadUrl()));
        into.setDownloadUrl(tr(from.getDownloadUrl()));
    }

    private static Map<String, org.jreleaser.model.Http> convertHttp(Map<String, Http> http) {
        Map<String, org.jreleaser.model.Http> map = new LinkedHashMap<>();
        for (Map.Entry<String, Http> e : http.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertHttp(e.getValue()));
        }
        return map;
    }

    private static org.jreleaser.model.Http convertHttp(Http http) {
        org.jreleaser.model.Http h = new org.jreleaser.model.Http();
        convertUploader(http, h);
        h.setUsername(tr(http.getUsername()));
        h.setPassword(tr(http.getPassword()));
        h.setAuthorization(tr(http.resolveAuthorization().name()));
        h.setMethod(tr(http.resolveMethod().name()));
        h.setHeaders(http.getHeaders());
        return h;
    }

    private static Map<String, org.jreleaser.model.S3> convertS3(Map<String, S3> s3) {
        Map<String, org.jreleaser.model.S3> map = new LinkedHashMap<>();
        for (Map.Entry<String, S3> e : s3.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertS3(e.getValue()));
        }
        return map;
    }

    private static org.jreleaser.model.S3 convertS3(S3 s3) {
        org.jreleaser.model.S3 s = new org.jreleaser.model.S3();
        convertUploader(s3, s);
        s.setRegion(tr(s3.getRegion()));
        s.setBucket(tr(s3.getBucket()));
        s.setAccessKeyId(tr(s3.getAccessKeyId()));
        s.setSecretKey(tr(s3.getSecretKey()));
        s.setSessionToken(tr(s3.getSessionToken()));
        s.setEndpoint(tr(s3.getEndpoint()));
        s.setPath(tr(s3.getPath()));
        s.setDownloadUrl(tr(s3.getDownloadUrl()));
        s.setHeaders(s3.getHeaders());
        return s;
    }

    private static org.jreleaser.model.Packagers convertPackagers(Packagers packagers) {
        org.jreleaser.model.Packagers p = new org.jreleaser.model.Packagers();
        if (packagers.getBrew().isSet()) p.setBrew(convertBrew(packagers.getBrew()));
        if (packagers.getChocolatey().isSet()) p.setChocolatey(convertChocolatey(packagers.getChocolatey()));
        if (packagers.getDocker().isSet()) p.setDocker(convertDocker(packagers.getDocker()));
        if (packagers.getGofish().isSet()) p.setGofish(convertGofish(packagers.getGofish()));
        if (packagers.getJbang().isSet()) p.setJbang(convertJbang(packagers.getJbang()));
        if (packagers.getMacports().isSet()) p.setMacports(convertMacports(packagers.getMacports()));
        if (packagers.getScoop().isSet()) p.setScoop(convertScoop(packagers.getScoop()));
        if (packagers.getSdkman().isSet()) p.setSdkman(convertSdkman(packagers.getSdkman()));
        if (packagers.getSnap().isSet()) p.setSnap(convertSnap(packagers.getSnap()));
        if (packagers.getSpec().isSet()) p.setSpec(convertSpec(packagers.getSpec()));
        return p;
    }

    private static org.jreleaser.model.Announce convertAnnounce(Announce announce) {
        org.jreleaser.model.Announce a = new org.jreleaser.model.Announce();
        if (announce.isEnabledSet()) a.setEnabled(announce.isEnabled());
        if (announce.getArticle().isSet()) a.setArticle(convertArticle(announce.getArticle()));
        if (announce.getDiscord().isSet()) a.setDiscord(convertDiscord(announce.getDiscord()));
        if (announce.getDiscussions().isSet()) a.setDiscussions(convertDiscussions(announce.getDiscussions()));
        if (announce.getGitter().isSet()) a.setGitter(convertGitter(announce.getGitter()));
        if (announce.getGoogleChat().isSet()) a.setGoogleChat(convertGoogleChat(announce.getGoogleChat()));
        if (announce.getMail().isSet()) a.setMail(convertMail(announce.getMail()));
        if (announce.getMastodon().isSet()) a.setMastodon(convertMastodon(announce.getMastodon()));
        if (announce.getMattermost().isSet()) a.setMattermost(convertMattermost(announce.getMattermost()));
        if (announce.getSdkman().isSet()) a.setSdkman(convertSdkmanAnnouncer(announce.getSdkman()));
        if (announce.getSlack().isSet()) a.setSlack(convertSlack(announce.getSlack()));
        if (announce.getTeams().isSet()) a.setTeams(convertTeams(announce.getTeams()));
        if (announce.getTelegram().isSet()) a.setTelegram(convertTelegram(announce.getTelegram()));
        if (announce.getTwitter().isSet()) a.setTwitter(convertTwitter(announce.getTwitter()));
        if (announce.getZulip().isSet()) a.setZulip(convertZulip(announce.getZulip()));
        a.setWebhooks(convertWebhooks(announce.getWebhooks()));
        return a;
    }

    private static org.jreleaser.model.Article convertArticle(Article article) {
        org.jreleaser.model.Article a = new org.jreleaser.model.Article();
        a.setActive(tr(article.resolveActive()));
        a.setExtraProperties(article.getExtraProperties());
        a.setFiles(convertArtifacts(article.getFiles()));
        a.setTemplateDirectory(tr(article.getTemplateDirectory()));
        a.setCommitAuthor(convertCommitAuthor(article.getCommitAuthor()));
        a.setRepository(convertRepository(article.getRepository()));
        return a;
    }

    private static Repository convertRepository(Tap tap) {
        Repository t = new Repository();
        convertTap(tap, t);
        return t;
    }

    private static void convertTap(Tap from, RepositoryTap into) {
        into.setActive(tr(from.resolveActive()));
        into.setOwner(tr(from.getOwner()));
        into.setName(tr(from.getName()));
        into.setTagName(tr(from.getTagName()));
        into.setBranch(tr(from.getBranch()));
        into.setUsername(tr(from.getUsername()));
        into.setToken(tr(from.getToken()));
        into.setCommitMessage(tr(from.getCommitMessage()));
    }

    private static void convertAnnouncer(Announcer from, org.jreleaser.model.Announcer into) {
        into.setActive(tr(from.resolveActive()));
        into.setConnectTimeout(from.getConnectTimeout());
        into.setReadTimeout(from.getReadTimeout());
        into.setExtraProperties(from.getExtraProperties());
    }

    private static org.jreleaser.model.Discord convertDiscord(Discord discord) {
        org.jreleaser.model.Discord a = new org.jreleaser.model.Discord();
        convertAnnouncer(discord, a);
        a.setWebhook(tr(discord.getWebhook()));
        a.setMessage(tr(discord.getMessage()));
        a.setMessageTemplate(tr(discord.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Discussions convertDiscussions(Discussions discussions) {
        org.jreleaser.model.Discussions a = new org.jreleaser.model.Discussions();
        convertAnnouncer(discussions, a);
        a.setOrganization(tr(discussions.getOrganization()));
        a.setTeam(tr(discussions.getTeam()));
        a.setTitle(tr(discussions.getTitle()));
        a.setMessage(tr(discussions.getMessage()));
        a.setMessageTemplate(tr(discussions.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Gitter convertGitter(Gitter gitter) {
        org.jreleaser.model.Gitter a = new org.jreleaser.model.Gitter();
        convertAnnouncer(gitter, a);
        a.setWebhook(tr(gitter.getWebhook()));
        a.setMessage(tr(gitter.getMessage()));
        a.setMessageTemplate(tr(gitter.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.GoogleChat convertGoogleChat(GoogleChat googleChat) {
        org.jreleaser.model.GoogleChat a = new org.jreleaser.model.GoogleChat();
        convertAnnouncer(googleChat, a);
        a.setWebhook(tr(googleChat.getWebhook()));
        a.setMessage(tr(googleChat.getMessage()));
        a.setMessageTemplate(tr(googleChat.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Mail convertMail(Mail mail) {
        org.jreleaser.model.Mail a = new org.jreleaser.model.Mail();
        convertAnnouncer(mail, a);
        if (mail.isAuthSet()) a.setAuth(mail.isAuth());
        if (null != mail.getTransport()) a.setTransport(mail.getTransport().name());
        if (null != mail.getMimeType()) a.setMimeType(mail.getMimeType().name());
        a.setPort(mail.getPort());
        a.setUsername(tr(mail.getUsername()));
        a.setPassword(tr(mail.getPassword()));
        a.setFrom(tr(mail.getFrom()));
        a.setTo(tr(mail.getTo()));
        a.setCc(tr(mail.getCc()));
        a.setBcc(tr(mail.getBcc()));
        a.setSubject(tr(mail.getSubject()));
        a.setMessage(tr(mail.getMessage()));
        a.setMessageTemplate(tr(mail.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Mastodon convertMastodon(Mastodon mastodon) {
        org.jreleaser.model.Mastodon a = new org.jreleaser.model.Mastodon();
        convertAnnouncer(mastodon, a);
        a.setHost(tr(mastodon.getHost()));
        a.setAccessToken(tr(mastodon.getAccessToken()));
        a.setStatus(tr(mastodon.getStatus()));
        return a;
    }

    private static org.jreleaser.model.Mattermost convertMattermost(Mattermost mattermost) {
        org.jreleaser.model.Mattermost a = new org.jreleaser.model.Mattermost();
        convertAnnouncer(mattermost, a);
        a.setWebhook(tr(mattermost.getWebhook()));
        a.setMessage(tr(mattermost.getMessage()));
        a.setMessageTemplate(tr(mattermost.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.SdkmanAnnouncer convertSdkmanAnnouncer(SdkmanAnnouncer sdkman) {
        org.jreleaser.model.SdkmanAnnouncer a = new org.jreleaser.model.SdkmanAnnouncer();
        convertAnnouncer(sdkman, a);
        a.setConsumerKey(tr(sdkman.getConsumerKey()));
        a.setConsumerToken(tr(sdkman.getConsumerToken()));
        a.setCandidate(tr(sdkman.getCandidate()));
        a.setReleaseNotesUrl(tr(sdkman.getReleaseNotesUrl()));
        a.setDownloadUrl(tr(sdkman.getDownloadUrl()));
        a.setCommand(sdkman.resolveCommand());
        return a;
    }

    private static org.jreleaser.model.Slack convertSlack(Slack slack) {
        org.jreleaser.model.Slack a = new org.jreleaser.model.Slack();
        convertAnnouncer(slack, a);
        a.setToken(tr(slack.getToken()));
        a.setWebhook(tr(slack.getWebhook()));
        a.setChannel(tr(slack.getChannel()));
        a.setMessage(tr(slack.getMessage()));
        a.setMessageTemplate(tr(slack.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Teams convertTeams(Teams teams) {
        org.jreleaser.model.Teams a = new org.jreleaser.model.Teams();
        convertAnnouncer(teams, a);
        a.setWebhook(tr(teams.getWebhook()));
        a.setMessageTemplate(tr(teams.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Telegram convertTelegram(Telegram telegram) {
        org.jreleaser.model.Telegram a = new org.jreleaser.model.Telegram();
        convertAnnouncer(telegram, a);
        a.setToken(tr(telegram.getToken()));
        a.setChatId(tr(telegram.getChatId()));
        a.setMessage(tr(telegram.getMessage()));
        a.setMessageTemplate(tr(telegram.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Twitter convertTwitter(Twitter twitter) {
        org.jreleaser.model.Twitter a = new org.jreleaser.model.Twitter();
        convertAnnouncer(twitter, a);
        a.setConsumerKey(tr(twitter.getConsumerKey()));
        a.setConsumerSecret(tr(twitter.getConsumerSecret()));
        a.setAccessToken(tr(twitter.getAccessToken()));
        a.setAccessTokenSecret(tr(twitter.getAccessTokenSecret()));
        a.setStatus(tr(twitter.getStatus()));
        return a;
    }

    private static org.jreleaser.model.Zulip convertZulip(Zulip zulip) {
        org.jreleaser.model.Zulip a = new org.jreleaser.model.Zulip();
        convertAnnouncer(zulip, a);
        a.setAccount(tr(zulip.getAccount()));
        a.setApiKey(tr(zulip.getApiKey()));
        a.setApiHost(tr(zulip.getApiHost()));
        a.setChannel(tr(zulip.getChannel()));
        a.setSubject(tr(zulip.getSubject()));
        a.setMessage(tr(zulip.getMessage()));
        a.setMessageTemplate(tr(zulip.getMessageTemplate()));
        return a;
    }

    private static Map<String, org.jreleaser.model.Webhook> convertWebhooks(Map<String, Webhook> webhooks) {
        Map<String, org.jreleaser.model.Webhook> ds = new LinkedHashMap<>();
        for (Map.Entry<String, Webhook> e : webhooks.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            ds.put(e.getValue().getName(), convertWebhook(e.getValue()));
        }
        return ds;
    }

    private static org.jreleaser.model.Webhook convertWebhook(Webhook webhook) {
        org.jreleaser.model.Webhook a = new org.jreleaser.model.Webhook();
        convertAnnouncer(webhook, a);
        a.setWebhook(tr(webhook.getWebhook()));
        a.setMessage(tr(webhook.getMessage()));
        a.setMessageProperty(tr(webhook.getMessageProperty()));
        a.setMessageTemplate(tr(webhook.getMessageTemplate()));
        return a;
    }

    private static org.jreleaser.model.Assemble convertAssemble(Assemble assemble) {
        org.jreleaser.model.Assemble a = new org.jreleaser.model.Assemble();
        if (assemble.isEnabledSet()) a.setEnabled(assemble.isEnabled());
        a.setArchive(convertArchive(assemble.getArchive()));
        a.setJlink(convertJlink(assemble.getJlink()));
        a.setJpackage(convertJpackage(assemble.getJpackage()));
        a.setNativeImage(convertNativeImage(assemble.getNativeImage()));
        return a;
    }

    private static Map<String, org.jreleaser.model.Archive> convertArchive(Map<String, Archive> archive) {
        Map<String, org.jreleaser.model.Archive> map = new LinkedHashMap<>();
        for (Map.Entry<String, Archive> e : archive.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertArchive(e.getValue()));
        }
        return map;
    }

    private static org.jreleaser.model.Archive convertArchive(Archive archive) {
        org.jreleaser.model.Archive a = new org.jreleaser.model.Archive();
        convertAssembler(archive, a);
        a.setArchiveName(tr(archive.getArchiveName()));
        if (archive.getDistributionType() != null) a.setDistributionType(tr(archive.getDistributionType().name()));
        if (archive.isAttachPlatformSet()) a.setAttachPlatform(archive.isAttachPlatform());
        a.setFormats(archive.getFormats().stream()
            .map(Object::toString)
            .map(org.jreleaser.model.Archive.Format::valueOf)
            .collect(Collectors.toSet()));
        a.setFileSets(convertFileSets(archive.getFileSets()));
        return a;
    }

    private static void convertAssembler(Assembler from, org.jreleaser.model.Assembler into) {
        into.setExported(from.isExported());
        into.setName(tr(from.getName()));
        into.setActive(tr(from.resolveActive()));
        into.setExtraProperties(from.getExtraProperties());
        into.setPlatform(convertPlatform(from.getPlatform()));
    }

    private static List<org.jreleaser.model.FileSet> convertFileSets(List<FileSet> fileSets) {
        return fileSets.stream()
            .map(JReleaserModelConverter::convertFileSet)
            .collect(Collectors.toList());
    }

    private static org.jreleaser.model.FileSet convertFileSet(FileSet fileSet) {
        org.jreleaser.model.FileSet f = new org.jreleaser.model.FileSet();
        f.setInput(tr(fileSet.getInput()));
        f.setOutput(tr(fileSet.getOutput()));
        f.setIncludes(tr(fileSet.getIncludes()));
        f.setExcludes(tr(fileSet.getExcludes()));
        f.setExtraProperties(fileSet.getExtraProperties());
        if (fileSet.isFailOnMissingInputSet()) f.setFailOnMissingInput(fileSet.isFailOnMissingInput());
        return f;
    }

    private static Map<String, org.jreleaser.model.Jlink> convertJlink(Map<String, Jlink> jlink) {
        Map<String, org.jreleaser.model.Jlink> map = new LinkedHashMap<>();
        for (Map.Entry<String, Jlink> e : jlink.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertJlink(e.getValue()));
        }
        return map;
    }

    private static org.jreleaser.model.Jlink convertJlink(Jlink jlink) {
        org.jreleaser.model.Jlink a = new org.jreleaser.model.Jlink();
        convertJavaAssembler(jlink, a);
        a.setTargetJdks(convertArtifacts(jlink.getTargetJdks()));
        a.setModuleNames(tr(jlink.getModuleNames()));
        a.setAdditionalModuleNames(tr(jlink.getAdditionalModuleNames()));
        a.setArgs(tr(jlink.getArgs()));
        a.setJdeps(convertJdeps(jlink.getJdeps()));
        a.setJdk(convertArtifact(jlink.getJdk()));
        a.setImageName(tr(jlink.getImageName()));
        a.setImageNameTransform(tr(jlink.getImageNameTransform()));
        if (jlink.isCopyJarsSet()) a.setCopyJars(jlink.isCopyJars());
        return a;
    }

    private static Map<String, org.jreleaser.model.Jpackage> convertJpackage(Map<String, Jpackage> jpackage) {
        Map<String, org.jreleaser.model.Jpackage> map = new LinkedHashMap<>();
        for (Map.Entry<String, Jpackage> e : jpackage.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertJpackage(e.getValue()));
        }
        return map;
    }

    private static org.jreleaser.model.Jpackage convertJpackage(Jpackage jpackage) {
        org.jreleaser.model.Jpackage a = new org.jreleaser.model.Jpackage();
        convertJavaAssembler(jpackage, a);
        a.setJlink(tr(jpackage.getJlink()));
        if (jpackage.isAttachPlatformSet()) a.setAttachPlatform(jpackage.isAttachPlatform());
        if (jpackage.isVerboseSet()) a.setVerbose(jpackage.isVerbose());
        a.setRuntimeImages(convertArtifacts(jpackage.getRuntimeImages()));
        a.setApplicationPackage(convertApplicationPackage(jpackage.getApplicationPackage()));
        a.setLauncher(convertLauncher(jpackage.getLauncher()));
        a.setLinux(convertLinux(jpackage.getLinux()));
        a.setWindows(convertWindows(jpackage.getWindows()));
        a.setOsx(convertOsx(jpackage.getOsx()));
        return a;
    }

    private static org.jreleaser.model.Jpackage.ApplicationPackage convertApplicationPackage(Jpackage.ApplicationPackage applicationPackage) {
        org.jreleaser.model.Jpackage.ApplicationPackage a = new org.jreleaser.model.Jpackage.ApplicationPackage();
        a.setFileAssociations(tr(applicationPackage.getFileAssociations()));
        a.setAppName(tr(applicationPackage.getAppName()));
        a.setAppVersion(tr(applicationPackage.getAppVersion()));
        a.setVendor(tr(applicationPackage.getVendor()));
        a.setCopyright(tr(applicationPackage.getCopyright()));
        a.setLicenseFile(tr(applicationPackage.getLicenseFile()));
        return a;
    }

    private static org.jreleaser.model.Jpackage.Launcher convertLauncher(Jpackage.Launcher launcher) {
        org.jreleaser.model.Jpackage.Launcher a = new org.jreleaser.model.Jpackage.Launcher();
        a.setArguments(tr(launcher.getArguments()));
        a.setJavaOptions(tr(launcher.getJavaOptions()));
        a.addLaunchers(tr(launcher.getLaunchers()));
        return a;
    }

    private static void convertPackager(Jpackage.PlatformPackager from, org.jreleaser.model.Jpackage.PlatformPackager into) {
        into.setAppName(tr(from.getAppName()));
        into.setIcon(tr(from.getIcon()));
        into.setJdk(convertArtifact(from.getJdk()));
        into.setTypes(tr(from.getTypes()));
        into.setInstallDir(tr(from.getInstallDir()));
        into.setResourceDir(tr(from.getResourceDir()));
    }

    private static org.jreleaser.model.Jpackage.Linux convertLinux(Jpackage.Linux linux) {
        org.jreleaser.model.Jpackage.Linux a = new org.jreleaser.model.Jpackage.Linux();
        convertPackager(linux, a);
        if (linux.isShortcutSet()) a.setShortcut(linux.isShortcut());
        a.setPackageName(tr(linux.getPackageName()));
        a.setMaintainer(tr(linux.getMaintainer()));
        a.setMenuGroup(tr(linux.getMenuGroup()));
        a.setLicense(tr(linux.getLicense()));
        a.setAppRelease(tr(linux.getAppRelease()));
        a.setAppCategory(tr(linux.getAppCategory()));
        a.setPackageDeps(tr(linux.getPackageDeps()));
        return a;
    }

    private static org.jreleaser.model.Jpackage.Windows convertWindows(Jpackage.Windows windows) {
        org.jreleaser.model.Jpackage.Windows a = new org.jreleaser.model.Jpackage.Windows();
        convertPackager(windows, a);
        if (windows.isConsoleSet()) a.setConsole(windows.isConsole());
        if (windows.isDirChooserSet()) a.setDirChooser(windows.isDirChooser());
        if (windows.isMenuSet()) a.setMenu(windows.isMenu());
        if (windows.isPerUserInstallSet()) a.setPerUserInstall(windows.isPerUserInstall());
        if (windows.isShortcutSet()) a.setShortcut(windows.isShortcut());
        a.setMenuGroup(tr(windows.getMenuGroup()));
        a.setUpgradeUuid(tr(windows.getUpgradeUuid()));
        return a;
    }

    private static org.jreleaser.model.Jpackage.Osx convertOsx(Jpackage.Osx osx) {
        org.jreleaser.model.Jpackage.Osx a = new org.jreleaser.model.Jpackage.Osx();
        convertPackager(osx, a);
        a.setPackageName(tr(osx.getPackageName()));
        a.setPackageIdentifier(tr(osx.getPackageIdentifier()));
        a.setPackageSigningPrefix(tr(osx.getPackageSigningPrefix()));
        a.setSigningKeychain(tr(osx.getSigningKeychain()));
        a.setSigningKeyUsername(tr(osx.getSigningKeyUsername()));
        if (osx.isSignSet()) a.setSign(osx.isSign());
        return a;
    }

    private static org.jreleaser.model.Jlink.Jdeps convertJdeps(Jlink.Jdeps jdeps) {
        org.jreleaser.model.Jlink.Jdeps j = new org.jreleaser.model.Jlink.Jdeps();
        j.setMultiRelease(jdeps.getMultiRelease());
        if (jdeps.isIgnoreMissingDepsSet()) j.setIgnoreMissingDeps(jdeps.isIgnoreMissingDeps());
        if (jdeps.isUseWildcardInPathSet()) j.setUseWildcardInPath(jdeps.isUseWildcardInPath());
        j.setTargets(tr(jdeps.getTargets()));
        return j;
    }

    private static Map<String, org.jreleaser.model.NativeImage> convertNativeImage(Map<String, NativeImage> nativeImage) {
        Map<String, org.jreleaser.model.NativeImage> map = new LinkedHashMap<>();
        for (Map.Entry<String, NativeImage> e : nativeImage.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            map.put(e.getValue().getName(), convertNativeImage(e.getValue()));
        }
        return map;
    }

    private static void convertJavaAssembler(JavaAssembler from, org.jreleaser.model.JavaAssembler into) {
        convertAssembler(from, into);
        into.setExecutable(tr(from.getExecutable()));
        into.setJava(convertJava(from.getJava()));
        into.setTemplateDirectory(tr(from.getTemplateDirectory()));
        into.setMainJar(convertArtifact(from.getMainJar()));
        into.setJars(convertGlobs(from.getJars()));
        into.setFiles(convertGlobs(from.getFiles()));
        into.setFileSets(convertFileSets(from.getFileSets()));
    }

    private static org.jreleaser.model.NativeImage convertNativeImage(NativeImage nativeImage) {
        org.jreleaser.model.NativeImage a = new org.jreleaser.model.NativeImage();
        convertJavaAssembler(nativeImage, a);
        a.setGraal(convertArtifact(nativeImage.getGraal()));
        a.setGraalJdks(convertArtifacts(nativeImage.getGraalJdks()));
        a.setImageName(tr(nativeImage.getImageName()));
        a.setImageNameTransform(tr(nativeImage.getImageNameTransform()));
        a.setArgs(tr(nativeImage.getArgs()));
        a.setUpx(convertUpx(nativeImage.getUpx()));
        a.setLinux(convertLinux(nativeImage.getLinux()));
        a.setWindows(convertWindows(nativeImage.getWindows()));
        a.setOsx(convertOsx(nativeImage.getOsx()));
        return a;
    }

    private static org.jreleaser.model.NativeImage.Upx convertUpx(NativeImage.Upx upx) {
        org.jreleaser.model.NativeImage.Upx a = new org.jreleaser.model.NativeImage.Upx();
        a.setActive(tr(upx.resolveActive()));
        a.setVersion(tr(upx.getVersion()));
        a.setArgs(tr(upx.getArgs()));
        return a;
    }

    private static org.jreleaser.model.NativeImage.Linux convertLinux(NativeImage.Linux linux) {
        org.jreleaser.model.NativeImage.Linux a = new org.jreleaser.model.NativeImage.Linux();
        a.setArgs(tr(linux.getArgs()));
        return a;
    }

    private static org.jreleaser.model.NativeImage.Windows convertWindows(NativeImage.Windows windows) {
        org.jreleaser.model.NativeImage.Windows a = new org.jreleaser.model.NativeImage.Windows();
        a.setArgs(tr(windows.getArgs()));
        return a;
    }

    private static org.jreleaser.model.NativeImage.Osx convertOsx(NativeImage.Osx osx) {
        org.jreleaser.model.NativeImage.Osx a = new org.jreleaser.model.NativeImage.Osx();
        a.setArgs(tr(osx.getArgs()));
        return a;
    }

    private static org.jreleaser.model.Checksum convertChecksum(Checksum checksum) {
        org.jreleaser.model.Checksum s = new org.jreleaser.model.Checksum();
        s.setName(tr(checksum.getName()));
        s.setIndividual(checksum.isIndividual());
        s.setAlgorithms(checksum.getAlgorithms());
        if (checksum.isFilesSet()) s.setFiles(checksum.isFiles());
        return s;
    }

    private static org.jreleaser.model.Signing convertSigning(Signing signing) {
        org.jreleaser.model.Signing s = new org.jreleaser.model.Signing();
        s.setActive(tr(signing.resolveActive()));
        s.setArmored(signing.isArmored());
        s.setPublicKey(tr(signing.getPublicKey()));
        s.setSecretKey(tr(signing.getSecretKey()));
        s.setPassphrase(tr(signing.getPassphrase()));
        s.setMode(tr(signing.resolveMode()));
        if (signing.isArtifactsSet()) s.setArtifacts(signing.isArtifacts());
        if (signing.isFilesSet()) s.setFiles(signing.isFiles());
        if (signing.isChecksumsSet()) s.setChecksums(signing.isChecksums());
        s.setCommand(convertSigningCommand(signing.getCommand()));
        s.setCosign(convertCosign(signing.getCosign()));
        return s;
    }

    private static org.jreleaser.model.Signing.Command convertSigningCommand(Signing.Command command) {
        org.jreleaser.model.Signing.Command c = new org.jreleaser.model.Signing.Command();
        if (command.isDefaultKeyringSet()) c.setDefaultKeyring(command.isDefaultKeyring());
        c.setExecutable(tr(command.getExecutable()));
        c.setKeyName(tr(command.getKeyName()));
        c.setHomeDir(tr(command.getHomeDir()));
        c.setPublicKeyring(tr(command.getPublicKeyring()));
        c.setArgs(tr(command.getArgs()));
        return c;
    }

    private static org.jreleaser.model.Signing.Cosign convertCosign(Signing.Cosign cosign) {
        org.jreleaser.model.Signing.Cosign c = new org.jreleaser.model.Signing.Cosign();
        c.setVersion(tr(cosign.getVersion()));
        c.setPrivateKeyFile(tr(cosign.getPrivateKeyFile()));
        c.setPublicKeyFile(tr(cosign.getPublicKeyFile()));
        return c;
    }

    private static Map<String, org.jreleaser.model.Distribution> convertDistributions(Map<String, Distribution> distributions) {
        Map<String, org.jreleaser.model.Distribution> ds = new LinkedHashMap<>();
        for (Map.Entry<String, Distribution> e : distributions.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            ds.put(e.getValue().getName(), convertDistribution(e.getValue()));
        }
        return ds;
    }

    private static org.jreleaser.model.Distribution convertDistribution(Distribution distribution) {
        org.jreleaser.model.Distribution d = new org.jreleaser.model.Distribution();
        d.setActive(tr(distribution.resolveActive()));
        d.setName(tr(distribution.getName()));
        d.setType(tr(distribution.getType().name()));
        d.setExecutable(convertExecutable(distribution.getExecutable()));
        d.setJava(convertJava(distribution.getJava()));
        d.setPlatform(convertPlatform(distribution.getPlatform()));
        d.setTags(tr(distribution.getTags()));
        d.setExtraProperties(distribution.getExtraProperties());
        d.setArtifacts(convertArtifacts(distribution.getArtifacts()));

        if (distribution.getBrew().isSet()) d.setBrew(convertBrew(distribution.getBrew()));
        if (distribution.getChocolatey().isSet()) d.setChocolatey(convertChocolatey(distribution.getChocolatey()));
        if (distribution.getDocker().isSet()) d.setDocker(convertDocker(distribution.getDocker()));
        if (distribution.getGofish().isSet()) d.setGofish(convertGofish(distribution.getGofish()));
        if (distribution.getJbang().isSet()) d.setJbang(convertJbang(distribution.getJbang()));
        if (distribution.getMacports().isSet()) d.setMacports(convertMacports(distribution.getMacports()));
        if (distribution.getScoop().isSet()) d.setScoop(convertScoop(distribution.getScoop()));
        if (distribution.getSdkman().isSet()) d.setSdkman(convertSdkman(distribution.getSdkman()));
        if (distribution.getSnap().isSet()) d.setSnap(convertSnap(distribution.getSnap()));
        if (distribution.getSpec().isSet()) d.setSpec(convertSpec(distribution.getSpec()));

        return d;
    }

    private static org.jreleaser.model.Distribution.Executable convertExecutable(Distribution.Executable executable) {
        org.jreleaser.model.Distribution.Executable e = new org.jreleaser.model.Distribution.Executable();
        e.setName(tr(executable.getName()));
        e.setUnixExtension(tr(executable.getUnixExtension()));
        if (isNotBlank(executable.getWindowsExtension())) e.setWindowsExtension(tr(executable.getWindowsExtension()));
        return e;
    }

    private static org.jreleaser.model.Files convertFiles(Files files) {
        org.jreleaser.model.Files fs = new org.jreleaser.model.Files();
        fs.setActive(tr(files.resolveActive()));
        fs.setArtifacts(convertArtifacts(files.getArtifacts()));
        fs.setGlobs(convertGlobs(files.getGlobs()));
        return fs;
    }

    private static Set<org.jreleaser.model.Artifact> convertArtifacts(Set<Artifact> artifacts) {
        Set<org.jreleaser.model.Artifact> as = new LinkedHashSet<>();
        for (Artifact artifact : artifacts) {
            as.add(convertArtifact(artifact));
        }
        return as;
    }

    private static org.jreleaser.model.Artifact convertArtifact(Artifact artifact) {
        org.jreleaser.model.Artifact a = new org.jreleaser.model.Artifact();
        a.setPath(tr(artifact.getPath()));
        a.setTransform(tr(artifact.getTransform()));
        a.setPlatform(tr(artifact.getPlatform()));
        a.setExtraProperties(artifact.getExtraProperties());
        return a;
    }

    private static List<org.jreleaser.model.Glob> convertGlobs(List<Glob> globs) {
        List<org.jreleaser.model.Glob> gs = new ArrayList<>();
        for (Glob glob : globs) {
            gs.add(convertGlob(glob));
        }
        return gs;
    }

    private static org.jreleaser.model.Glob convertGlob(Glob glob) {
        org.jreleaser.model.Glob g = new org.jreleaser.model.Glob();
        g.setPattern(tr(glob.getPattern()));
        g.setPlatform(tr(glob.getPlatform()));
        if (isNotBlank(glob.getDirectory())) g.setDirectory(tr(glob.getDirectory()));
        return g;
    }

    private static void convertPackager(Packager from, org.jreleaser.model.Packager into) {
        into.setActive(tr(from.resolveActive()));
        into.setDownloadUrl(tr(from.getDownloadUrl()));
        if (from.isContinueOnErrorSet()) into.setContinueOnError(from.isContinueOnError());
        into.setExtraProperties(from.getExtraProperties());
    }

    private static org.jreleaser.model.Brew convertBrew(Brew packager) {
        org.jreleaser.model.Brew t = new org.jreleaser.model.Brew();
        convertPackager(packager, t);
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setTap(convertHomebrewTap(packager.getTap()));
        t.setFormulaName(tr(packager.getFormulaName()));
        if (packager.isMultiPlatformSet()) t.setMultiPlatform(packager.isMultiPlatform());
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        packager.getDependencies().forEach(dependency -> {
            if (isNotBlank(dependency.getValue())) {
                t.addDependency(dependency.getKey(), dependency.getValue());
            } else {
                t.addDependency(dependency.getKey());
            }
        });
        t.setLivecheck(tr(packager.getLivecheck()));
        if (packager.getCask().isSet()) {
            t.setCask(convertCask(packager.getCask()));
        }
        return t;
    }

    private static org.jreleaser.model.Brew.Cask convertCask(Brew.Cask cask) {
        org.jreleaser.model.Brew.Cask c = new org.jreleaser.model.Brew.Cask();
        c.setName(tr(cask.getName()));
        c.setDisplayName(tr(cask.getDisplayName()));
        c.setPkgName(tr(cask.getPkgName()));
        c.setAppName(tr(cask.getAppName()));
        c.setAppcast(tr(cask.getAppcast()));
        if (cask.isEnabledSet()) c.setEnabled(cask.isEnabled());
        c.setUninstall(cask.getUninstall());
        c.setZap(cask.getZap());
        return c;
    }

    private static org.jreleaser.model.Brew.HomebrewTap convertHomebrewTap(Tap tap) {
        org.jreleaser.model.Brew.HomebrewTap t = new org.jreleaser.model.Brew.HomebrewTap();
        convertTap(tap, t);
        return t;
    }

    private static org.jreleaser.model.Chocolatey convertChocolatey(Chocolatey packager) {
        org.jreleaser.model.Chocolatey t = new org.jreleaser.model.Chocolatey();
        convertPackager(packager, t);
        t.setPackageName(tr(packager.getPackageName()));
        t.setPackageVersion(tr(packager.getPackageVersion()));
        t.setUsername(tr(packager.getUsername()));
        t.setApiKey(tr(packager.getApiKey()));
        t.setTitle(tr(packager.getTitle()));
        t.setIconUrl(tr(packager.getIconUrl()));
        t.setSource(tr(packager.getSource()));
        t.setRemoteBuild(packager.isRemoteBuild());
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setBucket(convertChocolateyBucket(packager.getBucket()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Chocolatey.ChocolateyBucket convertChocolateyBucket(Bucket bucket) {
        org.jreleaser.model.Chocolatey.ChocolateyBucket b = new org.jreleaser.model.Chocolatey.ChocolateyBucket();
        convertTap(bucket, b);
        return b;
    }

    private static org.jreleaser.model.Docker convertDocker(Docker docker) {
        org.jreleaser.model.Docker t = new org.jreleaser.model.Docker();
        convertDocker(t, docker);
        t.setSpecs(convertDockerSpecs(docker.getSpecs()));
        return t;
    }

    private static void convertDocker(org.jreleaser.model.DockerConfiguration d, DockerConfiguration docker) {
        if (d instanceof org.jreleaser.model.Docker && docker instanceof Docker) {
            org.jreleaser.model.Docker dd = (org.jreleaser.model.Docker) d;
            Docker kk = (Docker) docker;
            if (kk.isContinueOnErrorSet()) dd.setContinueOnError(kk.isContinueOnError());

            dd.setRepository(convertDockerRepository(kk.getRepository()));
            dd.setCommitAuthor(convertCommitAuthor(kk.getCommitAuthor()));
            dd.setDownloadUrl(tr(kk.getDownloadUrl()));
        }
        d.setActive(tr(docker.resolveActive()));
        d.setTemplateDirectory(tr(docker.getTemplateDirectory()));
        d.setSkipTemplates(tr(docker.getSkipTemplates()));
        d.setExtraProperties(docker.getExtraProperties());
        d.setBaseImage(tr(docker.getBaseImage()));
        d.setImageNames(tr(docker.getImageNames()));
        d.setBuildArgs(tr(docker.getBuildArgs()));
        d.setPreCommands(tr(docker.getPreCommands()));
        d.setPostCommands(tr(docker.getPostCommands()));
        d.setLabels(docker.getLabels());
        d.setRegistries(convertRegistries(docker.getRegistries()));
        if (docker.isUseLocalArtifactSet()) d.setUseLocalArtifact(docker.isUseLocalArtifact());
    }

    private static org.jreleaser.model.Docker.DockerRepository convertDockerRepository(Docker.DockerRepository tap) {
        org.jreleaser.model.Docker.DockerRepository t = new org.jreleaser.model.Docker.DockerRepository();
        convertTap(tap, t);
        if (tap.isVersionedSubfoldersSet()) t.setVersionedSubfolders(tap.isVersionedSubfolders());
        return t;
    }

    private static Map<String, org.jreleaser.model.DockerSpec> convertDockerSpecs(Map<String, DockerSpec> specs) {
        Map<String, org.jreleaser.model.DockerSpec> ds = new LinkedHashMap<>();
        for (Map.Entry<String, DockerSpec> e : specs.entrySet()) {
            e.getValue().setName(tr(e.getKey()));
            ds.put(e.getValue().getName(), convertDockerSpec(e.getValue()));
        }
        return ds;
    }

    private static org.jreleaser.model.DockerSpec convertDockerSpec(DockerSpec spec) {
        org.jreleaser.model.DockerSpec d = new org.jreleaser.model.DockerSpec();
        convertDocker(d, spec);
        d.setMatchers(spec.getMatchers());
        return d;
    }

    private static Set<org.jreleaser.model.Registry> convertRegistries(Set<Registry> repositories) {
        Set<org.jreleaser.model.Registry> set = new LinkedHashSet<>();
        for (Registry registry : repositories) {
            set.add(convertRegistry(registry));
        }
        return set;
    }

    private static org.jreleaser.model.Registry convertRegistry(Registry registry) {
        org.jreleaser.model.Registry r = new org.jreleaser.model.Registry();
        if (isNotBlank(registry.getServerName())) r.setServerName(registry.getServerName());
        r.setServer(tr(registry.getServer()));
        r.setRepositoryName(tr(registry.getRepositoryName()));
        r.setUsername(tr(registry.getUsername()));
        r.setPassword(tr(registry.getPassword()));
        return r;
    }

    private static org.jreleaser.model.Jbang convertJbang(Jbang packager) {
        org.jreleaser.model.Jbang t = new org.jreleaser.model.Jbang();
        convertPackager(packager, t);
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setAlias(tr(packager.getAlias()));
        t.setCatalog(convertJbangCatalog(packager.getCatalog()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Jbang.JbangCatalog convertJbangCatalog(Jbang.Catalog catalog) {
        org.jreleaser.model.Jbang.JbangCatalog t = new org.jreleaser.model.Jbang.JbangCatalog();
        convertTap(catalog, t);
        return t;
    }

    private static org.jreleaser.model.Macports convertMacports(Macports packager) {
        org.jreleaser.model.Macports t = new org.jreleaser.model.Macports();
        convertPackager(packager, t);
        t.setPackageName(tr(packager.getPackageName()));
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setRevision(packager.getRevision());
        t.setCategories(tr(packager.getCategories()));
        t.setMaintainers(tr(packager.getMaintainers()));
        t.setRepository(convertMacportsRepository(packager.getRepository()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Macports.MacportsRepository convertMacportsRepository(Tap tap) {
        org.jreleaser.model.Macports.MacportsRepository r = new org.jreleaser.model.Macports.MacportsRepository();
        convertTap(tap, r);
        return r;
    }

    private static org.jreleaser.model.Scoop convertScoop(Scoop packager) {
        org.jreleaser.model.Scoop t = new org.jreleaser.model.Scoop();
        convertPackager(packager, t);
        t.setPackageName(tr(packager.getPackageName()));
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setCheckverUrl(tr(packager.getCheckverUrl()));
        t.setAutoupdateUrl(tr(packager.getAutoupdateUrl()));
        t.setBucket(convertScoopBucket(packager.getBucket()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Scoop.ScoopBucket convertScoopBucket(Bucket bucket) {
        org.jreleaser.model.Scoop.ScoopBucket b = new org.jreleaser.model.Scoop.ScoopBucket();
        convertTap(bucket, b);
        return b;
    }

    private static org.jreleaser.model.Sdkman convertSdkman(Sdkman packager) {
        org.jreleaser.model.Sdkman t = new org.jreleaser.model.Sdkman();
        convertPackager(packager, t);
        t.setConsumerKey(tr(packager.getConsumerKey()));
        t.setConsumerToken(tr(packager.getConsumerToken()));
        t.setCandidate(tr(packager.getCandidate()));
        t.setCommand(tr(packager.resolveCommand()));
        t.setConnectTimeout(packager.getConnectTimeout());
        t.setReadTimeout(packager.getReadTimeout());
        return t;
    }

    private static org.jreleaser.model.Snap convertSnap(Snap packager) {
        org.jreleaser.model.Snap t = new org.jreleaser.model.Snap();
        convertPackager(packager, t);
        t.setPackageName(tr(packager.getPackageName()));
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        if (isNotBlank(packager.getBase())) t.setBase(packager.getBase());
        if (isNotBlank(packager.getGrade())) t.setGrade(packager.getGrade());
        if (isNotBlank(packager.getConfinement())) t.setConfinement(packager.getConfinement());
        if (null != packager.getExportedLogin()) t.setExportedLogin(packager.getExportedLogin().getAbsolutePath());
        t.setRemoteBuild(packager.isRemoteBuild());
        t.setLocalPlugs(packager.getLocalPlugs());
        t.setLocalSlots(packager.getLocalSlots());
        t.setPlugs(convertPlugs(packager.getPlugs()));
        t.setSlots(convertSlots(packager.getSlots()));
        t.setArchitectures(convertArchitectures(packager.getArchitectures()));
        t.setSnap(convertSnapTap(packager.getSnap()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Snap.SnapTap convertSnapTap(Tap tap) {
        org.jreleaser.model.Snap.SnapTap t = new org.jreleaser.model.Snap.SnapTap();
        convertTap(tap, t);
        return t;
    }

    private static List<org.jreleaser.model.Snap.Plug> convertPlugs(List<Snap.Plug> plugs) {
        List<org.jreleaser.model.Snap.Plug> ps = new ArrayList<>();
        for (Snap.Plug plug : plugs) {
            ps.add(convertPlug(plug));
        }
        return ps;
    }

    private static org.jreleaser.model.Snap.Plug convertPlug(Snap.Plug plug) {
        org.jreleaser.model.Snap.Plug p = new org.jreleaser.model.Snap.Plug();
        p.setName(tr(plug.getName()));
        p.setAttributes(plug.getAttributes());
        p.setReads(tr(plug.getReads()));
        p.setWrites(tr(plug.getWrites()));
        return p;
    }

    private static List<org.jreleaser.model.Snap.Slot> convertSlots(List<Snap.Slot> slots) {
        List<org.jreleaser.model.Snap.Slot> ss = new ArrayList<>();
        for (Snap.Slot slot : slots) {
            ss.add(convertSlot(slot));
        }
        return ss;
    }

    private static org.jreleaser.model.Snap.Slot convertSlot(Snap.Slot slot) {
        org.jreleaser.model.Snap.Slot s = new org.jreleaser.model.Snap.Slot();
        s.setName(tr(slot.getName()));
        s.setAttributes(slot.getAttributes());
        s.setReads(tr(slot.getReads()));
        s.setWrites(tr(slot.getWrites()));
        return s;
    }

    private static List<org.jreleaser.model.Snap.Architecture> convertArchitectures(List<Snap.Architecture> architectures) {
        List<org.jreleaser.model.Snap.Architecture> as = new ArrayList<>();
        for (Snap.Architecture architecture : architectures) {
            as.add(convertArchitecture(architecture));
        }
        return as;
    }

    private static org.jreleaser.model.Snap.Architecture convertArchitecture(Snap.Architecture architecture) {
        org.jreleaser.model.Snap.Architecture a = new org.jreleaser.model.Snap.Architecture();
        a.setBuildOn(architecture.getBuildOn());
        a.setRunOn(architecture.getRunOn());
        if (architecture.isIgnoreErrorSet()) a.setIgnoreError(architecture.isIgnoreError());
        return a;
    }

    private static org.jreleaser.model.Gofish convertGofish(Gofish packager) {
        org.jreleaser.model.Gofish t = new org.jreleaser.model.Gofish();
        convertPackager(packager, t);
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setRepository(convertGofishRepository(packager.getRepository()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Gofish.GofishRepository convertGofishRepository(Tap tap) {
        org.jreleaser.model.Gofish.GofishRepository r = new org.jreleaser.model.Gofish.GofishRepository();
        convertTap(tap, r);
        return r;
    }

    private static org.jreleaser.model.Spec convertSpec(Spec packager) {
        org.jreleaser.model.Spec t = new org.jreleaser.model.Spec();
        convertPackager(packager, t);
        t.setPackageName(tr(packager.getPackageName()));
        t.setTemplateDirectory(tr(packager.getTemplateDirectory()));
        t.setSkipTemplates(tr(packager.getSkipTemplates()));
        t.setRelease(tr(packager.getRelease()));
        t.setRequires(tr(packager.getRequires()));
        t.setRepository(convertSpecRepository(packager.getRepository()));
        t.setCommitAuthor(convertCommitAuthor(packager.getCommitAuthor()));
        return t;
    }

    private static org.jreleaser.model.Spec.SpecRepository convertSpecRepository(Tap tap) {
        org.jreleaser.model.Spec.SpecRepository r = new org.jreleaser.model.Spec.SpecRepository();
        convertTap(tap, r);
        return r;
    }
}
