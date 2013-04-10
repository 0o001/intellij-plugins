package com.google.jstestdriver.idea.assertFramework.qunit;

import com.google.jstestdriver.idea.assertFramework.AbstractTestFileStructureBuilder;
import com.google.jstestdriver.idea.util.JsPsiUtils;
import com.intellij.lang.javascript.psi.*;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QUnitFileStructureBuilder extends AbstractTestFileStructureBuilder<QUnitFileStructure> {

  private static final QUnitFileStructureBuilder INSTANCE = new QUnitFileStructureBuilder();
  public static final String MODULE_NAME = "module";
  public static final String TEST_NAME = "test";
  public static final String ASYNC_TEST_NAME = "asyncTest";

  @NotNull
  @Override
  public QUnitFileStructure buildTestFileStructure(@NotNull JSFile jsFile) {
    return new Builder(jsFile).build();
  }

  private static class Builder {
    private final QUnitFileStructure myFileStructure;
    @NotNull
    private AbstractQUnitModuleStructure myCurrentModuleStructure;

    private Builder(@NotNull JSFile jsFile) {
      myFileStructure = new QUnitFileStructure(jsFile);
      myCurrentModuleStructure = myFileStructure.getDefaultModuleStructure();
    }

    public QUnitFileStructure build() {
      List<JSStatement> jsStatements = JsPsiUtils.listStatementsInExecutionOrder(myFileStructure.getJsFile());
      for (JSStatement jsStatement : jsStatements) {
        update(jsStatement);
      }
      myFileStructure.postProcess();
      return myFileStructure;
    }

    private void update(JSElement jsElement) {
      JSExpressionStatement expressionStatement = ObjectUtils.tryCast(jsElement, JSExpressionStatement.class);
      if (expressionStatement != null) {
        JSCallExpression callExpression = ObjectUtils.tryCast(expressionStatement.getExpression(), JSCallExpression.class);
        if (callExpression != null) {
          updateJsCallExpression(callExpression);
        }
      }
    }

    private void updateJsCallExpression(@NotNull JSCallExpression callExpression) {
      JSReferenceExpression methodExpression = ObjectUtils.tryCast(callExpression.getMethodExpression(), JSReferenceExpression.class);
      JSArgumentList argumentList = callExpression.getArgumentList();
      if (methodExpression != null && methodExpression.getQualifier() == null && argumentList != null) {
        String methodName = methodExpression.getReferencedName();
        JSExpression[] arguments = ObjectUtils.notNull(argumentList.getArguments(), JSExpression.EMPTY_ARRAY);
        if (arguments.length >= 1) {
          String name = JsPsiUtils.extractStringValue(arguments[0]);
          if (name != null) {
            if (MODULE_NAME.equals(methodName)) {
              boolean ok = arguments.length == 1;
              JSObjectLiteralExpression lifecycleObjectLiteral = null;
              if (arguments.length == 2) {
                lifecycleObjectLiteral = JsPsiUtils.extractObjectLiteralExpression(arguments[1]);
                if (lifecycleObjectLiteral != null) {
                  ok = true;
                }
              }
              if (ok) {
                QUnitModuleStructure moduleStructure = new QUnitModuleStructure(myFileStructure, name, callExpression, lifecycleObjectLiteral);
                myFileStructure.addModuleStructure(moduleStructure);
                myCurrentModuleStructure = moduleStructure;
              }
            }
            else if ((TEST_NAME.equals(methodName) || ASYNC_TEST_NAME.equals(methodName)) && arguments.length == 2) {
              JSFunctionExpression body = JsPsiUtils.extractFunctionExpression(arguments[1]);
              if (body != null) {
                QUnitTestMethodStructure testMethodStructure = new QUnitTestMethodStructure(myCurrentModuleStructure, name, callExpression, body);
                myCurrentModuleStructure.addTestMethodStructure(testMethodStructure);
              }
            }
          }
        }
      }
    }
  }

  public static QUnitFileStructureBuilder getInstance() {
    return INSTANCE;
  }

}
