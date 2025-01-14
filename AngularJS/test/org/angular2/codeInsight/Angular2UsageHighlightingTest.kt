// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.angular2.codeInsight

import org.angular2.Angular2TestCase

class Angular2UsageHighlightingTest : Angular2TestCase("usageHighlighting") {

  fun testPrivateComponentField() = checkUsageHighlighting()

  fun testPublicComponentField() = checkUsageHighlighting()

  fun testDirectiveInputFromField() = checkUsageHighlighting()

  fun testDirectiveInputFromMapping() = checkUsageHighlighting()

  fun testDirectiveInputFromTemplate() = checkUsageHighlighting()

  fun testDirectiveSelectorFromDefinition() = checkUsageHighlighting()

  fun testDirectiveSelectorFromUsage() = checkUsageHighlighting()

  fun testDirectiveExportAsFromDefinition() = checkUsageHighlighting()

  fun testDirectiveExportAsFromUsage() = checkUsageHighlighting()

}