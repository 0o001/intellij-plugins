// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.entities.metadata.psi;

import org.angular2.entities.Angular2Declaration;
import org.angular2.entities.Angular2Module;
import org.angular2.entities.metadata.stubs.Angular2MetadataEntityStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Angular2MetadataDeclaration<Stub extends Angular2MetadataEntityStub<?>>
  extends Angular2MetadataEntity<Stub> implements Angular2Declaration {

  public Angular2MetadataDeclaration(@NotNull Stub element) {
    super(element);
  }

  @Nullable
  @Override
  public Angular2Module getModule() {
    return null;
  }
}
