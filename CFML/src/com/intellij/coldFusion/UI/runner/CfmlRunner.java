/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.coldFusion.UI.runner;

import com.intellij.coldFusion.CfmlBundle;
import com.intellij.coldFusion.mxunit.CfmlUnitRunConfiguration;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class CfmlRunner extends DefaultProgramRunner {
  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
    final RunProfile runProfileRaw = env.getRunProfile();
    if (runProfileRaw instanceof CfmlRunConfiguration) {
      FileDocumentManager.getInstance().saveAllDocuments();
      final CfmlRunConfiguration runProfile = (CfmlRunConfiguration)runProfileRaw;

      //check if CfmlRunConfiguration generated from default server http://localhost:8500/
      if (runProfile.isFromDefaultHost()) {
        showDefaultRunConfigWarn(state, env, runProfile);
      } else {
        final CfmlRunnerParameters params = runProfile.getRunnerParameters();
        BrowserLauncher.getInstance().browse(params.getUrl(), params.getCustomBrowser(), env.getProject());
      }
      return null;
    }
    else {
      return super.doExecute(state, env);
    }
  }

  @Override
  @NotNull
  public String getRunnerId() {
    return "CfmlRunner";
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) &&
           (profile instanceof CfmlRunConfiguration || profile instanceof CfmlUnitRunConfiguration);
  }

  private static void showDefaultRunConfigWarn(@NotNull RunProfileState state,
                                               @NotNull ExecutionEnvironment env,
                                               CfmlRunConfiguration runProfile) {
    DialogBuilder db = new DialogBuilder(env.getProject());
    JLabel info = new JLabel(CfmlBundle.message("cfml.runconfig.dialog.template.label"));
    info.setMaximumSize(new Dimension(400, 500));
    JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.LINE_AXIS));
    JLabel webPathLabel = new JLabel(CfmlBundle.message("cfml.runconfig.editor.server.url"));
    JTextField webPathField = new JTextField(runProfile.getRunnerParameters().getUrl());
    centerPanel.add(webPathLabel);
    centerPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    centerPanel.add(webPathField);

    info.setIcon(UIUtil.getWarningIcon());
    db.setNorthPanel(info);
    db.setCenterPanel(centerPanel);
    db.setPreferredFocusComponent(info);
    db.setTitle(CfmlBundle.message("cfml.runconfig.dialog.template.title"));
    db.addOkAction().setText(CfmlBundle.message("cfml.runconfig.dialog.template.button.run"));
    db.addCancelAction().setText(CfmlBundle.message("cfml.runconfig.dialog.template.button.cancel"));
    db.setOkOperation(new Runnable() {
      @Override
      public void run() {
        runProfile.setFromDefaultHost(false);
        final CfmlRunnerParameters params = runProfile.getRunnerParameters();
        RunnerAndConfigurationSettings configurationTemplate =
          RunManager.getInstance(env.getProject()).getConfigurationTemplate(runProfile.getFactory());
        ((CfmlRunConfiguration)configurationTemplate.getConfiguration()).getRunnerParameters().setUrl(webPathField.getText());
        BrowserLauncher.getInstance().browse(params.getUrl(), params.getCustomBrowser(), env.getProject());
        db.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
      }
    });
    db.show();
  }

}