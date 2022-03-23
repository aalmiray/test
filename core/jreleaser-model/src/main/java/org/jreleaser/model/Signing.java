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
package org.jreleaser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jreleaser.util.Env;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jreleaser.util.Constants.HIDE;
import static org.jreleaser.util.Constants.UNSET;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public class Signing implements Domain, Activatable {
    public static final String KEY_SKIP_SIGNING = "skipSigning";
    public static final String COSIGN_PASSWORD = "COSIGN_PASSWORD";
    public static final String COSIGN_PRIVATE_KEY = "COSIGN_PRIVATE_KEY";
    public static final String COSIGN_PUBLIC_KEY = "COSIGN_PUBLIC_KEY";
    public static final String GPG_PASSPHRASE = "GPG_PASSPHRASE";
    public static final String GPG_PUBLIC_KEY = "GPG_PUBLIC_KEY";
    public static final String GPG_SECRET_KEY = "GPG_SECRET_KEY";
    public static final String GPG_EXECUTABLE = "GPG_EXECUTABLE";
    public static final String GPG_KEYNAME = "GPG_KEYNAME";
    public static final String GPG_HOMEDIR = "GPG_HOMEDIR";
    public static final String GPG_PUBLIC_KEYRING = "GPG_PUBLIC_KEYRING";

    private final List<String> args = new ArrayList<>();
    private final Command command = new Command();
    private final Cosign cosign = new Cosign();

    private Active active;
    @JsonIgnore
    private boolean enabled;
    private Boolean armored;
    private String publicKey;
    private String secretKey;
    private String passphrase;
    private Mode mode;
    private Boolean artifacts;
    private Boolean files;
    private Boolean checksums;

    void setAll(Signing signing) {
        this.active = signing.active;
        this.enabled = signing.enabled;
        this.armored = signing.armored;
        this.publicKey = signing.publicKey;
        this.secretKey = signing.secretKey;
        this.passphrase = signing.passphrase;
        this.mode = signing.mode;
        this.artifacts = signing.artifacts;
        this.files = signing.files;
        this.checksums = signing.checksums;
        setCommand(signing.command);
        setCosign(signing.cosign);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        active = Active.NEVER;
        enabled = false;
    }

    public boolean resolveEnabled(Project project) {
        if (null == active) {
            active = Active.NEVER;
        }
        enabled = active.check(project);
        return enabled;
    }

    public Mode resolveMode() {
        if (null == mode) {
            mode = Mode.MEMORY;
        }
        return mode;
    }

    @Override
    public Active getActive() {
        return active;
    }

    @Override
    public void setActive(Active active) {
        this.active = active;
    }

    @Override
    public void setActive(String str) {
        this.active = Active.of(str);
    }

    @Override
    public boolean isActiveSet() {
        return active != null;
    }

    public String getResolvedPublicKey() {
        return Env.resolve(GPG_PUBLIC_KEY, publicKey);
    }

    public String getResolvedSecretKey() {
        return Env.resolve(GPG_SECRET_KEY, secretKey);
    }

    public String getResolvedPassphrase() {
        return Env.resolve(GPG_PASSPHRASE, passphrase);
    }

    public String getResolvedCosignPassword() {
        return Env.resolve(COSIGN_PASSWORD, passphrase);
    }

    public Boolean isArmored() {
        return armored != null && armored;
    }

    public void setArmored(Boolean armored) {
        this.armored = armored;
    }

    public boolean isArmoredSet() {
        return armored != null;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setMode(String str) {
        this.mode = Mode.of(str);
    }

    public boolean isArtifactsSet() {
        return artifacts != null;
    }

    public Boolean isArtifacts() {
        return artifacts == null || artifacts;
    }

    public void setArtifacts(Boolean artifacts) {
        this.artifacts = artifacts;
    }

    public Boolean isFiles() {
        return files == null || files;
    }

    public boolean isFilesSet() {
        return files != null;
    }

    public void setFiles(Boolean files) {
        this.files = files;
    }

    public boolean isChecksumsSet() {
        return checksums != null;
    }

    public Boolean isChecksums() {
        return checksums == null || checksums;
    }

    public void setChecksums(Boolean checksums) {
        this.checksums = checksums;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command.setAll(command);
    }

    public Cosign getCosign() {
        return cosign;
    }

    public void setCosign(Cosign cosign) {
        this.cosign.setAll(cosign);
    }

    @Override
    public Map<String, Object> asMap(boolean full) {
        if (!full && !isEnabled()) return Collections.emptyMap();

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("enabled", isEnabled());
        props.put("active", active);
        props.put("armored", isArmored());
        props.put("mode", mode);
        props.put("artifacts", isArtifacts());
        props.put("files", isFiles());
        props.put("checksums", isChecksums());
        props.put("passphrase", isNotBlank(passphrase) ? HIDE : UNSET);

        if (mode == Mode.COMMAND) {
            props.put("command", command.asMap(full));
        } else if (mode == Mode.COSIGN) {
            props.put("cosign", cosign.asMap(full));
        } else {
            props.put("publicKey", isNotBlank(publicKey) ? HIDE : UNSET);
            props.put("secretKey", isNotBlank(secretKey) ? HIDE : UNSET);
        }

        return props;
    }

    public String getSignatureExtension() {
        String extension = ".sig";
        if (mode != Signing.Mode.COSIGN) {
            extension = isArmored() ? ".asc" : ".sig";
        }

        return extension;
    }

    public enum Mode {
        MEMORY,
        FILE,
        COMMAND,
        COSIGN;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        public static Mode of(String str) {
            if (isBlank(str)) return null;
            return Mode.valueOf(str.toUpperCase().trim());
        }
    }

    public static class Command implements Domain {
        private final List<String> args = new ArrayList<>();

        private String executable;
        private String keyName;
        private String homeDir;
        private String publicKeyring;
        private Boolean defaultKeyring;

        void setAll(Command command) {
            this.executable = command.executable;
            this.keyName = command.keyName;
            this.homeDir = command.homeDir;
            this.publicKeyring = command.publicKeyring;
            this.defaultKeyring = command.defaultKeyring;
            setArgs(command.args);
        }

        public String getExecutable() {
            return executable;
        }

        public void setExecutable(String executable) {
            this.executable = executable;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }

        public String getHomeDir() {
            return homeDir;
        }

        public void setHomeDir(String homeDir) {
            this.homeDir = homeDir;
        }

        public String getPublicKeyring() {
            return publicKeyring;
        }

        public void setPublicKeyring(String publicKeyring) {
            this.publicKeyring = publicKeyring;
        }

        public boolean isDefaultKeyringSet() {
            return defaultKeyring != null;
        }

        public Boolean isDefaultKeyring() {
            return defaultKeyring == null || defaultKeyring;
        }

        public void setDefaultKeyring(Boolean defaultKeyring) {
            this.defaultKeyring = defaultKeyring;
        }

        public List<String> getArgs() {
            return args;
        }

        public void setArgs(List<String> args) {
            this.args.clear();
            this.args.addAll(args);
        }

        public void addArgs(List<String> args) {
            this.args.addAll(args);
        }

        public void addArg(String arg) {
            if (isNotBlank(arg)) {
                this.args.add(arg.trim());
            }
        }

        public void removeArg(String arg) {
            if (isNotBlank(arg)) {
                this.args.remove(arg.trim());
            }
        }

        @Override
        public Map<String, Object> asMap(boolean full) {
            Map<String, Object> props = new LinkedHashMap<>();

            props.put("executable", executable);
            props.put("keyName", keyName);
            props.put("homeDir", homeDir);
            props.put("publicKeyring", publicKeyring);
            props.put("defaultKeyring", isDefaultKeyring());
            props.put("args", args);

            return props;
        }
    }

    public static class Cosign implements Domain {
        private String version;
        private String privateKeyFile;
        private String publicKeyFile;

        void setAll(Cosign cosign) {
            this.version = cosign.version;
            this.privateKeyFile = cosign.privateKeyFile;
            this.publicKeyFile = cosign.publicKeyFile;
        }

        public String getResolvedPrivateKeyFile() {
            return Env.resolve(COSIGN_PRIVATE_KEY, privateKeyFile);
        }

        public String getResolvedPublicKeyFile() {
            return Env.resolve(COSIGN_PUBLIC_KEY, publicKeyFile);
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getPrivateKeyFile() {
            return privateKeyFile;
        }

        public void setPrivateKeyFile(String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
        }

        public String getPublicKeyFile() {
            return publicKeyFile;
        }

        public void setPublicKeyFile(String publicKeyFile) {
            this.publicKeyFile = publicKeyFile;
        }

        @Override
        public Map<String, Object> asMap(boolean full) {
            Map<String, Object> props = new LinkedHashMap<>();

            props.put("version", version);
            props.put("privateKeyFile", null != privateKeyFile ? HIDE : UNSET);
            props.put("publicKeyFile", publicKeyFile);

            return props;
        }

        public Path getResolvedPrivateKeyFilePath(JReleaserContext context) {
            String privateKey = getResolvedPrivateKeyFile();

            if (isNotBlank(privateKey)) {
                return context.getBasedir().resolve(privateKey);
            }

            return resolveJReleaserHomeDir().resolve("cosign.key");
        }

        public Path getResolvedPublicKeyFilePath(JReleaserContext context) {
            String publicKey = getResolvedPublicKeyFile();

            if (isNotBlank(publicKey)) {
                return context.getBasedir().resolve(publicKey);
            }

            return resolveJReleaserHomeDir().resolve("cosign.pub");
        }

        private Path resolveJReleaserHomeDir() {
            String home = System.getenv("JRELEASER_USER_HOME");
            if (isBlank(home)) {
                home = System.getProperty("user.home") + File.separator + ".jreleaser";
            }

            return Paths.get(home);
        }
    }
}
