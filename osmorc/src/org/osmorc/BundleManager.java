/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.osmorc;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.libraries.Library;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.manifest.BundleManifest;

import java.util.Collection;
import java.util.Set;

/**
 * The bundle manager allows for queries over the bundles which are known in the current project. It allows queries over
 * IDEA's project structures (like Module and Library) and internally maps them to OSGi structures (bundles). It exists per project.
 *
 * @author Robert F. Beeger (robert@beeger.net)
 * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
 */
public interface BundleManager {
  /**
   * Returns the manifest for the given symbolic name or null if no bundle with that symbolic name is known.
   * If multiple bundles with that name are known, it will return the manifest of an arbitrarily chosen bundle.
   */
  @Nullable
  BundleManifest getManifestBySymbolicName(@NotNull String bundleSymbolicName);

  /**
   * Returns the manifest for a given object or null if no manifest for the given object is known.
   */
  @Nullable
  BundleManifest getManifestByObject(@NotNull Object object);

  /**
   * Returns manifest of a bundle specified by a given requirement specification, or null if not found.
   */
  @Nullable
  BundleManifest getManifestByBundleSpec(@NotNull String bundleSpec);

  /**
   * Adds the given module and it's dependencies to the list of known bundles.
   * If it exists, it's entries are updated.
   */
  void reindex(@NotNull Module module);

  /**
   * Adds the given libraries to the bundle index.
   */
  void reindex(@NotNull Collection<Library> libraries);

  /**
   * Does a complete project reindex.
   * Cleans all data from the bundle manager and replaces it with the data from the given project instance.
   */
  void reindexAll();

  /**
   * Resolves all dependencies of the given module, by analyzing the package import, require-bundle, bundle-classpath and fragment hosts
   * headers. Returns a list of {@link Module} and {@link Library} objects.
   */
  @NotNull
  Set<Object> resolveDependenciesOf(@NotNull Module module);

  /**
   * Checks if the given dependency is re-exported by the given module. This is only the case, if the module has a Require-Bundle-Header
   * that requires the given dependency and the visibility directive of this requirement is set to "reexport".
   */
  boolean isReExported(@NotNull Object dependency, @NotNull Module module);

  /**
   * Checks if the given host module/library is a fragment host of the given fragment module/library
   */
  boolean isFragmentHost(@NotNull Object host, @NotNull Object fragment);

  /**
   * Returns true if the given package is provided by one of the registered bundles.
   */
  boolean isProvided(@NotNull String packageSpec);
}
