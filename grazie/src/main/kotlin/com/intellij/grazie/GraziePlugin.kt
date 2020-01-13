// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.io.File

internal object GraziePlugin {
  const val id = "tanvd.grazi"

  private val descriptor: IdeaPluginDescriptor
    get() = PluginManagerCore.getPlugin(PluginId.getId(id))!!

  val isBundled: Boolean
    get() = descriptor.isBundled

  val version: String
    get() = descriptor.version

  val classLoader: ClassLoader
    get() = descriptor.pluginClassLoader

  val libFolder: File
    get() = descriptor.path.resolve("lib")
}
