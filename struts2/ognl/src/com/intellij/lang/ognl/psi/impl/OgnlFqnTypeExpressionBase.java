/*
 * Copyright 2015 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.lang.ognl.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ognl.psi.OgnlFqnTypeExpression;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class OgnlFqnTypeExpressionBase extends OgnlExpressionImpl implements OgnlFqnTypeExpression {

  protected OgnlFqnTypeExpressionBase(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    JavaClassReferenceProvider referenceProvider = new JavaClassReferenceProvider();
    referenceProvider.setOption(JavaClassReferenceProvider.DEFAULT_PACKAGE, CommonClassNames.DEFAULT_PACKAGE);

    OgnlPsiUtil.customizeFqnTypeExpressionReferences(this, referenceProvider);

    final JavaClassReferenceSet classReferenceSet =
      new JavaClassReferenceSet(getText(), this, 0, false, referenceProvider);
    return classReferenceSet.getReferences();
  }

  @Nullable
  @Override
  public PsiType getType() {
    final String text = getText();
    final String qualifiedClassName = StringUtil.containsChar(text, '.') ? text : CommonClassNames.DEFAULT_PACKAGE + "." + text;
    final String escapedName = StringUtil.replaceChar(qualifiedClassName, '$', '.');
    return JavaPsiFacade.getInstance(getProject()).getElementFactory().createTypeByFQClassName(escapedName);
  }
}