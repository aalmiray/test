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
package org.jreleaser.sdk.tool;

import org.jreleaser.model.JReleaserContext;
import org.jreleaser.util.command.Command;
import org.jreleaser.util.command.CommandException;
import org.jreleaser.util.command.CommandExecutor;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Andres Almiray
 * @since 1.0.0
 */
public class Upx extends AbstractTool {
    public Upx(JReleaserContext context, String version) {
        super(context, "upx", version);
    }

    public void compress(Path parent, List<String> args) throws CommandException {
        Command command = tool.asCommand().args(args);
        executeCommand(() -> new CommandExecutor(context.getLogger())
            .executeCommand(parent, command));
    }
}
