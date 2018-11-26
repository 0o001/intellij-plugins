package com.intellij.lang.javascript.linter.tslint;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TsLintCodeStyleImportBasicTest.class,
  TsLintCodeStyleImportIntegrationTest.class,
  TsLintHighlightingTest.class,
  TsLintConfigCompletionTest.class,
  TsLintConfigHighlightingTest.class,
  TsLintResolveTest.class
})
public class TsLintTestSuite {
}