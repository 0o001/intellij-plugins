package com.intellij.lang.javascript.uml;

import com.intellij.diagram.AbstractDiagramNodeContentManager;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.impl.JSFunctionImpl;
import com.intellij.lang.javascript.psi.util.JSUtils;
import com.intellij.psi.PsiElement;
import com.intellij.util.PlatformIcons;

public class FlashUmlNodeContentManager extends AbstractDiagramNodeContentManager {

  private static final DiagramCategory FIELDS = new DiagramCategory("Fields", PlatformIcons.FIELD_ICON);
  private static final DiagramCategory CONSTRUCTORS = new DiagramCategory("Constructors", JSFunctionImpl.CONSTRUCTOR_ICON);
  private static final DiagramCategory METHODS = new DiagramCategory("Methods", PlatformIcons.METHOD_ICON);
  private static final DiagramCategory PROPERTIES = new DiagramCategory("Properties", PlatformIcons.PROPERTY_ICON);

  private final static DiagramCategory[] CATEGORIES = {FIELDS, CONSTRUCTORS, METHODS, PROPERTIES};

  @Override
  public DiagramCategory[] getContentCategories() {
    return CATEGORIES;
  }

  @Override
  public boolean isInCategory(Object obj, DiagramCategory category, DiagramState presentation) {
    if (!(obj instanceof PsiElement)) return false;
    PsiElement element = (PsiElement)obj;

    if (JSUtils.getMemberContainingClass(element) == null) return false;

    if (FIELDS.equals(category)) {
      return element instanceof JSVariable;
    }
    if (CONSTRUCTORS.equals(category)) {
      return element instanceof JSFunction && ((JSFunction)element).getKind() == JSFunction.FunctionKind.CONSTRUCTOR;
    }
    if (METHODS.equals(category)) {
      return element instanceof JSFunction && ((JSFunction)element).getKind() == JSFunction.FunctionKind.SIMPLE;
    }

    if (PROPERTIES.equals(category)) {
      return element instanceof JSFunction &&
             (((JSFunction)element).getKind() == JSFunction.FunctionKind.GETTER ||
              ((JSFunction)element).getKind() == JSFunction.FunctionKind.SETTER);
    }
    return false;
  }
}
