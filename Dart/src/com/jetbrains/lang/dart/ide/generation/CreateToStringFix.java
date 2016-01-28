/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.lang.dart.ide.generation;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.jetbrains.lang.dart.DartTokenTypes;
import com.jetbrains.lang.dart.psi.DartClass;
import com.jetbrains.lang.dart.psi.DartComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;

public class CreateToStringFix extends BaseCreateMethodsFix<DartComponent> {

  public CreateToStringFix(final DartClass dartClass) {
    super(dartClass);
  }

  @Override
  protected void processElements(@NotNull Project project, @NotNull Editor editor, Set<DartComponent> elementsToProcess) {
    final TemplateManager templateManager = TemplateManager.getInstance(project);
    anchor = doAddMethodsForOne(editor, templateManager, buildFunctionsText(templateManager, elementsToProcess), anchor);
  }

  @Override
  @NotNull
  protected String getNothingFoundMessage() {
    return ""; // can't be called actually because processElements() is overridden
  }

  protected Template buildFunctionsText(TemplateManager templateManager, Set<DartComponent> elementsToProcess) {
    final Template template = templateManager.createTemplate(getClass().getName(), DART_TEMPLATE_GROUP);
    template.setToReformat(true);

    if (CodeStyleSettingsManager.getSettings(myDartClass.getProject()).INSERT_OVERRIDE_ANNOTATION) {
      template.addTextSegment("@override\n");
    }
    template.addTextSegment("String toString() {");
    template.addTextSegment(DartTokenTypes.RETURN.toString());
    template.addTextSegment(" ");

    template.addTextSegment("'");
    //noinspection ConstantConditions
    template.addTextSegment(myDartClass.getName());
    template.addTextSegment("{");
    for (Iterator<DartComponent> iterator = elementsToProcess.iterator(); iterator.hasNext(); ) {
      DartComponent component = iterator.next();
      //noinspection ConstantConditions
      template.addTextSegment(component.getName());
      template.addTextSegment(": $");
      //noinspection ConstantConditions
      template.addTextSegment(component.getName());
      if (iterator.hasNext()) {
        template.addTextSegment(", ");
      }
    }
    template.addTextSegment("}';\n");
    template.addTextSegment("}");
    template.addEndVariable();
    template.addTextSegment("\n");
    return template;
  }

  @Override
  protected Template buildFunctionsText(TemplateManager templateManager, DartComponent e) {
    // ignore
    return null;
  }
}
