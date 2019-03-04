// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.entities.metadata.psi;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.StubElement;
import org.angular2.entities.metadata.stubs.Angular2MetadataElementStub;
import org.angular2.entities.metadata.stubs.Angular2MetadataNodeModuleStub;
import org.angular2.lang.metadata.psi.MetadataElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.util.ObjectUtils.doIfNotNull;

public abstract class Angular2MetadataElement<Stub extends Angular2MetadataElementStub<?>> extends MetadataElement<Stub> {

  public Angular2MetadataElement(@NotNull Stub element) {
    super(element);
  }

  public Angular2MetadataNodeModule getNodeModule() {
    StubElement stub = getStub();
    while (stub != null && !(stub instanceof Angular2MetadataNodeModuleStub)) {
      stub = stub.getParentStub();
    }
    return stub != null ? (Angular2MetadataNodeModule)stub.getPsi() : null;
  }

  @Nullable
  @Override
  public String getText() {
    return "";
  }

  @Override
  public int getTextLength() {
    return 0;
  }

  protected PsiFile loadRelativeFile(@NotNull String path) {
    VirtualFile sourceFile = getContainingFile().getViewProvider().getVirtualFile();
    VirtualFile moduleFile = doIfNotNull(sourceFile.getParent(),
                                         parentDir -> parentDir.findFileByRelativePath(path));
    return doIfNotNull(moduleFile, getManager()::findFile);
  }
}
