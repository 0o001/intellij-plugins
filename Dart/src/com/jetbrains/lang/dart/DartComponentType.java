package com.jetbrains.lang.dart;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.*;
import icons.DartIcons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public enum DartComponentType {
  CLASS(DartIcons.Class_dart),
  FUNCTION(DartIcons.Function_dart),
  METHOD(DartIcons.Method_dart),
  VARIABLE(DartIcons.Variable_dart),
  FIELD(DartIcons.Field_dart),
  PARAMETER(DartIcons.Parameter_dart),
  TYPEDEF(DartIcons.Annotationtype_dart),
  CONSTRUCTOR(DartIcons.Class_dart),
  OPERATOR(DartIcons.Method_dart),
  LABEL(DartIcons.Label_dart);

  private final Icon myIcon;

  DartComponentType(final Icon icon) {
    myIcon = icon;
  }

  public int getKey() {
    return ordinal();
  }

  public Icon getIcon() {
    return myIcon;
  }

  @Nullable
  public static DartComponentType valueOf(int key) {
    return key >= 0 && key < values().length ? values()[key] : null;
  }

  @Nullable
  public static DartComponentType typeOf(@Nullable PsiElement element) {
    if (element instanceof DartComponentName) {
      return typeOf(element.getParent());
    }
    if ((element instanceof DartComponent && PsiTreeUtil.getParentOfType(element, DartNormalFormalParameter.class, false) != null) ||
        element instanceof DartNormalFormalParameter) {
      return PARAMETER;
    }
    if (element instanceof DartClassDefinition) {
      return CLASS;
    }
    if (element instanceof DartFunctionTypeAlias || element instanceof DartClassTypeAlias) {
      return TYPEDEF;
    }
    if (element instanceof DartNamedConstructorDeclaration
        || element instanceof DartFactoryConstructorDeclaration) {
      return CONSTRUCTOR;
    }
    if (element instanceof DartFunctionSignature
        || element instanceof DartFunctionDeclarationWithBody
        || element instanceof DartFunctionDeclarationWithBodyOrNative
        || element instanceof DartFunctionExpression) {
      return FUNCTION;
    }
    if (element instanceof DartOperatorDeclaration
        || element instanceof DartAbstractOperatorDeclaration) {
      return OPERATOR;
    }
    if (element instanceof DartGetterDeclaration || element instanceof DartSetterDeclaration) {
      final PsiElement dartClassCandidate = PsiTreeUtil.getParentOfType(element, DartComponent.class, DartOperator.class);
      return dartClassCandidate instanceof DartClass ? METHOD : FUNCTION;
    }
    if (element instanceof DartMethodDeclaration) {
      final DartClass dartClass = PsiTreeUtil.getParentOfType(element, DartClass.class);
      final String dartClassName = dartClass != null ? dartClass.getName() : null;
      return dartClassName != null && dartClassName.equals(((DartComponent)element).getName()) ? CONSTRUCTOR : METHOD;
    }
    if (element instanceof DartVarAccessDeclaration
        || element instanceof DartVarDeclarationListPart) {
      return PsiTreeUtil.getParentOfType(element, DartComponent.class, DartOperator.class) instanceof DartClass ? FIELD : VARIABLE;
    }

    if (element instanceof DartForInPart) {
      return VARIABLE;
    }

    if (element instanceof DartLabel) {
      return LABEL;
    }

    return null;
  }

}
