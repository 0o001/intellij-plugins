package org.angularjs.codeInsight;

import com.intellij.codeInsight.daemon.impl.analysis.XmlUnboundNsPrefixInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownAttributeInspection;
import com.intellij.codeInspection.htmlInspections.RequiredAttributesInspectionBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.angularjs.AngularTestUtil;

/**
 * @author Dennis.Ushakov
 */
public class AttributesTest extends LightPlatformCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return AngularTestUtil.getBaseTestDataPath(getClass()) + "attributes";
  }

  @Override
  protected boolean isWriteActionRequired() {
    return getTestName(true).contains("Completion");
  }

  private static String getDirectiveDefinitionText(PsiElement resolve) {
    return resolve.getParent().getText();
  }

  public void testStandardAttributesCompletion() {
    myFixture.testCompletion("standard.html", "standard.after.html", "angular.js");
  }

  public void testNgInclude() {
    myFixture.testCompletion("ng-include.html", "ng-include.after.html", "angular.js");
  }

  public void testStandardAttributesResolve() {
    myFixture.configureByFiles("standard.after.html", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("ng-mo<caret>del", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("angular.js", resolve.getContainingFile().getName());
  }

  public void testStandardAttributesResolveOldStyle() {
    myFixture.configureByFiles("standard.after.html", "angular12.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("ng-mo<caret>del", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("angular12.js", resolve.getContainingFile().getName());
  }

  public void testStandardAttributesDataResolve() {
    myFixture.configureByFiles("standard-data.after.html", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("ng-mo<caret>del", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("angular.js", resolve.getContainingFile().getName());
  }

  public void testCustomAttributesInDirectiveCompletion() {
    myFixture.testCompletion("customInDirective.html", "customInDirective.after.html", "custom.js", "angular.js");
  }

  public void testCustomAttributesInDirectiveResolve() {
    myFixture.configureByFiles("customInDirective.after.html", "custom.js", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("my-cus<caret>tomer", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("custom.js", resolve.getContainingFile().getName());
    assertEquals("'myCustomer'", getDirectiveDefinitionText(resolve));
  }

  public void testCustomAttributesInDirectiveEmptyCompletion() {
    myFixture.testCompletion("customInDirectiveEmpty.html", "customInDirectiveEmpty.after.html", "custom.js", "angular.js");
  }

  public void testCustomAttributesInDirectiveEmptyResolve() {
    myFixture.configureByFiles("customInDirectiveEmpty.after.html", "custom.js", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("my-cus<caret>tomer", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("custom.js", resolve.getContainingFile().getName());
    assertEquals("'myCustomer'", getDirectiveDefinitionText(resolve));
  }

  public void testCustomAttributesCompletion() {
    myFixture.testCompletion("custom.html", "custom.after.html", "custom.js");
  }

  public void testCustomAttributesResolve() {
    myFixture.configureByFiles("custom.after.html", "custom.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("my-cus<caret>tomer", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("custom.js", resolve.getContainingFile().getName());
    assertEquals("'myCustomer'", getDirectiveDefinitionText(resolve));
  }

  public void testControllerCompletion() {
    myFixture.testCompletionTyping("controller.html", "\n", "controller.after.html", "custom.js", "angular.js");
  }

  public void testControllerResolve() {
    myFixture.configureByFiles("controller.resolve.html", "custom.js", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("Supa<caret>Controller", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("custom.js", resolve.getContainingFile().getName());
    assertEquals("'SupaController'", getDirectiveDefinitionText(resolve));
  }

  public void testPrefixedControllerResolve() {
    myFixture.configureByFiles("controller.prefixed.html", "custom.js", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("Supa<caret>Controller", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("custom.js", resolve.getContainingFile().getName());
    assertEquals("'SupaController'", getDirectiveDefinitionText(resolve));
  }

  public void testAppCompletion() {
    myFixture.testCompletion("app.html", "app.after.html", "custom.js", "angular.js");
  }

  public void testAppResolve() {
    myFixture.configureByFiles("app.after.html", "custom.js", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("docs<caret>SimpleDirective", myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertNotNull(resolve);
    assertEquals("custom.js", resolve.getContainingFile().getName());
    assertEquals("'docsSimpleDirective'", getDirectiveDefinitionText(resolve));
  }

  public void testNormalization() {
    myFixture.configureByFiles("normalize.html", "angular.js");
    myFixture.enableInspections(HtmlUnknownAttributeInspection.class);
    myFixture.enableInspections(XmlUnboundNsPrefixInspection.class);
    myFixture.checkHighlighting();
  }

  public void testNgSrc() {
    myFixture.configureByFiles("ng-src.html", "angular.js");
    myFixture.enableInspections(RequiredAttributesInspectionBase.class);
    myFixture.enableInspections(HtmlUnknownAttributeInspection.class);
    myFixture.checkHighlighting();
  }

  public void testNgSrcCompletion() {
    myFixture.configureByFiles("ng-src.completion.html", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("img ng-<caret>", myFixture.getFile());
    myFixture.getEditor().getCaretModel().moveToOffset(offsetBySignature);
    myFixture.completeBasic();
    assertContainsElements(myFixture.getLookupElementStrings(), "ng-src");

    offsetBySignature = AngularTestUtil.findOffsetBySignature("div ng-<caret>", myFixture.getFile());
    myFixture.getEditor().getCaretModel().moveToOffset(offsetBySignature);
    myFixture.completeBasic();
    assertDoesntContain(myFixture.getLookupElementStrings(), "ng-src");
  }

  public void testRestrictE() {
    myFixture.configureByFiles("form.html", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("div f<caret>", myFixture.getFile());
    myFixture.getEditor().getCaretModel().moveToOffset(offsetBySignature);
    myFixture.completeBasic();
    assertDoesntContain(myFixture.getLookupElementStrings(), "form");
  }

  public void testIncorrectJSDoc() {
    myFixture.configureByText(System.currentTimeMillis() + ".js",
                              "/**\n" +
                              " * @ngdoc directive\n" +
                              " * @name yaSelect\n" +
                              " * @restrict E\n" +
                              " *\n" +
                              " * @param description\n" +
                              " *\n" +
                              " * @description Р’С‹РІРѕРґРёС‚ select\n" +
                              " *\n" +
                              " * @param ngModel Assignable angular expression to data-bind to. sa\n" +
                              " * bla bla bla l\n" +
                              " */");
    myFixture.doHighlighting();
  }

  public void testInlineStyle() {
    myFixture.configureByFiles("style.html", "angular.js");
    myFixture.checkHighlighting();
  }

  public void testElement() {
    myFixture.configureByFiles("ng-copy.html", "angular.js");
    for (String signature : new String[]{"input", "select", "textarea", "a"}) {
      int offsetBySignature = AngularTestUtil.findOffsetBySignature("<" + signature + " ng-<caret>", myFixture.getFile());
      myFixture.getEditor().getCaretModel().moveToOffset(offsetBySignature);
      myFixture.completeBasic();
      assertContainsElements(myFixture.getLookupElementStrings(), "ng-copy");
    }
  }

  public void testRepeatCompletion() {
    myFixture.configureByFiles("ng-repeat.html", "angular.js");
    int offsetBySignature = AngularTestUtil.findOffsetBySignature("<div ng-rep<caret>", myFixture.getFile());
    myFixture.getEditor().getCaretModel().moveToOffset(offsetBySignature);
    myFixture.completeBasic();
    assertContainsElements(myFixture.getLookupElementStrings(), "ng-repeat", "ng-repeat-start", "ng-repeat-end");
  }

  public void testRepeatResolve() {
    myFixture.configureByFiles("ng-repeat.resolve.html", "angular.js");
    for (String suffix : new String[]{"", "-start", "-end"}) {
      int offsetBySignature = AngularTestUtil.findOffsetBySignature("ng<caret>-repeat" + suffix, myFixture.getFile());
      PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
      assertNotNull(ref);
      PsiElement resolve = ref.resolve();
      assertNotNull(resolve);
      assertEquals("angular.js", resolve.getContainingFile().getName());
    }
  }
}
