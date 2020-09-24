// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package training.learn.lesson.general

import training.lang.JavaLangSupport
import training.learn.LessonsBundle
import training.learn.interfaces.Module
import training.learn.lesson.kimpl.KLesson
import training.learn.lesson.kimpl.LessonContext
import training.learn.lesson.kimpl.LessonSample

class MoveLesson(module: Module, lang: String, private val sample: LessonSample)
  : KLesson("Move", LessonsBundle.message("move.lesson.name"), module, lang) {
  override val lessonContent: LessonContext.() -> Unit
    get() = {
      prepareSample(sample)

      actionTask("MoveLineDown") {
        LessonsBundle.message("move.pull.down", action(it))
      }
      actionTask("MoveLineUp") {
        LessonsBundle.message("move.pull.up", action(it))
      }
      if (lang == JavaLangSupport.lang) caret(9, 5)
      task("MoveStatementUp") {
        text(LessonsBundle.message("move.whole.method.up", action(it)))
        trigger(it, { editor.document.text }, { before, now ->
          checkSwapMoreThan2Lines(before, now)
        })
        test { actions("EditorUp", it) }
      }
      actionTask("MoveStatementDown") {
        LessonsBundle.message("move.whole.method.down", action(it))
      }
    }
}

fun checkSwapMoreThan2Lines(before: String, now: String): Boolean {
  val beforeLines = before.split("\n")
  val nowLines = now.split("\n")

  if (beforeLines.size != nowLines.size) {
    return false
  }
  val chane = beforeLines.size - commonPrefix(beforeLines, nowLines) - commonSuffix(beforeLines, nowLines)
  return chane >= 4
}

private fun <T> commonPrefix(list1: List<T>, list2: List<T>): Int {
  val size = Integer.min(list1.size, list2.size)
  for (i in 0 until size) {
    if (list1[i] != list2[i])
      return i
  }
  return size
}

private fun <T> commonSuffix(list1: List<T>, list2: List<T>): Int {
  val size = Integer.min(list1.size, list2.size)
  for (i in 0 until size) {
    if (list1[list1.size - i - 1] != list2[list2.size - i - 1])
      return i
  }
  return size
}
