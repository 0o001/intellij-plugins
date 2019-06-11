// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.codeInsight.attributes

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.css.CSSLanguage
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.XmlPatterns.xmlAttribute
import com.intellij.psi.impl.source.html.HtmlTagImpl
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import icons.VuejsIcons
import org.jetbrains.vuejs.codeInsight.VueComponentDetailsProvider
import org.jetbrains.vuejs.codeInsight.VueComponents
import org.jetbrains.vuejs.codeInsight.completion.vuetify.VuetifyIcons
import org.jetbrains.vuejs.codeInsight.tags.VueElementDescriptor
import org.jetbrains.vuejs.index.hasVue

class VueTagCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_NAME).withParent(xmlAttribute()),
           VueEventAttrCompletionProvider())
    extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_DATA_CHARACTERS),
           VueEventAttrDataCompletionProvider())
    extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN),
           VueTagAttributeCompletionProvider())
  }
}


private class VueTagAttributeCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val VUE_SCRIPT_LANGUAGE = ContainerUtil.immutableSet("js", "ts")
  private val VUE_STYLE_LANGUAGE = vueStyleLanguages()
  private val VUE_TEMPLATE_LANGUAGE = ContainerUtil.immutableSet("html", "pug")

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val xmlTag = PsiTreeUtil.getParentOfType(parameters.position, XmlTag::class.java, false)
    val xmlAttribute = PsiTreeUtil.getParentOfType(parameters.position, XmlAttribute::class.java, false)
    if (xmlTag == null || xmlAttribute == null) return
    for (completion in listOfCompletions(xmlTag, xmlAttribute)) {
      result.addElement(LookupElementBuilder.create(completion))
    }
  }

  private fun listOfCompletions(xmlTag: XmlTag, xmlAttribute: XmlAttribute): Set<String> {
    if (xmlAttribute.name == "lang") {
      when (xmlTag.name) {
        "script" -> return VUE_SCRIPT_LANGUAGE
        "style" -> return VUE_STYLE_LANGUAGE
        "template" -> return VUE_TEMPLATE_LANGUAGE
      }
    }
    return ContainerUtil.immutableSet()
  }

  private fun vueStyleLanguages(): Set<String> {
    val result = mutableListOf<String>()
    result.add("css")
    CSSLanguage.INSTANCE.dialects.forEach {
      if (it.displayName != "JQuery-CSS") {
        result.add(it.displayName.toLowerCase())
      }
    }
    return result.toSet()
  }
}

