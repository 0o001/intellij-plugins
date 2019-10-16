// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model

import com.intellij.javascript.nodejs.library.NodeModulesDirectoryManager
import com.intellij.lang.javascript.modules.NodeModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.vuejs.model.source.VueSourcePlugin
import org.jetbrains.vuejs.model.webtypes.registry.VueWebTypesRegistry

class VuePluginImpl(private val project: Project, private val packageJson: VirtualFile) :
  VueDelegatedEntitiesContainer<VuePlugin>(), VuePlugin {

  override val moduleName: String? = NodeModuleUtil.inferNodeModulePackageName(packageJson)
  override val source: PsiElement? = null
  override val parents get() = VueGlobalImpl.getParents(this)

  override val delegate
    get() = packageJsonPsi?.let { psiFile ->
      CachedValuesManager.getCachedValue(psiFile) {
        buildPlugin()
      }
    } ?: EMPTY_PLUGIN

  private val packageJsonPsi: PsiFile? get() = PsiManager.getInstance(project).findFile(packageJson)

  private fun buildPlugin(): Result<VuePlugin>? {
    return VueWebTypesRegistry.createWebTypesPlugin(project, packageJson, this)
           ?: VueSourcePlugin.create(project, packageJson)?.let {
             Result.create(it as VuePlugin, packageJson,
                           NodeModulesDirectoryManager.getInstance(project).nodeModulesDirChangeTracker,
                           PsiModificationTracker.MODIFICATION_COUNT,
                           VueWebTypesRegistry.MODIFICATION_TRACKER)
           }
           ?: Result.create(null as VuePlugin?, packageJson,
                            NodeModulesDirectoryManager.getInstance(project).nodeModulesDirChangeTracker,
                            PsiModificationTracker.MODIFICATION_COUNT,
                            VueWebTypesRegistry.MODIFICATION_TRACKER)
  }

  override fun toString(): String {
    return "VuePlugin [$moduleName]"
  }

  override fun equals(other: Any?) =
    (other as? VuePluginImpl)?.let {
      it.project == project && it.packageJson == packageJson
    } ?: false

  override fun hashCode() = (project.hashCode()) * 31 + packageJson.hashCode()

  companion object {
    private val EMPTY_PLUGIN = object : VuePlugin {
      override val moduleName: String? = null
      override val components: Map<String, VueComponent> = emptyMap()
      override val directives: Map<String, VueDirective> = emptyMap()
      override val filters: Map<String, VueFilter> = emptyMap()
      override val mixins: List<VueMixin> = emptyList()
      override val source: PsiElement? = null
      override val parents get() = VueGlobalImpl.getParents(this)
    }
  }

}
