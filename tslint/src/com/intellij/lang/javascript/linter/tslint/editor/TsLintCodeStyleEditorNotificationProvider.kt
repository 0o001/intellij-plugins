package com.intellij.lang.javascript.linter.tslint.editor

import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.javascript.JavaScriptSupportLoader
import com.intellij.lang.javascript.linter.tslint.TsLintBundle
import com.intellij.lang.javascript.linter.tslint.config.style.rules.TsLintConfigWrapper
import com.intellij.lang.javascript.linter.tslint.config.style.rules.TslintRulesSet
import com.intellij.lang.javascript.linter.tslint.ide.TsLintConfigFileType
import com.intellij.lang.typescript.formatter.TypeScriptCodeStyleSettings
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.util.*
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications

private val KEY = Key.create<EditorNotificationPanel>("TsLint.Import.Code.Style.Notification")
private val DISMISS_KEY = "tslint.code.style.apply.dismiss"
val RULES_TO_APPLY: ParameterizedCachedValueProvider<TsLintConfigWrapper, PsiFile> = ParameterizedCachedValueProvider {
  if (it == null || PsiTreeUtil.hasErrorElements(it)) {
    return@ParameterizedCachedValueProvider CachedValueProvider.Result.create(null, it)
  }

  val jsonElement = JsonParser().parse(it.text)
  val result = (if (jsonElement.isJsonObject) TsLintConfigWrapper(jsonElement.asJsonObject) else null)

  return@ParameterizedCachedValueProvider CachedValueProvider.Result.create(result, it)
}

val RULES_CACHE_KEY = Key.create<ParameterizedCachedValue<TsLintConfigWrapper, PsiFile>>("tslint.cache.key.config.json")

class TsLintCodeStyleEditorNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

  override fun getKey(): Key<EditorNotificationPanel> = KEY


  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
    if (fileEditor !is TextEditor ||
        fileEditor.editor !is EditorEx ||
        file.fileType != TsLintConfigFileType.INSTANCE) return null


    val project = fileEditor.editor.project ?: return null

    if (PropertiesComponent.getInstance(project).getBoolean(DISMISS_KEY)) {
      return null
    }

    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null


    val wrapper = CachedValuesManager.getManager(project)
                    .getParameterizedCachedValue(psiFile, RULES_CACHE_KEY, RULES_TO_APPLY, false, psiFile) ?: return null

    val settings = CodeStyleSettingsManager.getInstance(project).currentSettings
    val languageSettings = settings.getCommonSettings(JavaScriptSupportLoader.TYPESCRIPT)
    val jsCodeStyleSettings = settings.getCustomSettings(TypeScriptCodeStyleSettings::class.java)

    if (languageSettings == null || jsCodeStyleSettings == null) {
      return null
    }

    val rules = TslintRulesSet.filter { it.isAvailable(project, languageSettings, jsCodeStyleSettings, wrapper) }

    if (rules.isEmpty()) return null

    return object : EditorNotificationPanel(EditorColors.GUTTER_BACKGROUND) {
      init {
        setText(TsLintBundle.message("tslint.code.style.apply.message"))
        val okAction: Runnable = Runnable {
          WriteAction.run<RuntimeException> {
            rules.forEach { rule -> rule.apply(project, languageSettings, jsCodeStyleSettings, wrapper) }
          }
          EditorNotifications.getInstance(project).updateAllNotifications()
        }
        createActionLabel(TsLintBundle.message("tslint.code.style.apply.text"), okAction)

        val dismissAction: Runnable = Runnable {
          PropertiesComponent.getInstance(project).setValue(DISMISS_KEY, true)
          EditorNotifications.getInstance(project).updateAllNotifications()
        }
        createActionLabel(TsLintBundle.message("tslint.code.style.dismiss.text"), dismissAction)
      }
    }
  }


}