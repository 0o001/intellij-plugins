// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.cli

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.ide.util.projectWizard.WebProjectTemplate
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.ui.AncestorListenerAdapter
import com.intellij.ui.RelativeFont
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.*
import com.intellij.util.PathUtil
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import org.jetbrains.vuejs.VueBundle
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.swing.*
import javax.swing.event.AncestorEvent

class VueCliProjectSettingsStep(projectGenerator: DirectoryProjectGenerator<NpmPackageProjectGenerator.Settings>?,
                                callback: AbstractNewProjectStep.AbstractCallback<NpmPackageProjectGenerator.Settings>?)
  : ProjectSettingsStepBase<NpmPackageProjectGenerator.Settings>(projectGenerator, callback) {
  private var process: VueCreateProjectProcess? = null
  private var mainPanel: JPanel? = null

  override fun createPanel(): JPanel {
    val mainPanel = super.createPanel()
    mainPanel.add(WebProjectTemplate.createTitlePanel(), BorderLayout.NORTH)

    myCreateButton.text = "Next"
    setFirstStepActionListener(mainPanel)

    this.mainPanel = mainPanel
    return mainPanel
  }

  private fun setFirstStepActionListener(mainPanel: JPanel) {
    removeActionListeners()
    myCreateButton.addActionListener(object : ActionListener {
      override fun actionPerformed(e: ActionEvent?) {
        startGeneration(mainPanel)
      }
    })
  }

  private fun ActionListener.startGeneration(mainPanel: JPanel) {
    val generationLocation = projectLocation
    val controller = createVueRunningGeneratorController(generationLocation, peer!!.settings,
                                                         MyVueRunningGeneratorListener(generationLocation),
                                                         this@VueCliProjectSettingsStep)
    if (controller != null) {
      val newPanel = controller.getPanel()
      replacePanel(mainPanel, newPanel)

      newPanel.addAncestorListener(object : AncestorListenerAdapter() {
        override fun ancestorRemoved(event: AncestorEvent) {
          // restore panel; see similar panel restoring in {@link YeomanProjectGeneratorPanel#dispose}
          // we need this code because the main panel is cached by the outside code;
          // it is needed for heavy welcome screen ui, like settings
          controller.stopProcess()
          val settingsPanel = createPanel()
          val scrollPane = (settingsPanel.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER) as JBScrollPane
          replacePanel(mainPanel, scrollPane.viewport.view as JPanel)
          setFirstStepActionListener(mainPanel)
        }
      })

      myCreateButton.removeActionListener(this)
      myCreateButton.addActionListener { controller.onNext() }
    }
    else {
      UIUtil.setEnabled((mainPanel.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER), false, true)
    }
  }

  inner class MyVueRunningGeneratorListener(private val generationLocation: String) : VueRunningGeneratorListener {
    override fun enableNext() {
      myCreateButton.isEnabled = true
    }

    override fun disableNext(validationError: String?) {
      setErrorText(validationError)
      myCreateButton.isEnabled = false
    }

    override fun error(validationError: String?) {
      onError(validationError ?: "")
    }

    override fun cancelCloseUI() {
      DialogWrapper.findInstance(myCreateButton)?.close(DialogWrapper.OK_EXIT_CODE)
    }

    override fun finishedQuestionsCloseUI(callback: (Project) -> Unit) {
      DialogWrapper.findInstance(myCreateButton)?.close(DialogWrapper.OK_EXIT_CODE)
      val function = Runnable {
        val projectVFolder = LocalFileSystem.getInstance().refreshAndFindFileByPath(generationLocation)
        if (projectVFolder == null) {
          VueCreateProjectProcess.LOG.info(
            String.format("Create Vue Project: can not find project directory in '%s'", generationLocation))
        }
        else {
          RecentProjectsManager.getInstance().lastProjectCreationLocation = PathUtil.toSystemIndependentName(
            Paths.get(generationLocation).parent.normalize().toString())
          PlatformProjectOpenProcessor.doOpenProject(projectVFolder, null, -1,
                                                     { project, _ ->
                                                       if (project != null) {
                                                         callback.invoke(project)
                                                       }
                                                     },
                                                     EnumSet.noneOf(PlatformProjectOpenProcessor.Option::class.java))
        }
      }
      ApplicationManager.getApplication().invokeLater(function, ModalityState.NON_MODAL)
    }
  }

  override fun checkValid(): Boolean {
    val text = myLocationField.textField.text.trim()
    if (Files.exists(Paths.get(text))) {
      setErrorText(VueBundle.message("vue.project.generator.project.location.already.exists"))
      return false
    }
    return super.checkValid()
  }

  private fun onError(errorText: String) {
    setErrorText("Error: $errorText")
    myCreateButton.text = "Close"
    myCreateButton.isEnabled = true
    if (process != null) {
      process!!.listener = null
      process!!.cancel()
    }
  }

  private fun replacePanel(mainPanel: JPanel, questioningPanel: JPanel) {
    val scrollPane = (mainPanel.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER) as JBScrollPane
    scrollPane.setViewportView(questioningPanel)
    mainPanel.revalidate()
    mainPanel.repaint()
  }

  private fun removeActionListeners() {
    val actionListeners = myCreateButton.actionListeners
    actionListeners.forEach { myCreateButton.removeActionListener(it) }
  }
}

