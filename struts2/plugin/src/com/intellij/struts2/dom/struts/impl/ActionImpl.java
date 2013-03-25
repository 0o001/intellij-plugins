/*
 * Copyright 2013 The authors
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

package com.intellij.struts2.dom.struts.impl;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.VolatileNotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.struts2.dom.struts.action.Action;
import com.intellij.struts2.dom.struts.strutspackage.DefaultClassRef;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Adds utility methods.
 *
 * @author Yann C&eacute;bron
 */
@SuppressWarnings({"AbstractClassNeverImplemented"})
public abstract class ActionImpl implements Action {

  private final NotNullLazyValue<String> myActionName = new VolatileNotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      return getName().getRawText();
    }
  };

  @Nullable
  private String getNameValue() {
    return myActionName.getValue();
  }

  @Override
  public boolean isWildcardMapping() {
    final String name = getNameValue();
    return name != null && StringUtil.indexOf(name, '*') != -1;
  }

  public boolean matchesPath(@NotNull final String path) {
    final String myPath = getNameValue();
    if (myPath == null) {
      return false;
    }

    return ActionUtil.matchesPath(myPath, path);
  }

  @NotNull
  public StrutsPackage getStrutsPackage() {
    final StrutsPackage strutsPackage = DomUtil.getParentOfType(this, StrutsPackage.class, true);
    assert strutsPackage != null : "could not resolve enclosing <package> for " + this + " (" + getNameValue() + ")";
    return strutsPackage;
  }

  @Nullable
  public PsiClass searchActionClass() {
    final GenericAttributeValue<PsiClass> actionClassAttribute = getActionClass();
    if (DomUtil.hasXml(actionClassAttribute)) {
      return actionClassAttribute.getValue();
    }

    // resolve parent package <default-class-ref> (walk upwards)
    final DefaultClassRef ref = getStrutsPackage().searchDefaultClassRef();
    if (ref != null) {
      return ref.getDefaultClass().getValue();
    }

    // nothing found in parents --> error highlighting
    return null;
  }

  @Nullable
  public PsiMethod searchActionMethod() {
    final GenericAttributeValue<PsiMethod> methodValue = getMethod();
    if (DomUtil.hasXml(methodValue)) {
      return methodValue.getValue();
    }

    return findActionMethod(DEFAULT_ACTION_METHOD_NAME);
  }

  @NotNull
  public String getNamespace() {
    return getStrutsPackage().searchNamespace();
  }

  @NotNull
  public List<PsiMethod> getActionMethods() {
    final PsiClass actionClass = searchActionClass();
    if (actionClass == null) {
      return Collections.emptyList();
    }

    return ActionUtil.findActionMethods(actionClass, null);
  }

  @Override
  public PsiMethod findActionMethod(final String methodName) {
    if (methodName == null) {
      return null;
    }

    final PsiClass actionClass = searchActionClass();
    if (actionClass == null) {
      return null;
    }

    final List<PsiMethod> actionMethods = ActionUtil.findActionMethods(actionClass, methodName);
    return actionMethods.size() == 1 ? actionMethods.get(0) : null;
  }

  @Nullable
  public PsiClass getParamsClass() {
    return searchActionClass();
  }
}