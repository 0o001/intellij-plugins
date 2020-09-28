package training.learn.lesson.swift.navigation

import training.learn.LessonsBundle
import training.learn.interfaces.Module
import training.learn.lesson.kimpl.*

class SwiftCodeNavigationLesson(module: Module) : KLesson("swift.navigation.code", LessonsBundle.message("swift.navigation.code.name"), module, "Swift") {

  private val sample: LessonSample = parseLessonSample("""
import Foundation

class Feature {
    var name = ""
}

protocol IDEProtocol {
    func navigation() -> Feature
    func assistance() -> Feature
    func generation() -> Feature
}

class JetBrainsIDE: IDEProtocol {
    func navigation() -> Feature {
        return Feature()
    }

    func assistance() -> Feature {
        return Feature()
    }

    func generation() -> Feature {
        return Feature()
    }
}

class AppCode: JetBrainsIDE {}""".trimIndent())
  override val lessonContent: LessonContext.() -> Unit = {
    prepareSample(sample)

    task {
      text(LessonsBundle.message("swift.navigation.code.intro"))
    }
    task {
      triggers("GotoClass", "DetailViewController.swift")
      text(LessonsBundle.message("swift.navigation.code.class", code("DetailViewController"), action("GotoClass"), code("dvc"), LessonUtil.rawEnter()))
    }
    task {
      text(LessonsBundle.message("swift.navigation.code.fuzzy"))
    }
    task {
      triggers("GotoFile", "AppDelegate.swift")
      text(LessonsBundle.message("swift.navigation.code.file", code("AppDelegate.swift"), action("GotoFile"), code("ad"), LessonUtil.rawEnter()))
    }
    task {
      triggers("GotoSymbol", "MasterViewController.swift")
      text(LessonsBundle.message("swift.navigation.code.symbol", code("detailViewController"), code("MasterViewController"), action("GotoSymbol"), code("dvc"), LessonUtil.rawEnter()))
    }
    task {
      text(LessonsBundle.message("swift.navigation.code.non.project.files"))
    }
    task { caret(5, 20) }
    task {
      triggers("GotoDeclaration", "DetailViewController.swift")
      text(LessonsBundle.message("swift.navigation.code.declaration", code("DetailViewController?"), action("GotoDeclaration")))
    }
    task { caret(3, 33) }
    task {
      triggers("GotoImplementation")
      text(LessonsBundle.message("swift.navigation.code.implementation", action("GotoDeclaration"), action("GotoImplementation"), code("UIViewController")))
    }
    task {
      triggers("GotoFile", "Navigation.swift")
      text(LessonsBundle.message("swift.navigation.code.go.back", code("Navigation.swift"), action("GotoFile")))
    }
    task { caret(27, 10) }
    task {
      triggers("GotoSuperMethod")
      text(LessonsBundle.message("swift.navigation.code.super", action("GotoSuperMethod"), code("JetBrainsIDE")))
    }
    task {
      triggers("GotoSuperMethod")
      text(LessonsBundle.message("swift.navigation.code.super.again", action("GotoSuperMethod"), code("IDEProtocol")))
    }
    task {
      triggers("RecentFiles")
      text(LessonsBundle.message("swift.navigation.code.recent", action("RecentFiles")))
    }
    task {
      triggers("Switcher")
      text(LessonsBundle.message("swift.navigation.code.switcher", action("Switcher")))
    }
  }
}