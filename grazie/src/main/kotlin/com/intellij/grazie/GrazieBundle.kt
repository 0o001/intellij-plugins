// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie

import com.intellij.AbstractBundle
import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object GrazieBundle {
  const val DEFAULT_BUNDLE_NAME = "messages.GrazieBundle"
  const val PLUGIN_BUNDLE_NAME = "messages.GraziePluginBundle"

  private val defaultBundle by lazy { ResourceBundle.getBundle(DEFAULT_BUNDLE_NAME) }
  private val pluginBundle by lazy { ResourceBundle.getBundle(PLUGIN_BUNDLE_NAME) }

  fun message(@PropertyKey(resourceBundle = DEFAULT_BUNDLE_NAME) key: String, vararg params: String): String {
    return AbstractBundle.message(if (!GraziePlugin.isBundled && pluginBundle.containsKey(key)) pluginBundle else defaultBundle, key, *params)
  }
}
