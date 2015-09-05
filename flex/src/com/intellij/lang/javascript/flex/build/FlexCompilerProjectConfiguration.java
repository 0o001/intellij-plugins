package com.intellij.lang.javascript.flex.build;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;

@State(
  name = "FlexCompilerConfiguration", // do not rename it for compatibility
  storages = {
    @Storage(file = StoragePathMacros.WORKSPACE_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/flexCompiler.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class FlexCompilerProjectConfiguration implements PersistentStateComponent<FlexCompilerProjectConfiguration> {

  public boolean GENERATE_FLEXMOJOS_CONFIGS = true;

  public boolean USE_BUILT_IN_COMPILER = true;
  public boolean USE_FCSH = false;
  public boolean USE_MXMLC_COMPC = false;
  public boolean PREFER_ASC_20 = true;
  public int MAX_PARALLEL_COMPILATIONS = 4;
  public int HEAP_SIZE_MB = 512;
  public String VM_OPTIONS = "";

  public static FlexCompilerProjectConfiguration getInstance(final Project project) {
    return ServiceManager.getService(project, FlexCompilerProjectConfiguration.class);
  }

  public FlexCompilerProjectConfiguration getState() {
    return this;
  }

  public void loadState(final FlexCompilerProjectConfiguration state) {
    GENERATE_FLEXMOJOS_CONFIGS = state.GENERATE_FLEXMOJOS_CONFIGS;

    USE_BUILT_IN_COMPILER = state.USE_BUILT_IN_COMPILER;
    USE_FCSH = state.USE_FCSH;
    USE_MXMLC_COMPC = state.USE_MXMLC_COMPC;

    PREFER_ASC_20 = state.PREFER_ASC_20;

    //compatibility
    if (USE_FCSH /*&& USE_BUILT_IN_COMPILER*/) {
      USE_FCSH = false;
      USE_BUILT_IN_COMPILER = true;
    }

    //  MAX_PARALLEL_COMPILATIONS = state.MAX_PARALLEL_COMPILATIONS;
    HEAP_SIZE_MB = state.HEAP_SIZE_MB;
    VM_OPTIONS = state.VM_OPTIONS;
  }
}
