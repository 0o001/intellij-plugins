// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.lang.html.psi.impl;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import org.angular2.lang.html.parser.Angular2HtmlElementTypes.Angular2ElementType;
import org.angular2.lang.html.psi.Angular2HtmlBananaBoxBinding;
import org.angular2.lang.html.psi.Angular2HtmlElementVisitor;
import org.jetbrains.annotations.NotNull;

public class Angular2HtmlBananaBoxBindingImpl extends Angular2HtmlPropertyBindingBase implements Angular2HtmlBananaBoxBinding {

  private static final Pair<String, String> DELIMITERS = Pair.pair("[(", ")]");
  private static final String PREFIX = "bindon-";

  public Angular2HtmlBananaBoxBindingImpl(@NotNull Angular2ElementType type) {
    super(type);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof Angular2HtmlElementVisitor) {
      ((Angular2HtmlElementVisitor)visitor).visitBananaBoxBinding(this);
    }
    else if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor)visitor).visitXmlAttribute(this);
    }
    else {
      visitor.visitElement(this);
    }
  }

  @Override
  protected Pair<String, String> getDelimiters() {
    return DELIMITERS;
  }

  @Override
  protected String getPrefix() {
    return PREFIX;
  }

  @Override
  protected boolean supportsEvents() {
    return false;
  }

  @Override
  public String toString() {
    return "Angular2HtmlBananaBoxBinding " + getNameAndType();
  }
}
