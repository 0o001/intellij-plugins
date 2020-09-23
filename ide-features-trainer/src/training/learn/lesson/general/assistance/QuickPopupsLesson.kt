// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package training.learn.lesson.general.assistance

import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.QuickDocUtil
import com.intellij.codeInsight.hint.ImplementationViewComponent
import com.intellij.testGuiFramework.framework.GuiTestUtil
import com.intellij.testGuiFramework.util.Key
import training.commands.kotlin.TaskRuntimeContext
import training.learn.LessonsBundle
import training.learn.interfaces.Module
import training.learn.lesson.kimpl.KLesson
import training.learn.lesson.kimpl.LessonContext
import training.learn.lesson.kimpl.LessonSample

class QuickPopupsLesson(module: Module, lang: String, private val sample: LessonSample) :
  KLesson("Quick Popups", LessonsBundle.message("quick.popups.lesson.name"), module, lang) {

  override val lessonContent: LessonContext.() -> Unit = {
    prepareSample(sample)

    task("QuickJavaDoc") {
      text(LessonsBundle.message("quick.popups.show.documentation", action(it)))
      triggerByUiComponentAndHighlight(highlightBorder = false, highlightInside = false) { _: DocumentationComponent -> true }
      test { actions(it) }
    }

    task {
      text(LessonsBundle.message("quick.popups.press.escape", action("EditorEscape")))
      stateCheck { checkDocComponentClosed() }
      test {
        GuiTestUtil.shortcut(Key.ESCAPE)
      }
    }

    task("QuickImplementations") {
      text(LessonsBundle.message("quick.popups.show.implementation", action(it)))
      triggerByUiComponentAndHighlight(highlightBorder = false, highlightInside = false) { _: ImplementationViewComponent -> true }
      test { actions(it) }
    }
  }

  fun TaskRuntimeContext.checkDocComponentClosed(): Boolean {
    val activeDocComponent = QuickDocUtil.getActiveDocComponent(project)
    return activeDocComponent == null || !activeDocComponent.isShowing
  }
}