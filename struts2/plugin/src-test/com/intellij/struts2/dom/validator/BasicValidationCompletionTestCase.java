/*
 * Copyright 2011 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts2.dom.validator;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yann C&eacute;bron
 */
abstract class BasicValidationCompletionTestCase extends BasicValidationHighlightingTestCase<JavaModuleFixtureBuilder> {

  @NotNull
  @Override
  protected final String getTestDataLocation() {
    return "validationXmlCompletion/" + getTestDataDirectory();
  }

  protected abstract String getTestDataDirectory();

  @Override
  protected void customizeSetup(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    installSrcHack();
  }

}