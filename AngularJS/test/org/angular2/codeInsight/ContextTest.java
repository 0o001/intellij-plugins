// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.codeInsight;

import com.intellij.lang.javascript.JSBundle;
import com.intellij.lang.javascript.JSTestUtils;
import com.intellij.lang.javascript.dialects.JSLanguageLevel;
import com.intellij.lang.javascript.inspections.*;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptFieldImpl;
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptParameterImpl;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.TestLookupElementPresentation;
import one.util.streamex.StreamEx;
import org.angularjs.AngularTestUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.util.containers.ContainerUtil.newArrayList;

public class ContextTest extends LightPlatformCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return AngularTestUtil.getBaseTestDataPath(getClass()) + "context";
  }

  @NotNull
  private PsiElement resolveReference(@NotNull String signature) {
    return AngularTestUtil.resolveReference(signature, myFixture);
  }

  public void testInlineTemplateCompletion2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, myFixture.getProject(),
                                        () -> myFixture.testCompletion("component.ts", "component.after.ts", "package.json"));
  }

  public void testInlineTemplateResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("component.after.ts", "package.json");
      PsiElement resolve = resolveReference("=\"onComple<caret>tedButton()");
      assertEquals("component.after.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, JSFunction.class);
    });
  }

  public void testInlineTemplateMethodResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("templateMethod.ts", "package.json", "customer.ts", "customer2.ts");
      PsiElement resolve = resolveReference("ca<caret>ll()");
      assertEquals("customer.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFunction.class);
    });
  }

  public void testNonInlineTemplateCompletion2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, myFixture.getProject(),
                                        () -> myFixture.testCompletion(
                                          "template.completion.html", "template.html", "package.json",
                                          "template.completion.ts"));
  }

  public void testNonInlineTemplateResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("template.html", "package.json", "template.ts");
      PsiElement resolve = resolveReference("myCu<caret>");
      assertEquals("template.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFieldImpl.class);
    });
  }

  public void testNonInlineTemplateUsage2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnusedLocalSymbolsInspection.class, JSUnusedGlobalSymbolsInspection.class);
      myFixture.configureByFiles("template.usage.ts", "template.usage.html", "package.json");
      myFixture.checkHighlighting();
    });
  }

  public void testNonInlineTemplateMethodResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("templateMethod.html", "package.json", "templateMethod.ts", "customer.ts", "customer2.ts");
      PsiElement resolve = resolveReference("ca<caret>ll()");
      assertEquals("customer.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFunction.class);
    });
  }

  public void testNonInlineTemplateDefinitionResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("definition.html", "package.json", "definition.ts", "definition2.ts");
      PsiElement resolve = resolveReference("tit<caret>le");
      assertEquals("definition.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFieldImpl.class);
    });
  }

  public void testInlineTemplateDefinitionResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("definition.ts", "package.json", "definition2.ts");
      PsiElement resolve = resolveReference("tit<caret>le");
      assertEquals("definition.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFieldImpl.class);
    });
  }

  public void testNonInlineTemplatePropertyResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("definition2.html", "package.json", "definition2.ts");
      PsiElement resolve = resolveReference("check<caret>ed");
      assertEquals("definition2.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFieldImpl.class);
    });
  }

  public void testInlineTemplatePropertyResolve2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("definition2.ts", "package.json");
      PsiElement resolve = resolveReference("check<caret>ed");
      assertEquals("definition2.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptFieldImpl.class);
    });
  }

  public void testComponentFieldsFromConstructorResolve() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("template.constr.html", "template.constr.ts", "package.json");
      PsiElement resolve = resolveReference("myCu<caret>stomer");
      assertEquals("template.constr.ts", resolve.getContainingFile().getName());
      assertInstanceOf(resolve, TypeScriptParameterImpl.class);
    });
  }

  public void testInlineComponentFieldsFromConstructorCompletion() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, myFixture.getProject(),
                                        () -> myFixture.testCompletion(
                                          "template.constr.completion.ts", "template.constr.completion.after.ts",
                                          "package.json"));
  }

  public void testInlineTemplateCreateFunction2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunction.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunction.fixed.ts", true);
    });
  }

  public void testInlineTemplateCreateFunctionWithParam2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunctionWithParam.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunctionWithParam.fixed.ts", true);
    });
  }

  public void testInlineTemplateCreateFunctionEventEmitter2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunctionEventEmitter.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunctionEventEmitter.fixed.ts", true);
    });
  }

  public void testInlineTemplateCreateFunctionWithType2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunctionWithType.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunctionWithType.fixed.ts", true);
    });
  }

  public void testInlineTemplateCreateFunctionEventEmitterImplicit2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunctionEventEmitterImplicit.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunctionEventEmitterImplicit.fixed.ts", true);
    });
  }

  public void testInlineTemplateCreateField2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedVariableInspection.class);
      myFixture.getAllQuickFixes("createField.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Field 'todo'"));
      myFixture.checkResultByFile("createField.fixed.ts", true);
    });
  }

  public void testNonInlineTemplateCreateFunction2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunction.html", "createFunction.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunction.ts", "createFunction.fixed.ts", true);
    });
  }

  public void testNonInlineTemplateCreateField2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedVariableInspection.class);
      myFixture.getAllQuickFixes("createField.html", "createField.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Field 'todo'"));
      myFixture.checkResultByFile("createField.ts", "createField.fixed.ts", true);
    });
  }

  public void testNonInlineTemplateCreateFunctionDoubleClass2TypeScript() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSUnresolvedFunctionInspection.class);
      myFixture.getAllQuickFixes("createFunctionDoubleClass.html", "createFunctionDoubleClass.ts", "package.json");
      myFixture.launchAction(myFixture.findSingleIntention("Create Method 'fetchFromApi'"));
      myFixture.checkResultByFile("createFunctionDoubleClass.ts", "createFunctionDoubleClass.fixed.ts", true);
    });
  }

  public void testFixSignatureMismatchFromUsageInTemplate() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.enableInspections(JSCheckFunctionSignaturesInspection.class);
      myFixture.getAllQuickFixes("changeMethodSignature.html", "changeMethodSignature.ts", "package.json");
      String fixTitle = JSBundle.message("change.method.signature.fix.text", "HeroDetailComponent.save()");
      myFixture.launchAction(myFixture.findSingleIntention(fixTitle));
      myFixture.checkResultByFile("changeMethodSignature.ts", "changeMethodSignature.fixed.ts", true);
    });
  }

  public void testOverriddenMethods() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), () -> {
      myFixture.configureByFiles("overriddenMethods.ts", "package.json");
      myFixture.completeBasic();
      assertEquals(newArrayList("$any#*#(arg: *)",
                                "bar#string#()",
                                "bar#string#(test: boolean)",
                                "bar#string#(test: string)",
                                "foo#string# (TodoCmp)"),
                   StreamEx.of(myFixture.getLookupElements()).map(el -> {
                     TestLookupElementPresentation presentation = TestLookupElementPresentation.renderReal(el);
                     return presentation.getItemText() + "#" + presentation.getTypeText() + "#" + presentation.getTailText();
                   }).sorted().toList());
    });
  }
}