class VueCliGeneratorQuestioningPanel(private val isOldPackage: Boolean,
                                      private val generatorName: String,
                                      private val validationListener: (Boolean) -> Unit) {
  private var currentPrefferdFocusOwner: JComponent? = null
  private var currentControl: (() -> String)? = null
  private var currentCheckboxControl: (() -> List<String>)? = null
  val panel: JPanel = JPanel(BorderLayout())

  init {
    val wrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    val progressLabel = JBLabel("Starting Vue CLI...")
    progressLabel.font = UIUtil.getLabelFont()
    RelativeFont.ITALIC.install<JLabel>(progressLabel)
    wrapper.add(progressLabel)
    wrapper.add(AsyncProcessIcon(""))
    panel.add(wrapper, BorderLayout.NORTH)
  }

  private fun addInput(message: String, defaultValue: String): () -> String {
    val formBuilder = questionHeader(message)
    val field = JBTextField(defaultValue)
    field.addKeyListener(object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent?) {
        validationListener.invoke(field.text.isNotBlank())
      }
    })
    field.addActionListener { validationListener.invoke(field.text.isNotBlank()) }
    currentPrefferdFocusOwner = field
    formBuilder.addComponent(field)
    panel.add(SwingHelper.wrapWithHorizontalStretch(formBuilder.panel), BorderLayout.CENTER)
    return { field.text }
  }

  private fun questionHeader(message: String): FormBuilder {
    panel.removeAll()
    val formBuilder = FormBuilder.createFormBuilder()
    val progressText = if (isOldPackage) "Running \"vue-init\" with the \"$generatorName\" template"
    else "Running \"vue create\""
    val titleLabel = JLabel(progressText)
    RelativeFont.ITALIC.install<JLabel>(titleLabel)
    formBuilder.addComponent(titleLabel)
    formBuilder.addVerticalGap(5)
    val label = JTextArea(message)
    label.lineWrap = true
    label.isEditable = false
    label.background = UIUtil.getLabelBackground()
    label.font = UIUtil.getLabelFont()
    label.wrapStyleWord = true
    formBuilder.addComponentFillVertically(label, 0)
    return formBuilder
  }

  private fun addChoices(message: String, choices: List<VueCreateProjectProcess.Choice>): () -> String {
    val formBuilder = questionHeader(message)
    val box = ComboBox<VueCreateProjectProcess.Choice>(choices.toTypedArray())
    box.renderer = SimpleListCellRenderer.create("", VueCreateProjectProcess.Choice::name)
    box.isEditable = false
    currentPrefferdFocusOwner = box
    formBuilder.addComponent(box)
    panel.add(SwingHelper.wrapWithHorizontalStretch(formBuilder.panel), BorderLayout.CENTER)
    return { (box.selectedItem as? VueCreateProjectProcess.Choice)?.value ?: "" }
  }

  private fun addCheckboxes(message: String, choices: List<VueCreateProjectProcess.Choice>): () -> List<String> {
    val formBuilder = questionHeader(message)
    val selectors = mutableListOf<(MutableList<String>) -> Unit>()
    currentPrefferdFocusOwner = null
    choices.forEach {
      val box = JBCheckBox(it.name)
      if (currentPrefferdFocusOwner == null) currentPrefferdFocusOwner = box
      formBuilder.addComponent(box)
      selectors.add { list -> if (box.isSelected) list.add(it.value) }
    }
    panel.add(SwingHelper.wrapWithHorizontalStretch(formBuilder.panel), BorderLayout.CENTER)
    return {
      val list = mutableListOf<String>()
      selectors.forEach { it.invoke(list) }
      list
    }
  }

  private fun addConfirm(message: String): () -> String {
    val formBuilder = questionHeader(message)

    val yesBtn = JBRadioButton("Yes")
    val noBtn = JBRadioButton("No")
    val buttonGroup = ButtonGroup()
    buttonGroup.add(yesBtn)
    buttonGroup.add(noBtn)
    yesBtn.isSelected = true
    noBtn.isSelected = false

    currentPrefferdFocusOwner = yesBtn
    formBuilder.addComponent(yesBtn)
    formBuilder.addComponent(noBtn)
    panel.add(SwingHelper.wrapWithHorizontalStretch(formBuilder.panel), BorderLayout.CENTER)
    return { if (yesBtn.isSelected) "Yes" else "no" }
  }

  fun error() {
    panel.removeAll()
    panel.add(SwingHelper.wrapWithHorizontalStretch(JBLabel("Vue CLI error")),
              BorderLayout.CENTER)
    panel.revalidate()
    panel.repaint()
  }

  fun question(question: VueCreateProjectProcess.Question) {
    when (question.type) {
      VueCreateProjectProcess.QuestionType.Input -> currentControl = addInput(question.message, question.defaultVal)
      VueCreateProjectProcess.QuestionType.Confirm -> currentControl = addConfirm(question.message)
      VueCreateProjectProcess.QuestionType.List -> currentControl = addChoices(question.message, question.choices)
      VueCreateProjectProcess.QuestionType.Checkbox -> currentCheckboxControl = addCheckboxes(question.message, question.choices)
    }
    if (currentPrefferdFocusOwner != null) currentPrefferdFocusOwner!!.requestFocus()
    panel.revalidate()
    panel.repaint()
  }

  fun getCheckboxAnswer(): List<String>? {
    return currentCheckboxControl?.invoke()
  }

  fun getAnswer(): String? {
    return currentControl?.invoke()
  }

  fun activateUi() {
    UIUtil.setEnabled(panel, true, true)
    if (currentPrefferdFocusOwner != null) currentPrefferdFocusOwner!!.requestFocus()
  }

  fun waitForNextQuestion() {
    UIUtil.setEnabled(panel, false, true)
  }
}
