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
package com.jetbrains.lang.dart.coverage;

import com.intellij.coverage.CoverageExecutor;
import com.intellij.coverage.CoverageHelper;
import com.intellij.coverage.CoverageRunnerData;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRunConfiguration;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRunningState;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class DartCoverageProgramRunner extends DefaultProgramRunner {
  private static final Logger LOG = Logger.getInstance(DartCoverageProgramRunner.class.getName());

  private static final String ID = "DartCoverageProgramRunner";
  private static final Set<String> activatedPubs = new HashSet<>();

  @NotNull
  @Override
  public String getRunnerId() {
    return ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return executorId.equals(CoverageExecutor.EXECUTOR_ID) && profile instanceof DartCommandLineRunConfiguration;
  }

  @Override
  public RunnerSettings createConfigurationData(final ConfigurationInfoProvider settingsProvider) {
    return new CoverageRunnerData();
  }

  @Nullable
  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
    FileDocumentManager.getInstance().saveAllDocuments();
    DartCommandLineRunConfiguration runConfiguration = (DartCommandLineRunConfiguration)env.getRunProfile();

    final DartSdk sdk = DartSdk.getDartSdk(runConfiguration.getProject());
    if (sdk == null) {
      throw new ExecutionException(DartBundle.message("dart.sdk.is.not.configured"));
    }

    final String dartPubPath = DartSdkUtil.getPubPath(sdk);

    DartCoverageEnabledConfiguration config = (DartCoverageEnabledConfiguration)CoverageEnabledConfiguration.getOrCreate(runConfiguration);
    String coverageFilePath = config.getCoverageFilePath();

    RunContentDescriptor result = super.doExecute(state, env);
    if (result == null) {
      return null;
    }

    if (!activatedPubs.contains(dartPubPath) && !activateCoverage(dartPubPath, runConfiguration.getProject())) {
      throw new ExecutionException("Cannot activate pub package 'coverage'!");
    }

    GeneralCommandLine cmdline = new GeneralCommandLine().withExePath(dartPubPath)
      .withParameters("global", "run", "coverage:collect_coverage", "-p",
                      Integer.toString(((DartCommandLineRunningState)state).getObservatoryPort()), "-o", coverageFilePath, "-r", "-w");
    ProcessHandler coverageProcess = new OSProcessHandler(cmdline);
    coverageProcess.startNotify();
    config.setCoverageProcess(coverageProcess);

    ProcessHandler resultProcessHandler = result.getProcessHandler();
    if (resultProcessHandler != null) {
      CoverageHelper.attachToProcess(runConfiguration, resultProcessHandler, env.getRunnerSettings());
    }

    return result;
  }

  private static boolean activateCoverage(@NotNull String dartPubPath, @Nullable Project project) {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        ProcessOutput activateOutput =
          new CapturingProcessHandler(new GeneralCommandLine().withExePath(dartPubPath).withParameters("global", "activate", "coverage"))
            .runProcess(60 * 1000);
        ProcessOutput listOutput =
          new CapturingProcessHandler(new GeneralCommandLine().withExePath(dartPubPath).withParameters("global", "list"))
            .runProcess(60 * 1000);
        if (listOutput.getExitCode() == 0 && listOutput.getStdout().contains("coverage ")) {
          activatedPubs.add(dartPubPath);
        }
        else {
          LOG.warn("Activation of coverage package failed.");
          LOG.warn("Activation output (exit code " + activateOutput.getExitCode() + "):\n" + activateOutput.getStdout());
          LOG.warn("List of activated packages (exit code " + listOutput.getExitCode() + "):\n" + listOutput.getStdout());
        }
      }
      catch (ExecutionException e) {
        LOG.warn(e);
      }
    }, "Activating Coverage Package...", false, project);
    return activatedPubs.contains(dartPubPath);
  }
}
