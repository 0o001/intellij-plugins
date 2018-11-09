package com.intellij.lang.javascript.flex.projectStructure.model.impl;

import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfigurationManager;
import com.intellij.lang.javascript.flex.projectStructure.model.ModifiableBuildConfigurationEntry;
import com.intellij.lang.javascript.flex.projectStructure.model.ModifiableDependencyEntry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class BuildConfigurationEntryImpl implements ModifiableBuildConfigurationEntry, StatefulDependencyEntry {

  private final DependencyTypeImpl myDependencyType = new DependencyTypeImpl();

  private final ModulePointer myModulePointer;

  @NotNull
  private final String myBcName;

  BuildConfigurationEntryImpl(@NotNull Module module, @NotNull String bcName) {
    this(ModulePointerManager.getInstance(module.getProject()).create(module), bcName);
  }

  BuildConfigurationEntryImpl(@NotNull Project project, @NotNull String moduleName, @NotNull String bcName) {
    this(ModulePointerManager.getInstance(project).create(moduleName), bcName);
  }

  BuildConfigurationEntryImpl(ModulePointer modulePointer, @NotNull String bcName) {
    myModulePointer = modulePointer;
    myBcName = bcName;
  }

  @Override
  @NotNull
  public String getModuleName() {
    return myModulePointer.getModuleName();
  }

  @NotNull
  @Override
  public DependencyTypeImpl getDependencyType() {
    return myDependencyType;
  }

  @Override
  @Nullable
  public Module findModule() {
    final Module module = myModulePointer.getModule();
    return module != null && ModuleType.get(module) instanceof FlexModuleType ? module : null;
  }

  @Override
  @Nullable
  public FlexBuildConfiguration findBuildConfiguration() {
    Module module = findModule();
    return module != null ? FlexBuildConfigurationManager.getInstance(module).findConfigurationByName(myBcName) : null;
  }

  @Override
  @NotNull
  public String getBcName() {
    return myBcName;
  }

  public BuildConfigurationEntryImpl getCopy() {
    BuildConfigurationEntryImpl copy = new BuildConfigurationEntryImpl(myModulePointer, myBcName);
    applyTo(copy);
    return copy;
  }

  private void applyTo(ModifiableBuildConfigurationEntry copy) {
    myDependencyType.applyTo(copy.getDependencyType());
  }

  @Override
  public boolean isEqual(ModifiableDependencyEntry other) {
    if (!(other instanceof BuildConfigurationEntryImpl)) return false;
    if (!myBcName.equals(((BuildConfigurationEntryImpl)other).myBcName)) return false;
    if (!getModuleName().equals(((BuildConfigurationEntryImpl)other).getModuleName())) return false;
    if (!myDependencyType.isEqual(((BuildConfigurationEntryImpl)other).myDependencyType)) return false;
    return true;
  }

  @Override
  public EntryState getState() {
    EntryState state = new EntryState();
    state.MODULE_NAME = getModuleName();
    state.BC_NAME = myBcName;
    state.DEPENDENCY_TYPE = myDependencyType.getState();
    return state;
  }
}
