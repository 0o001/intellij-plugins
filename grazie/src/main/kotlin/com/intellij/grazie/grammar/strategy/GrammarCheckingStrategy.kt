// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie.grammar.strategy

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.grazie.grammar.Typo
import com.intellij.grazie.grammar.strategy.GrammarCheckingStrategy.ElementBehavior.*
import com.intellij.grazie.grammar.strategy.impl.*
import com.intellij.grazie.utils.LinkedSet
import com.intellij.psi.PsiElement

/**
 * Strategy extracting elements for grammar checking used by Grazie plugin
 *
 * You need to implement [isMyContextRoot] and add com.intellij.grazie.grammar.strategy extension in your .xml config
 */
interface GrammarCheckingStrategy {

  /**
   * Possible PsiElement behavior during grammar check
   *
   * [TEXT] - element contains text
   * [STEALTH] - element's text is ignored
   * [ABSORB] - element's text is ignored, as well as the typos that contain this element
   *
   * [ABSORB] and [STEALTH] behavior also prevents visiting children of these elements.
   *
   * Examples:
   *  Text: This is a <bold>error</bold> sample.
   *  PsiElements: ["This is a ", "<bold>", "error", "</bold>", " sample"]
   *
   *  If this text pass as is to the grammar checker, then there are no errors.
   *  The reason are tags framing word 'error', these tags are not part of the sentence and used only for formatting.
   *  So you can add [STEALTH] behavior to these tags PsiElements, which made text passed to grammar checker 'This is a error sample.'
   *  and checker will find an error - 'Use "an" instead of 'a' if the following word starts with a vowel sound'
   *
   *
   *  Text: There is raining <br> Days passing by
   *  PsiElements: ["There is raining ", "<br>", "Days passing by"]
   *
   *  There is <br> tag. Like in previous example this tag not used in the result text.
   *  But if we make tag <br> [STEALTH] there will be an error - 'There *are* raining days'
   *  By the way, 'Days passing by' is a new sentence that doesn't applies to previous one.
   *  In that case you can use [ABSORB] behavior, then this kind of errors will be filtered.
   */
  enum class ElementBehavior {
    TEXT,
    STEALTH,
    ABSORB
  }

  /**
   * Determine text block root, for example a paragraph of text.
   *
   * @param element visited element
   * @return true if context root
   */
  fun isMyContextRoot(element: PsiElement): Boolean

  /**
   * Determine PsiElement behavior @see [ElementBehavior].
   *
   * @param root root element previously selected in [isMyContextRoot]
   * @param child current checking element for which behavior is specified
   * @return [ElementBehavior] for [child] element
   */
  fun getElementBehavior(root: PsiElement, child: PsiElement) = TEXT

  /**
   * Specify ranges, which will be removed from text before checking (like STEALTH behavior).
   * You can use [indentIndexes] to hide the indentation of each line of text.
   *
   * @param root root element previously selected in [isMyContextRoot]
   * @param text extracted text from root element without [ABSORB] and [STEALTH] ones
   * in which you need to specify the ranges to remove from the grammar checking
   * @return set of ranges in the [text] to be ignored
   */
  fun getStealthyRanges(root: PsiElement, text: CharSequence) = LinkedSet<IntRange>()

  /**
   * Determine if typo is will be shown to user. The final check before add typo to [ProblemsHolder].
   *
   * @param root root element previously selected in [isMyContextRoot]
   * @param typoRange range of the typo inside [root] element
   * @param ruleRange range of elements needed for rule to find typo
   * @return true if typo should be accepted
   */
  fun isTypoAccepted(root: PsiElement, typoRange: IntRange, ruleRange: IntRange) = true

  /**
   * Get ignored typo categories for [child] element @see [Typo.Category].
   *
   * @param root root element previously selected in [isMyContextRoot]
   * @param child current checking element for which ignored categories are specified
   * @return set of the ignored categories for [child]
   */
  fun getIgnoredTypoCategories(root: PsiElement, child: PsiElement): Set<Typo.Category>? = null

  /**
   * Get ignored rules for [child] element @see [RuleGroup].
   *
   * @param root root element previously selected in [isMyContextRoot]
   * @param child current checking element for which ignored rules are specified
   * @return RuleGroup with ignored rules for [child]
   */
  fun getIgnoredRuleGroup(root: PsiElement, child: PsiElement): RuleGroup? = null

  /**
   * Get rules for char replacement in PSI elements text @see [ReplaceCharRule].
   * (In most cases you don't want to change this)
   *
   * @param root root element previously selected in [isMyContextRoot]
   * @return list of char replacement rules for whole root context
   */
  @Deprecated("Use getStealthyRanges() if you don't need some chars")
  fun getReplaceCharRules(root: PsiElement): List<ReplaceCharRule> = listOf(ReplaceNewLines)
}
