/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
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
package org.osmorc;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.osgi.jps.model.ManifestGenerationMode;
import org.osmorc.facet.OsmorcFacet;
import org.osmorc.facet.OsmorcFacetType;

import java.io.File;

public abstract class LightOsgiFixtureTestCase extends LightCodeInsightFixtureTestCase {
  private static final DefaultLightProjectDescriptor OSGi_DESCRIPTOR = new DefaultLightProjectDescriptor() {
    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
      super.configureModule(module, model, contentEntry);

      String libPath = PluginPathManager.getPluginHomePath("osmorc") + "/lib";
      PsiTestUtil.addLibrary(module, model, "osgi.core", libPath, "org.apache.felix.framework-4.2.1.jar");
      PsiTestUtil.addLibrary(module, model, "plexus", libPath, "plexus-utils-3.0.10.jar");

      String annotationsPath = PathManager.getJarPathForClass(NotNull.class);
      assertNotNull(annotationsPath);
      File annotations = new File(annotationsPath);
      PsiTestUtil.addLibrary(module, model, "annotations", annotations.getParent(), annotations.getName());

      FacetManager.getInstance(module).addFacet(OsmorcFacetType.getInstance(), "OSGi", null);
    }
  };

  protected OsmorcFacet myFacet;

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return OSGi_DESCRIPTOR;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFacet = OsmorcFacet.getInstance(myModule);
    assertNotNull(myFacet);

    myFacet.getConfiguration().setUseProjectDefaultManifestFileLocation(false);
    myFacet.getConfiguration().setManifestLocation("META-INF/MANIFEST.MF");
    myFacet.getConfiguration().setManifestGenerationMode(ManifestGenerationMode.Manually);
  }
}
