// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie.ide

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.grazie.GrazieBundle
import com.intellij.grazie.GrazieConfig
import com.intellij.grazie.ide.msg.GrazieStateLifecycle
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.vcs.commit.message.BaseCommitMessageInspection
import com.intellij.vcs.commit.message.CommitMessageInspectionProfile

class GrazieCommitInspection : BaseCommitMessageInspection() {
  companion object : GrazieStateLifecycle, StartupActivity.Background {
    private val grazie: LocalInspectionTool by lazy { GrazieInspection() }

    override fun update(prevState: GrazieConfig.State, newState: GrazieConfig.State) {
      if (prevState.enabledCommitIntegration == newState.enabledCommitIntegration) return
      ProjectManager.getInstance().openProjects.forEach { project ->
        updateInspectionState(project, newState)
      }
    }

    override fun runActivity(project: Project) = updateInspectionState(project)

    private fun updateInspectionState(project: Project, state: GrazieConfig.State = GrazieConfig.get()) {
      with(CommitMessageInspectionProfile.getInstance(project)) {
        if (state.enabledCommitIntegration) {
          addTool(project, LocalInspectionToolWrapper(GrazieCommitInspection()), emptyMap())
          setToolEnabled("GrazieCommit", true, project)
        }
        else {
          if (getToolsOrNull("GrazieCommit", project) != null) setToolEnabled("GrazieCommit", false, project)
          //TODO-tanvd how to remove tool?
        }
      }
    }
  }

  override fun getShortName() = "GrazieCommit"

  override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevel.find("TYPO") ?: HighlightDisplayLevel.WARNING

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = grazie.buildVisitor(holder, isOnTheFly)

  override fun getDisplayName() = GrazieBundle.message("grazie.inspection.commit.text")
}
