/*
 * Copyright 2013 The authors
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

package com.intellij.struts2.dom.struts;

import com.intellij.struts2.Struts2ProjectDescriptorBuilder;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link com.intellij.struts2.tiles.TilesResultContributor} /
 * {@link com.intellij.struts2.tiles.Struts2TilesModelProvider} with default configuration (only /WEB-INF/tiles.xml).
 *
 * @author Yann C&eacute;bron
 */
public class StrutsResultTilesResolvingDefaultTest extends StrutsLightHighlightingTestCase {

  private final LightProjectDescriptor TILES = new Struts2ProjectDescriptorBuilder()
    .withStrutsLibrary()
    .withStrutsFacet()
    .withWebModuleType(getTestDataPath())
    .withLibrary("tiles", "struts2-tiles-plugin-" + STRUTS2_VERSION + ".jar");

  @Override
  @NotNull
  protected String getTestDataLocation() {
    return "strutsXml/resultTilesDefault";
  }

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return TILES;
  }

  @Override
  protected void performSetUp() {
    myFixture.copyDirectoryToProject("WEB-INF", "WEB-INF");
  }

  public void testHighlighting() {
    performHighlightingTest("struts-tiles.xml");
  }

  public void testCompletion() {
    createStrutsFileSet("struts-tiles-completion.xml");
    myFixture.testCompletionVariants("struts-tiles-completion.xml",
                                     "definition1",
                                     "definition2");
  }
}