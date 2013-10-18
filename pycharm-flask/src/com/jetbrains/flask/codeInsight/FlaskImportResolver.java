/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
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
package com.jetbrains.flask.codeInsight;

import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.PyPsiFacade;
import com.jetbrains.python.psi.impl.PyImportResolver;
import com.intellij.psi.util.QualifiedName;
import com.jetbrains.python.psi.resolve.QualifiedNameResolveContext;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class FlaskImportResolver implements PyImportResolver {

  @Nullable
  @Override
  public PsiElement resolveImportReference(QualifiedName qualifiedName,
                                           QualifiedNameResolveContext resolveContext) {
    if (qualifiedName.matchesPrefix(FlaskNames.FLASK_EXT) && qualifiedName.getComponentCount() >= 3) {
      PyPsiFacade psiFacade = PyPsiFacade.getInstance(resolveContext.getProject());
      String topName = qualifiedName.getComponents().get(2);
      QualifiedName subName = qualifiedName.removeHead(3);
      QualifiedName qName = QualifiedName.fromComponents("flask_" + topName).append(subName);
      PsiElement item = psiFacade.qualifiedNameResolver(qName).withContext(resolveContext).firstResult();
      if (item != null) {
        return item;
      }
      qName = QualifiedName.fromComponents(FlaskNames.FLASKEXT, topName).append(subName);
      item = psiFacade.qualifiedNameResolver(qName).withContext(resolveContext).withPlainDirectories().firstResult();
      if (item != null) {
        return item;
      }
    }
    return null;
  }
}
