package com.intellij.lang.javascript.flex.build;

import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.flex.FlexCommonUtils;
import com.intellij.flex.build.FlexBuildTargetType;
import com.intellij.lang.javascript.flex.flexunit.FlexUnitRunConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.lang.javascript.flex.run.FlashRunConfiguration;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerFilter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineProtoUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;

public class FlexBuildTargetScopeProvider extends BuildTargetScopeProvider {

  private static final Logger LOG = Logger.getInstance(FlexBuildTargetScopeProvider.class.getName());

  @NotNull
  public List<TargetTypeBuildScope> getBuildTargetScopes(@NotNull final CompileScope baseScope,
                                                         @NotNull final CompilerFilter filter,
                                                         @NotNull final Project project,
                                                         boolean forceBuild) {
    final FlexCompiler flexCompiler = FlexCompiler.getInstance(project);
    if (!filter.acceptCompiler(flexCompiler)) return Collections.emptyList();

    final RunConfiguration runConfiguration = CompileStepBeforeRun.getRunConfiguration(baseScope);
    final Collection<Pair<Module, FlexBuildConfiguration>> bcsToCompileForPackaging =
      FlexCompiler.getBCsToCompileForPackaging(baseScope);

    List<String> targetIds = new ArrayList<String>();
    try {
      for (Pair<Module, FlexBuildConfiguration> moduleAndBC : FlexCompiler.getModulesAndBCsToCompile(baseScope)) {
        final Module module = moduleAndBC.first;
        final FlexBuildConfiguration bc = moduleAndBC.second;

        if (bcsToCompileForPackaging != null && contains(bcsToCompileForPackaging, module, bc)) {
          final boolean forcedDebugStatus = FlexCompiler.getForcedDebugStatus(project, bc);
          targetIds.add(FlexCommonUtils.getBuildTargetId(module.getName(), bc.getName(), forcedDebugStatus));
        }
        else if (bc.isTempBCForCompilation()) {
          LOG.assertTrue(runConfiguration instanceof FlashRunConfiguration || runConfiguration instanceof FlexUnitRunConfiguration,
                         bc.getName());
          final BCBasedRunnerParameters params = runConfiguration instanceof FlashRunConfiguration
                                                 ? ((FlashRunConfiguration)runConfiguration).getRunnerParameters()
                                                 : ((FlexUnitRunConfiguration)runConfiguration).getRunnerParameters();
          LOG.assertTrue(params.getModuleName().equals(module.getName()),
                         "Module name in run config: " + params.getModuleName() + ", expected: " + module.getName());
          LOG.assertTrue(params.getBCName().equals(bc.getName()),
                         "BC name in run config: " + params.getBCName() + ", expected: " + bc.getName());
          targetIds.add(FlexCommonUtils.getBuildTargetIdForRunConfig(runConfiguration.getType().getId(), runConfiguration.getName()));
        }
        else {
          targetIds.add(FlexCommonUtils.getBuildTargetId(module.getName(), bc.getName(), null));
        }
      }
    }
    catch (ConfigurationException e) {
      // can't happen because checked in ValidateFlashConfigurationsPrecompileTask
      LOG.error(e);
    }

    if (targetIds.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(CmdlineProtoUtil.createTargetsScope(FlexBuildTargetType.INSTANCE.getTypeId(), targetIds, forceBuild));
  }

  private static boolean contains(final Collection<Pair<Module, FlexBuildConfiguration>> bcs,
                                  final Module module,
                                  final FlexBuildConfiguration bc) {
    for (Pair<Module, FlexBuildConfiguration> pair : bcs) {
      if (pair.first.getName().equals(module.getName()) && pair.second.getName().equals(bc.getName())) {
        return true;
      }
    }
    return false;
  }
}
