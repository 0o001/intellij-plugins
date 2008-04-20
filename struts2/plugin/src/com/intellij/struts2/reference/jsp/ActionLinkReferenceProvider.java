/*
 * Copyright 2008 The authors
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
package com.intellij.struts2.reference.jsp;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupValueFactory;
import com.intellij.javaee.web.CustomServletReferenceAdapter;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlElement;
import com.intellij.struts2.StrutsIcons;
import com.intellij.struts2.dom.struts.action.Action;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.dom.struts.model.StrutsModel;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides links to Action-URLs in all places where Servlet-URLs are processed.
 *
 * @author Yann C�bron
 */
public class ActionLinkReferenceProvider extends CustomServletReferenceAdapter {

  protected PsiReference[] createReferences(final @NotNull PsiElement psiElement,
                                            final int offset,
                                            final String text,
                                            final @Nullable ServletMappingInfo info,
                                            final boolean soft) {
    final StrutsModel strutsModel = StrutsManager.getInstance(psiElement.getProject()).
            getCombinedModel(ModuleUtil.findModuleForPsiElement(psiElement));

    if (strutsModel == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    if (text.indexOf("/") != -1) {
      return new PsiReference[]{
              new ActionLinkPackageReference((XmlElement) psiElement, offset, text, soft, strutsModel),
              new ActionLinkReference((XmlElement) psiElement, offset, text, soft, strutsModel)
      };
    } else {
      return new PsiReference[]{
              new ActionLinkReference((XmlElement) psiElement, offset, text, soft, strutsModel)
      };

    }
  }

  @Nullable
  public PathReference createWebPath(final String path,
                                     @NotNull final PsiElement psiElement,
                                     final ServletMappingInfo servletMappingInfo) {
    final StrutsManager strutsManager = StrutsManager.getInstance(psiElement.getProject());
    if (strutsManager.getCombinedModel(ModuleUtil.findModuleForPsiElement(psiElement)) == null) {
      return null;
    }

    return new PathReference(path, new PathReference.ConstFunction(StrutsIcons.ACTION)); /*{
TODO not needed so far ?!
   public PsiElement resolve() {
        return action.getXmlTag();
      }
    };*/
  }


  private static class ActionLinkReference extends PsiReferenceBase<XmlElement> implements EmptyResolveMessageProvider {

    private final StrutsModel strutsModel;
    private final String fullActionPath;

    private ActionLinkReference(final XmlElement element,
                                final int offset,
                                final String text,
                                final boolean soft,
                                final StrutsModel strutsModel) {
      super(element, new TextRange(offset, offset + text.length()), soft);
      this.strutsModel = strutsModel;

      fullActionPath = PathReference.trimPath(getValue());
      final int lastSlash = fullActionPath.lastIndexOf("/");

      // adapt TextRange to everything behind /packageName/
      if (lastSlash != -1) {
        setRangeInElement(TextRange.from(lastSlash + 2, fullActionPath.length() - lastSlash - 1));
      }

      // reduce to action-name if full path given
      // TODO hardcoded extension
      final int extensionIndex = fullActionPath.indexOf(".action");
      if (extensionIndex != -1) {
        setRangeInElement(TextRange.from(getRangeInElement().getStartOffset(),
                                         getRangeInElement().getLength() - ".action".length()));
      }
    }

    public PsiElement resolve() {
      // TODO hardcoded extension
      if (!fullActionPath.endsWith(".action")) {
        return null;
      }

      final String actionName = getActionName(fullActionPath);
      final String namespace = getNamespace(fullActionPath, -1, true);
      final List<Action> actions = strutsModel.findActionsByName(actionName, namespace);
      if (actions.isEmpty()) {
        return null;
      }

      final Action myAction = actions.get(0);
      return myAction.getXmlTag();
    }

    public Object[] getVariants() {
      final String namespace = getNamespace(fullActionPath, getRangeInElement().getStartOffset(), true);

      // namespace given?
      final List<Action> actionList;
      if (StringUtil.isNotEmpty(namespace)) {
        actionList = strutsModel.getActionsForNamespace(namespace);
      } else {
        actionList = new ArrayList<Action>();
        final List<StrutsPackage> strutsPackages = strutsModel.getStrutsPackages();
        for (final StrutsPackage strutsPackage : strutsPackages) {
          actionList.addAll(strutsPackage.getActions());
        }
      }

      final List<Object> variants = new ArrayList<Object>(actionList.size());
      for (final Action action : actionList) {
        final String actionPath = action.getName().getStringValue();
        if (actionPath != null) {
          // TODO hardcoded extension
          final Object variant = LookupValueFactory.createLookupValueWithHint(
                  actionPath + ".action",
                  StrutsIcons.ACTION,
                  action.getNamespace());
          variants.add(variant);
        }
      }
      return variants.toArray(new Object[variants.size()]);
    }

    public String getUnresolvedMessagePattern() {
      return "Cannot resolve action ''" + getValue() + "''";
    }

    @NotNull
    private static String getActionName(final String fullActionPath) {
      final int slashIndex = fullActionPath.lastIndexOf("/");
      // TODO hardcoded extension
      final int extensionIndex = fullActionPath.lastIndexOf(".action");
      return fullActionPath.substring(slashIndex + 1, extensionIndex);
    }

  }

  /**
   * Extracts the namespace from the given action path.
   *
   * @param fullActionPath Full path.
   * @param searchStart    Start position in path String (only if fakeRoot=false).
   * @param fakeRoot       Return fake root-namespace instead of empty String if none found.
   * @return Namespace
   */
  @NotNull
  private static String getNamespace(final String fullActionPath, final int searchStart, final boolean fakeRoot) {
    final int slashIndex = fullActionPath.lastIndexOf("/", fakeRoot ? fullActionPath.length() : searchStart);

    // no slash, use fake "root" for resolving "myAction.action"
    if (slashIndex == -1) {
      return "/";
    }

    // root-package
    if (slashIndex == 0) {
      return fakeRoot ? "/" : "";
    }

    return fullActionPath.substring(0, slashIndex);
  }


  /**
   * Provides reference to S2-package within action-path.
   */
  private static class ActionLinkPackageReference extends PsiReferenceBase<XmlElement> implements EmptyResolveMessageProvider {

    private final String namespace;
    private final List<StrutsPackage> allStrutsPackages;
    private final String fullActionPath;

    private ActionLinkPackageReference(final XmlElement element,
                                       final int offset,
                                       final String text,
                                       final boolean soft,
                                       final StrutsModel strutsModel) {
      super(element, new TextRange(offset, offset + text.length()), soft);

      fullActionPath = PathReference.trimPath(getValue());
      namespace = getNamespace(fullActionPath, getRangeInElement().getStartOffset(), true);

      final int firstSlash = fullActionPath.indexOf("/");
      if (firstSlash != -1) {
        setRangeInElement(TextRange.from(firstSlash + 1, namespace.length()));
      }

      allStrutsPackages = strutsModel.getStrutsPackages();
    }

    public PsiElement resolve() {
      if (!fullActionPath.endsWith(".action")) {
        return null;
      }

      for (final StrutsPackage strutsPackage : allStrutsPackages) {
        if (namespace.equals(strutsPackage.searchNamespace())) {
          return strutsPackage.getXmlTag();
        }
      }

      return null;
    }

    public Object[] getVariants() {
      final Set<Object> variants = new HashSet<Object>(allStrutsPackages.size());

      for (final StrutsPackage allPackage : allStrutsPackages) {
        final String namespace = allPackage.searchNamespace();
        variants.add(LookupValueFactory.createLookupValueWithHint(
                namespace.length() != 1 ? namespace + "/" : namespace,
                StrutsIcons.PACKAGE, allPackage.getName().getStringValue()));
      }

      return variants.toArray(new Object[variants.size()]);
    }

    public String getUnresolvedMessagePattern() {
      return "Cannot resolve Struts 2 package ''" + getCanonicalText() + "''";
    }
  }

}