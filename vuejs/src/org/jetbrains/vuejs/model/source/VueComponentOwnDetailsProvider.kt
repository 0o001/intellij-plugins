// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model.source

import com.intellij.lang.javascript.JSStubElementTypes
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.vuejs.codeInsight.attributes.VueAttributeDescriptor
import org.jetbrains.vuejs.codeInsight.getStringLiteralsFromInitializerArray

class VueComponentOwnDetailsProvider {
  companion object {
    private val FUNCTION_FILTER = { element: PsiElement ->
      element is JSFunctionProperty || element is JSProperty && element.value is JSFunction
    }
    private val DETAILS_COMPONENTS = listOf(CompMember.Props, CompMember.Computed, CompMember.Methods, CompMember.Data)

    fun getDetails(descriptor: JSObjectLiteralExpression,
                   filter: (String, PsiElement) -> Boolean,
                   onlyPublic: Boolean,
                   onlyFirst: Boolean): List<VueAttributeDescriptor> {
      val detailsList = DETAILS_COMPONENTS.filter { !onlyPublic || it.isPublic }
      val result = mutableListOf<VueAttributeDescriptor>()
      // since Kotlin "streams" seems to be not lazy
      for (compMember in detailsList) {
        val details = compMember.readMembers(descriptor, filter)
        if (onlyFirst && details.isNotEmpty()) return details
        result.addAll((details))
      }
      return result
    }
  }

  private enum class CompMember(val propertyName: String,
                                val isPublic: Boolean,
                                val isFunctions: Boolean,
                                private val canBeArray: Boolean) {
    Props("props", true, false, true),
    Computed("computed", false, true, false),
    Methods("methods", false, true, false),
    Components("components", false, false, false),
    Data("data", false, false, false) {
      override fun getObjectLiteralFromResolved(resolved: PsiElement): JSObjectLiteralExpression? = findReturnedObjectLiteral(resolved)

      override fun getObjectLiteral(property: JSProperty): JSObjectLiteralExpression? {
        val function = property.tryGetFunctionInitializer() ?: return null
        return findReturnedObjectLiteral(function)
      }
    };

    companion object {
      private fun findReturnedObjectLiteral(resolved: PsiElement): JSObjectLiteralExpression? {
        if (resolved !is JSFunction) return null
        return JSStubBasedPsiTreeUtil.findDescendants<JSObjectLiteralExpression>(
          resolved, TokenSet.create(
          JSStubElementTypes.OBJECT_LITERAL_EXPRESSION))
          .find {
            it.context == resolved ||
            it.context is JSParenthesizedExpression && it.context?.context == resolved ||
            it.context is JSReturnStatement
          }
      }
    }

    protected open fun getObjectLiteral(property: JSProperty): JSObjectLiteralExpression? = null
    protected open fun getObjectLiteralFromResolved(resolved: PsiElement): JSObjectLiteralExpression? = null

    fun readMembers(descriptor: JSObjectLiteralExpression,
                    filter: (String, PsiElement) -> Boolean): List<VueAttributeDescriptor> {
      val detailsFilter = if (isFunctions) { name, element -> FUNCTION_FILTER(element) && filter(name, element) } else filter
      val property = descriptor.findProperty(propertyName) ?: return emptyList()

      var propsObject = property.objectLiteralExpressionInitializer ?: getObjectLiteral(property)
      if (propsObject == null && property.initializerReference != null) {
        val resolved = JSStubBasedPsiTreeUtil.resolveLocally(property.initializerReference!!, property)
        if (resolved != null) {
          propsObject = JSStubBasedPsiTreeUtil.findDescendants(resolved, JSStubElementTypes.OBJECT_LITERAL_EXPRESSION)
                          .find { it.context == resolved } ?: getObjectLiteralFromResolved(resolved)
          if (propsObject == null && canBeArray) {
            return readPropsFromArray(resolved, detailsFilter)
          }
        }
      }
      if (propsObject != null) {
        return filteredObjectProperties(propsObject, detailsFilter)
      }
      return if (canBeArray) readPropsFromArray(property, detailsFilter) else return emptyList()
    }

    private fun filteredObjectProperties(propsObject: JSObjectLiteralExpression, filter: (String, PsiElement) -> Boolean) =
      propsObject.properties.filter {
        val propName = it.name
        propName != null && filter(propName, it)
      }.map { VueAttributeDescriptor(it.name!!, it) }

    private fun readPropsFromArray(holder: PsiElement, filter: (String, PsiElement) -> Boolean): List<VueAttributeDescriptor> =
      getStringLiteralsFromInitializerArray(holder, filter).map { VueAttributeDescriptor(it.stringValue ?: "", it) }
  }
}