private class VueEventAttrCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    // https://vuejs.org/v2/guide/events.html#Key-Modifiers
    private val KEY_MODIFIERS = arrayOf("enter", "tab", "delete", "esc", "space", "up", "down", "left", "right")
    // KEY_MODIFIERS are applicable only for the KEY_EVENTS
    private val KEY_EVENTS = arrayOf("keydown", "keypress", "keyup")
    // https://vuejs.org/v2/guide/events.html#Mouse-Button-Modifiers
    private val MOUSE_BUTTON_MODIFIERS = arrayOf("left", "right", "middle")
    // MOUSE_BUTTON_MODIFIERS are applicable only for the MOUSE_BUTTON_EVENTS
    private val MOUSE_BUTTON_EVENTS = arrayOf("click", "dblclick", "mousedown", "mouseup")
    // https://vuejs.org/v2/guide/events.html#System-Modifier-Keys
    private val SYSTEM_MODIFIERS = arrayOf("ctrl", "alt", "shift", "meta", "exact")
    // SYSTEM_MODIFIERS are applicable only for the KEY_EVENTS and all MOUSE_EVENTS
    private val MOUSE_EVENTS = arrayOf("click", "contextmenu", "dblclick", "mousedown", "mouseenter", "mouseleave", "mousemove", "mouseout",
                                       "mouseover", "mouseup", "show", "drag", "dragend", "dragenter", "dragleave", "dragover", "dragstart",
                                       "drop")
    // https://vuejs.org/v2/guide/events.html#Event-Modifiers
    private val EVENT_MODIFIERS = arrayOf("stop", "prevent", "capture", "self", "once", "passive", "native")
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    if (!hasVue(parameters.position.project)) return
    val attr = parameters.position.parent as? XmlAttribute ?: return
    val attrName = attr.name
    if (attrName.startsWith("v-on:") || attrName.startsWith("@")) {
      addEventCompletions(attr, result)
      return
    }

    if (attrName.startsWith("v-bind:") || attrName.startsWith(":")) {
      addBindCompletions(attr, result)
      return
    }

    val insertHandler = InsertHandler<LookupElement> { insertionContext, _ ->
      insertionContext.setLaterRunnable {
        CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(parameters.originalFile.project, parameters.editor)
      }
    }
    result.addElement(LookupElementBuilder.create("v-on:").withIcon(VuejsIcons.Vue).withInsertHandler(insertHandler))
    result.addElement(LookupElementBuilder.create("v-bind:").withIcon(VuejsIcons.Vue).withInsertHandler(insertHandler))
  }

  private fun addEventCompletions(attr: XmlAttribute, result: CompletionResultSet) {
    val prefix = result.prefixMatcher.prefix
    val lastDotIndex = prefix.lastIndexOf('.')
    if (lastDotIndex < 0) {
      val newResult = if (prefix == "v-on:") result.withPrefixMatcher("") else result
      addEventCompletions(attr.parent, newResult, if (prefix.startsWith("@")) "@" else "")
    }
    else {
      addModifierCompletions(result, prefix, lastDotIndex)
    }
  }

  private fun addEventCompletions(tag: XmlTag?, result: CompletionResultSet, prefix: String) {
    val descriptor = tag?.descriptor as? HtmlElementDescriptorImpl ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(tag) ?: return
    for (attrDescriptor in descriptor.getAttributesDescriptors(tag)) {
      val name = attrDescriptor.name
      if (name.startsWith("on")) {
        result.addElement(LookupElementBuilder
                            .create(prefix + name.substring("on".length))
                            .withInsertHandler(XmlAttributeInsertHandler.INSTANCE))
      }
    }
  }

  private fun addModifierCompletions(result: CompletionResultSet, prefix: String, lastDotIndex: Int) {
    val newResult = result.withPrefixMatcher(prefix.substring(lastDotIndex + 1))
    val usedModifiers = getUsedModifiers(prefix, lastDotIndex)

    doAddModifierCompletions(newResult, usedModifiers,
                             EVENT_MODIFIERS)

    if (isEventFromGroup(KEY_EVENTS, prefix)) {
      doAddModifierCompletions(newResult, usedModifiers,
                               KEY_MODIFIERS)
      // Do we also want to suggest the full list of https://vuejs.org/v2/guide/events.html#Automatic-Key-Modifiers?
    }

    if (isEventFromGroup(MOUSE_BUTTON_EVENTS, prefix)) {
      doAddModifierCompletions(newResult, usedModifiers,
                               MOUSE_BUTTON_MODIFIERS)
    }

    if (isEventFromGroup(KEY_EVENTS, prefix) || isEventFromGroup(
        MOUSE_EVENTS, prefix)) {
      doAddModifierCompletions(newResult, usedModifiers,
                               SYSTEM_MODIFIERS)
    }
  }

  private fun getUsedModifiers(prefix: String, lastDotIndex: Int): Collection<String> {
    val usedModifiers = SmartList<String>()
    var substring = prefix.substring(0, lastDotIndex)
    var dotIndex = substring.lastIndexOf('.')
    while (dotIndex > 0) {
      usedModifiers.add(substring.substring(dotIndex + 1))
      substring = substring.substring(0, dotIndex)
      dotIndex = substring.lastIndexOf('.')
    }
    return usedModifiers
  }

  private fun isEventFromGroup(eventGroup: Array<String>, prefix: String): Boolean {
    // in case of @click attribute prefix=="@click" but in case of v-on:click prefix=="click"
    val trimmedPrefix = StringUtil.trimStart(prefix, "@")
    return eventGroup.find { trimmedPrefix.startsWith("$it.") } != null
  }

  private fun doAddModifierCompletions(result: CompletionResultSet, usedModifiers: Collection<String>, modifiers: Array<String>) {
    modifiers.forEach {
      if (!usedModifiers.contains(it)) {
        result.addElement(LookupElementBuilder.create(it))
      }
    }
  }

  private fun addBindCompletions(attr: XmlAttribute, result: CompletionResultSet) {
    val prefix = result.prefixMatcher.prefix
    val newResult = if (prefix == "v-bind:") result.withPrefixMatcher("") else result
    val lookupItemPrefix = if (prefix.startsWith(":")) ":" else ""

    newResult.addElement(LookupElementBuilder.create(lookupItemPrefix + "is").withInsertHandler(XmlAttributeInsertHandler.INSTANCE))
    newResult.addElement(LookupElementBuilder.create(lookupItemPrefix + "key").withInsertHandler(XmlAttributeInsertHandler.INSTANCE))

    // v-bind:any-standard-attribute support
    for (attribute in VueElementDescriptor.getDefaultHtmlAttributes(attr.parent)) {
      newResult.addElement(LookupElementBuilder
                             .create(lookupItemPrefix + attribute.name)
                             .withInsertHandler(XmlAttributeInsertHandler.INSTANCE))
    }

    // see also VueElementDescriptor.getAttributesDescriptors()
    val jsElement = (attr.parent?.descriptor as? VueElementDescriptor)?.declaration ?: return
    val obj = VueComponents.findComponentDescriptor(jsElement)
    for (attribute in VueComponentDetailsProvider.INSTANCE.getAttributes(obj, attr.project, true, xmlContext = true)) {
      newResult.addElement(LookupElementBuilder
                             .create(lookupItemPrefix + attribute.name)
                             .withInsertHandler(XmlAttributeInsertHandler.INSTANCE))
    }
  }
}

private class VueEventAttrDataCompletionProvider : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    if (!hasVue(parameters.position.project)) return
    if (parameters.position.parent.parent is HtmlTagImpl && (parameters.position.parent.parent as HtmlTagImpl).name.contains("v-icon")) {
      VuetifyIcons.materialAndFontAwesome.forEach {
        result.addElement(LookupElementBuilder.create(it).withIcon(VuejsIcons.Vue))
      }
    }
  }

}
