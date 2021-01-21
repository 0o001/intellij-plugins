// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.context

import com.intellij.javascript.web.context.WebFrameworkNodeModulesContext
import org.jetbrains.vuejs.index.VUE_MODULE

private class VueNodeModulesContext : WebFrameworkNodeModulesContext(VUE_MODULE)