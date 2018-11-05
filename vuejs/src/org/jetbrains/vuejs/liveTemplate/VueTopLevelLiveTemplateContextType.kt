// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.liveTemplate

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import org.jetbrains.vuejs.VueFileType

/**
 * @author Irina.Chernushina on 10/26/2017.
 */
private const val CONTEXT_TYPE = "VUE_TOP_LEVEL"

class VueTopLevelLiveTemplateContextType : TemplateContextType(CONTEXT_TYPE, "Vue top-level", VueBaseLiveTemplateContextType::class.java) {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    if (VueFileType.INSTANCE == file.fileType) {
      val element = file.findElementAt(offset) ?: return true
      return PsiTreeUtil.getParentOfType(element, XmlTag::class.java) == null
    }
    return false
  }
}