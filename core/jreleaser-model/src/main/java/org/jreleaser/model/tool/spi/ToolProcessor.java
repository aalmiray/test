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
package org.jreleaser.model.tool.spi;

import org.jreleaser.model.Distribution;
import org.jreleaser.model.Tool;

import java.util.Map;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public interface ToolProcessor<T extends Tool> {
    T getTool();

    void setTool(T tool);

    String getToolName();

    boolean supportsDistribution(Distribution distribution);

    void prepareDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException;

    void packageDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException;

    void publishDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException;
}
