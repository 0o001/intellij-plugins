// Copyright 2000-2018 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.jetbrains.lang.dart.ide.runner;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XLineBreakpointTypeBase;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.ide.runner.base.DartDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DartLineBreakpointType extends XLineBreakpointTypeBase {

  protected DartLineBreakpointType() {
    super("Dart", DartBundle.message("dart.line.breakpoints.title"), new DartDebuggerEditorsProvider());
  }

  public boolean canPutAt(@NotNull final VirtualFile file, final int line, @NotNull final Project project) {
    return DartFileType.INSTANCE.equals(file.getFileType());
  }

  @Nullable
  @Override
  public Icon getPendingIcon() {
    return AllIcons.Debugger.Db_validate_breakpoint;
  }
}
