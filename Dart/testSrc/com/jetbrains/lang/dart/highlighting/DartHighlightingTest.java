package com.jetbrains.lang.dart.highlighting;

import com.intellij.codeInsight.daemon.impl.analysis.HtmlUnknownTargetInspection;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.jetbrains.lang.dart.DartCodeInsightFixtureTestCase;
import com.jetbrains.lang.dart.ide.inspections.DartDeprecatedApiUsageInspection;

public class DartHighlightingTest extends DartCodeInsightFixtureTestCase {
  protected String getBasePath() {
    return "/highlighting";
  }

  protected boolean isWriteActionRequired() {
    return false;
  }

  public void testScriptSrcPathToPackagesFolder() {
    final String testName = getTestName(false);
    myFixture.enableInspections(HtmlUnknownTargetInspection.class);
    myFixture.addFileToProject("packages/browser/dart.js", "");
    myFixture.addFileToProject("pubspec.yaml", "");
    myFixture.configureByFile(testName + "/" + testName + ".html");
    myFixture.checkHighlighting(true, false, true);
  }

  public void testSpelling() {
    myFixture.enableInspections(SpellCheckingInspection.class);
    myFixture.configureByFile(getTestName(false) + ".dart");
    myFixture.checkHighlighting(true, false, true);
  }

  public void testEscapeSequences() {
    myFixture.configureByFile(getTestName(false) + ".dart");
    myFixture.checkHighlighting(true, true, true);
  }

  public void testBuiltInIdentifiers() {
    myFixture.configureByFile(getTestName(false) + ".dart");
    myFixture.checkHighlighting(false, true, true);
  }

  public void testDeprecatedApiUsageInspection() {
    myFixture.enableInspections(DartDeprecatedApiUsageInspection.class);
    myFixture.configureByFile(getTestName(false) + ".dart");
    myFixture.checkHighlighting(true, false, true);
  }

  public void testUnusedImports() {
    myFixture.configureByFile(getTestName(false) + ".dart");
    myFixture.checkHighlighting(true, false, true);
  }

  public void testColorAnnotator() {
    myFixture.configureByFile(getTestName(false) + ".dart");
    myFixture.checkHighlighting(true, true, true);
  }
}
