package com.intellij.coldFusion.model.psi.impl;

import com.intellij.coldFusion.UI.CfmlLookUpItemUtil;
import com.intellij.coldFusion.model.info.CfmlFunctionDescription;
import com.intellij.coldFusion.model.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiType;
import com.intellij.ui.RowIcon;
import com.intellij.util.PlatformIcons;
import icons.CFMLIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * User: vnikolaenko
 * Date: 29.04.2009
 */
public class CfmlTagFunctionImpl extends CfmlNamedTagImpl implements CfmlFunction, PlatformIcons {
  public static final String TAG_NAME = "cffunction";

  public CfmlTagFunctionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  public String getParametersAsString() {
    return getFunctionInfo().getParametersListPresentableText();
  }

  @NotNull
  public CfmlParameter[] getParameters() {
    return findChildrenByClass(CfmlParameter.class);
  }

  @Nullable
  public PsiType getReturnType() {
    final String returnTypeString = CfmlPsiUtil.getPureAttributeValue(this, "returntype");
    return returnTypeString != null ?
           new CfmlComponentType(returnTypeString, getContainingFile(), getProject()) : null;
  }

  @Override
  public Icon getIcon(int flags) {
    String access = CfmlPsiUtil.getPureAttributeValue(this, "access");
    if (access == null) {
      return METHOD_ICON;
    }
    access = access.toLowerCase();
    RowIcon baseIcon = new RowIcon(2);
    baseIcon.setIcon(METHOD_ICON, 0);
    if ("private".equals(access)) {
      baseIcon.setIcon(PRIVATE_ICON, 1);
    }
    else if ("package".equals(access)) {
      baseIcon.setIcon(PACKAGE_LOCAL_ICON, 1);
    }
    else if ("public".equals(access)) {
      baseIcon.setIcon(PUBLIC_ICON, 1);
    }
    else if ("remote".equals(access)) {
      baseIcon.setIcon(CFMLIcons.Remote_access, 1);
    }
    return baseIcon;
  }

  public String getTagName() {
    return TAG_NAME;
  }

  public boolean isTrulyDeclaration() {
    return true;
  }

  @NotNull
  public CfmlFunctionDescription getFunctionInfo() {
    return CfmlLookUpItemUtil.getFunctionDescription(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfmlRecursiveElementVisitor) {
      ((CfmlRecursiveElementVisitor)visitor).visitCfmlFunction(this);
    }
    else {
      super.accept(visitor);
    }
  }
}
