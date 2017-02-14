package org.jetbrains.vuejs.codeInsight

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.meta.PsiPresentableMetaData
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ArrayUtil
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlAttributeDescriptorsProvider
import com.intellij.xml.impl.BasicXmlAttributeDescriptor
import icons.VuejsIcons
import org.jetbrains.vuejs.VueLanguage
import javax.swing.Icon

class VueAttributesProvider : XmlAttributeDescriptorsProvider{
  companion object {
    val DEFAULT_BINDABLE = arrayOf("key", "is")
    val DEFAULT = arrayOf("v-text", "v-html", "v-show", "v-if", "v-else", "v-else-if", "v-for",
                          "v-on", "v-bind", "v-model", "v-pre", "v-cloak","v-once",
                          "slot", "ref").
                  plus(DEFAULT_BINDABLE.map { "v-bind:" + it }).
                  plus(DEFAULT_BINDABLE.map { ":" + it })

    fun vueAttributeDescriptor(attributeName: String?): VueAttributeDescriptor? {
      if (DEFAULT.contains(attributeName!!)) return VueAttributeDescriptor(attributeName)
      if (attributeName.startsWith(":") || attributeName.startsWith("v-bind:")) return VueAttributeDescriptor(attributeName)
      if (attributeName.startsWith("@") || attributeName.startsWith("v-on:")) return VueAttributeDescriptor(attributeName)
      return null
    }
  }
  override fun getAttributeDescriptors(context: XmlTag?): Array<out XmlAttributeDescriptor> {
    val default = DEFAULT.map { VueAttributeDescriptor(it) }.toTypedArray()
    if ("style" == context?.name && context?.containingFile?.language == VueLanguage.INSTANCE) {
      return default.plus(VueAttributeDescriptor("scoped"))
    }
    return default
  }

  override fun getAttributeDescriptor(attributeName: String?, context: XmlTag?): XmlAttributeDescriptor? {
    if ("style" == context?.name && context?.containingFile?.language == VueLanguage.INSTANCE) {
      return VueAttributeDescriptor("scoped")
    }
    return vueAttributeDescriptor(attributeName)
  }
}

class VueAttributeDescriptor(private val name:String,
                             private val element:PsiNamedElement? = null) : BasicXmlAttributeDescriptor(), PsiPresentableMetaData {
  override fun getName() = name
  override fun getDeclaration() = element
  override fun init(element: PsiElement?) {}
  override fun isRequired() = element is JSProperty && findProperty(element.objectLiteralExpressionInitializer, "required") != null
  override fun isFixed() = false
  override fun hasIdType() = false
  override fun getDependences(): Array<out Any> = ArrayUtil.EMPTY_OBJECT_ARRAY
  override fun getEnumeratedValueDeclaration(xmlElement: XmlElement?, value: String?): PsiElement? {
    return if ("v-else" == name || "scoped" == name) xmlElement else super.getEnumeratedValueDeclaration(xmlElement, value)
  }

  override fun hasIdRefType() = false
  override fun getDefaultValue() = null
  override fun isEnumerated() = false
  override fun getEnumeratedValues(): Array<out String> = ArrayUtil.EMPTY_STRING_ARRAY
  override fun getTypeName() = null
  override fun getIcon(): Icon = VuejsIcons.Vue
}

fun findProperty(obj: JSObjectLiteralExpression?, name:String) = obj?.properties?.find { it.name == name }