package com.intellij.lang.javascript.flex.projectStructure.model;

import org.jetbrains.annotations.NotNull;

public interface SharedLibraryEntry extends DependencyEntry {

  @NotNull
  String getLibraryName();
  
  @NotNull
  String getLibraryLevel();
}
