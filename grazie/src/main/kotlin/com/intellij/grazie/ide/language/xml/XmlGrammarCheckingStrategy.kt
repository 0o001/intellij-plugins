// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie.ide.language.xml

import com.intellij.grazie.grammar.strategy.GrammarCheckingStrategy
import com.intellij.grazie.utils.isAtEnd
import com.intellij.grazie.utils.isAtStart
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType

class XmlGrammarCheckingStrategy : GrammarCheckingStrategy {
  override fun isMyContextRoot(element: PsiElement) = element is XmlText || (element is XmlToken && element.tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)

  override fun isTypoAccepted(root: PsiElement, typoRange: IntRange, ruleRange: IntRange) = !typoRange.isAtStart(root) && !typoRange.isAtEnd(root)
}
