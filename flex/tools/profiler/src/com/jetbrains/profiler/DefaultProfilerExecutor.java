package com.jetbrains.profiler;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DefaultProfilerExecutor extends Executor {
  @NonNls
  public static final String EXECUTOR_ID = "Profile";

  @Override
  public String getToolWindowId() {
    return "Profiler";
  }

  @Override
  public Icon getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowDebugger;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Actions.ProfileCPU;
  }

  @Override
  public Icon getDisabledIcon() {
    return IconLoader.getDisabledIcon(getIcon());
  }

  @Override
  public String getDescription() {
    return "Profile ActionScript application"; // TODO: profiler description
  }

  @NotNull
  @Override
  public String getActionName() {
    return "Profile";
  }

  @NotNull
  @Override
  public String getId() {
    return EXECUTOR_ID;
  }

  @NotNull
  @Override
  public String getStartActionText() {
    return "&Profile";
  }

  @Override
  public String getContextActionId() {
    return "ProfileClass";
  }

  @Override
  public String getHelpId() {
    return null;  //TODO:
  }

  public static Executor getProfileExecutorInstance() {
    return ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
  }
}
