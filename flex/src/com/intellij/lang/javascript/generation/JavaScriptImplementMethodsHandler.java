/*
 * @author max
 */
package com.intellij.lang.javascript.generation;

import com.intellij.featureStatistics.ProductivityFeatureNames;
import com.intellij.lang.javascript.JSBundle;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.validation.ActionScriptImplementedMethodProcessor;
import com.intellij.lang.javascript.validation.fixes.BaseCreateMethodsFix;
import com.intellij.lang.javascript.validation.fixes.ImplementMethodsFix;

import java.util.Collection;

/**
 * as only
 */
public class JavaScriptImplementMethodsHandler extends BaseJSGenerateHandler {
  protected void collectCandidates(final JSClass clazz, final Collection<JSNamedElementNode> candidates) {
    for(JSFunction fun: ActionScriptImplementedMethodProcessor.collectFunctionsToImplement(clazz)) {
      candidates.add(new JSNamedElementNode(fun));
    }
  }

  protected String getTitleKey() {
    return "methods.to.implement.chooser.title";
  }

  protected String getNoCandidatesMessage() {
    return JSBundle.message("no.methods.to.implement");
  }

  protected BaseCreateMethodsFix createFix(final JSClass clazz) {
    return new ImplementMethodsFix(clazz);
  }

  @Override
  protected String getProductivityFeatureId() {
    return ProductivityFeatureNames.CODEASSISTS_OVERRIDE_IMPLEMENT;
  }
}