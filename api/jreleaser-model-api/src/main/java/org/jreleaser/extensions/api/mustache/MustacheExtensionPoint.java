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
package org.jreleaser.extensions.api.mustache;

import org.jreleaser.extensions.api.ExtensionPoint;
import org.jreleaser.mustache.TemplateContext;

/**
 * Enables customization of the Mustache context used when evaluating named templates.
 *
 * @author Andres Almiray
 * @since 1.3.0
 */
public interface MustacheExtensionPoint extends ExtensionPoint {
    /**
     * Enhances the given context with additional properties.
     * <p>
     * <strong>WARNING: </strong> be careful when defining key names
     * as you may override existing ones.
     *
     * @param context the evaluation context.
     */
    void apply(TemplateContext context);
}
