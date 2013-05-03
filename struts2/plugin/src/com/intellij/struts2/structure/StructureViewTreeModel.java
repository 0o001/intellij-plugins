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

package com.intellij.struts2.structure;

import com.intellij.icons.AllIcons;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.xml.XmlFileTreeElement;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.StrutsBundle;
import com.intellij.struts2.dom.params.Param;
import com.intellij.util.Function;
import com.intellij.util.ReflectionCache;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.structure.DomStructureTreeElement;
import com.intellij.util.xml.structure.DomStructureViewTreeModel;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the sorters, filters etc. available for structure view.
 *
 * @author Yann C&eacute;bron
 */
class StructureViewTreeModel extends DomStructureViewTreeModel implements StructureViewModel.ElementInfoProvider {

  static final String HIDE_PARAMS_ID = StrutsBundle.message("structure.view.filter.params");

  private final Class[] alwaysPlus;
  private final Class[] alwaysLeaf;

  StructureViewTreeModel(@NotNull final XmlFile xmlFile,
                         @NotNull final Class[] alwaysPlus,
                         @NotNull final Class[] alwaysLeaf,
                         Function<DomElement, DomService.StructureViewMode> descriptor) {
    super(xmlFile, descriptor);
    this.alwaysPlus = alwaysPlus;
    this.alwaysLeaf = alwaysLeaf;
  }

  @NotNull
  @Override
  public StructureViewTreeElement getRoot() {
    final XmlFile xmlFile = (XmlFile)getPsiFile();
    final DomFileElement<DomElement> fileElement = DomManager.getDomManager(xmlFile.getProject()).getFileElement(xmlFile, DomElement.class);
    if (fileElement == null) {
      return new XmlFileTreeElement(xmlFile);
    }

    return new com.intellij.struts2.structure.StructureViewTreeElement(fileElement.getRootElement().createStableCopy());
  }

  @NotNull
  public Filter[] getFilters() {
    return new Filter[]{new Filter() {
      public boolean isVisible(final TreeElement treeElement) {
        DomStructureTreeElement domStructureTreeElement = (DomStructureTreeElement)treeElement;
        return !(domStructureTreeElement.getElement() instanceof Param);
      }

      public boolean isReverted() {
        return false;
      }

      @NotNull
      public ActionPresentation getPresentation() {
        return new ActionPresentationData(StrutsBundle.message("structure.view.filter.params"),
                                          StrutsBundle.message("structure.view.filter.params"), AllIcons.Actions.Properties);
      }

      @NotNull
      public String getName() {
        return HIDE_PARAMS_ID;
      }
    }};
  }


  @Override
  public boolean isAlwaysShowsPlus(final StructureViewTreeElement element) {
    return isDomElementOfKind(element, alwaysPlus);
  }

  @Override
  public boolean isAlwaysLeaf(final StructureViewTreeElement element) {
    return isDomElementOfKind(element, alwaysLeaf);
  }

  private static boolean isDomElementOfKind(final StructureViewTreeElement element,
                                            final Class... kinds) {
    final DomElement domElement = ((DomStructureTreeElement)element).getElement();
    for (final Class clazz : kinds) {
      if (ReflectionCache.isInstance(domElement, clazz)) {
        return true;
      }
    }
    return false;
  }
}
