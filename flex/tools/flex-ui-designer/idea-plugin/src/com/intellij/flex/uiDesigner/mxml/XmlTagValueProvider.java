// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.flex.uiDesigner.mxml;

import com.google.common.base.CharMatcher;
import com.intellij.flex.uiDesigner.InjectionUtil;
import com.intellij.lang.javascript.flex.AnnotationBackedDescriptor;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagChild;
import com.intellij.psi.xml.XmlText;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// isCollapseWhiteSpace - only for tag: https://bugs.adobe.com/jira/browse/SDK-3983
class XmlTagValueProvider implements XmlElementValueProvider {
  private XmlTag tag;

  public XmlTag getTag() {
    return tag;
  }

  public void setTag(XmlTag tag) {
    this.tag = tag;
  }

  @Override
  public String getTrimmed() {
    return tag.getValue().getTrimmedText();
  }

  @Override
  public CharSequence getSubstituted() {
    CharSequence v = getDisplay(tag.getValue().getChildren());
    if (v == EMPTY) {
      return EMPTY;
    }

    XmlElementDescriptor descriptor = tag.getDescriptor();
    // may be ClassBackedElementDescriptor for fx:String: <TextArea><text><fx:String>sfsdsd</fx:String></text></TextArea>
    if (descriptor instanceof AnnotationBackedDescriptor && ((AnnotationBackedDescriptor)descriptor).isCollapseWhiteSpace()) {
      return CharMatcher.whitespace().trimAndCollapseFrom(v, ' ');
    }
    else {
      return v;
    }
  }

  @Override
  public PsiLanguageInjectionHost getInjectedHost() {
    return MxmlUtil.getInjectedHost(tag);
  }

  @Override
  @Nullable
  public JSClass getJsClass() {
    return InjectionUtil.getJsClassFromPackageAndLocalClassNameReferences(tag);
  }

  @NotNull
  @Override
  public XmlElement getElement() {
    return tag;
  }

  @Override
  public PsiMetaData getPsiMetaData() {
    return tag.getDescriptor();
  }

  private static CharSequence getDisplay(XmlTagChild[] children) {
    if (children.length == 1) {
      if (children[0] instanceof XmlText) {
        return ((XmlText)children[0]).getValue();
      }
      else {
        return EMPTY;
      }
    }
    else {
      final StringBuilder consolidatedText = new StringBuilder();
      for (final XmlTagChild element : children) {
        consolidatedText.append(element instanceof XmlText ? ((XmlText)element).getValue() : element.getText());
      }
      return consolidatedText.length() == 0 ? EMPTY : consolidatedText;
    }
  }
}
