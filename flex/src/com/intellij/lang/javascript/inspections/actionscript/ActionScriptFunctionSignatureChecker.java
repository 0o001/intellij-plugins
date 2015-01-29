package com.intellij.lang.javascript.inspections.actionscript;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.javascript.JSBundle;
import com.intellij.lang.javascript.inspections.JSCheckFunctionSignaturesInspection;
import com.intellij.lang.javascript.inspections.JSValidateTypesInspection;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.JSSuperExpression;
import com.intellij.lang.javascript.psi.types.primitives.JSObjectType;
import com.intellij.lang.javascript.validation.JSAnnotatorProblemReporter;
import com.intellij.lang.javascript.validation.JSFunctionSignatureChecker;
import com.intellij.lang.javascript.validation.JSTypeChecker;
import com.intellij.lang.javascript.validation.ValidateTypesUtil;
import com.intellij.lang.javascript.validation.fixes.CreateConstructorFix;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin.Ulitin
 */
public class ActionScriptFunctionSignatureChecker extends JSFunctionSignatureChecker {
  private final JSAnnotatorProblemReporter myReporter;

  public ActionScriptFunctionSignatureChecker(JSTypeChecker typeChecker, JSAnnotatorProblemReporter reporter) {
    super(typeChecker);
    myReporter = reporter;
  }

  @Override
  protected void registerProblem(JSCallExpression callExpression, String message, LocalQuickFix... fixes) {
    PsiElement place = ValidateTypesUtil.getPlaceForSignatureProblem(callExpression, callExpression.getArgumentList());
    myReporter.registerProblem(place, message, ProblemHighlightType.GENERIC_ERROR,
                                               JSValidateTypesInspection.SHORT_NAME,
                                               fixes);
  }

  @Override
  public void checkFunction(@NotNull JSCallExpression node, @NotNull PsiElement element) {
    super.checkFunction(node, element);
    if (element instanceof JSClass) {
      if (node instanceof JSNewExpression || node.getMethodExpression() instanceof JSSuperExpression) {
        final JSArgumentList argumentList = node.getArgumentList();
        final JSExpression[] expressions = argumentList != null ? argumentList.getArguments() : JSExpression.EMPTY_ARRAY;
        if (expressions.length > 0) {
          final CreateConstructorFix fix = CreateConstructorFix.createIfApplicable(node);
          myReporter.registerProblem(
            ValidateTypesUtil.getPlaceForSignatureProblem(node, argumentList),
            JSBundle.message("javascript.invalid.number.of.parameters", "0"),
            ProblemHighlightType.GENERIC_ERROR, JSCheckFunctionSignaturesInspection.SHORT_NAME,
            fix != null ? new LocalQuickFix[]{fix} : LocalQuickFix.EMPTY_ARRAY);
          return;
        }
        checkCallParameters(node, null);
      }
      else {
        reportProblemIfNotExpectedCountOfParameters(node, 1, "one");
      }
    }
  }

  @Override
  protected boolean isCallableType(boolean inNewExpression, @NotNull JSType type) {
    final String typeText = type.getTypeText();
    return "Class".equals(typeText) ||
           inNewExpression && type instanceof JSObjectType ||
           JSTypeUtils.hasFunctionType(type);
  }

  @Override
  protected void checkCallArgumentType(JSParameterItem p, JSExpression expression, JSCallExpression node, PsiElement resolveResult) {
    if (p instanceof JSParameter) {
      myTypeChecker.checkExpressionIsAssignableToVariable((JSParameter)p, expression, node.getContainingFile(),
                                                           "javascript.argument.type.mismatch", false);
    }
  }

  @Override
  protected boolean obtainNextMatchedParams(int[] matchedParams, JSParameterItem[] parameters) {
    return false;
  }
}
