package com.intellij.flex.model.bc.impl;

import com.intellij.flex.model.bc.JpsFlexBCDependencyEntry;
import com.intellij.flex.model.bc.JpsFlexBCReference;
import com.intellij.flex.model.bc.JpsFlexBuildConfiguration;
import com.intellij.flex.model.bc.LinkageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.module.JpsModuleReference;

class JpsFlexBCDependencyEntryImpl extends JpsFlexDependencyEntryBase<JpsFlexBCDependencyEntryImpl> implements JpsFlexBCDependencyEntry {

  JpsFlexBCDependencyEntryImpl(final String moduleName, final String bcName, final LinkageType linkageType) {
    super(linkageType);

    final JpsModuleReference moduleRef = JpsElementFactory.getInstance().createModuleReference(moduleName);
    final JpsFlexBCReferenceImpl bcRef = new JpsFlexBCReferenceImpl(bcName, moduleRef);
    myContainer.setChild(JpsFlexBCReferenceImpl.ROLE, bcRef);
  }

  private JpsFlexBCDependencyEntryImpl(final JpsFlexBCDependencyEntryImpl original) {
    super(original);
  }

  @Override
  @NotNull
  public JpsFlexBCDependencyEntryImpl createCopy() {
    return new JpsFlexBCDependencyEntryImpl(this);
  }

// ------------------------------------

  @Override
  @Nullable
  public JpsFlexBuildConfiguration getBC() {
    final JpsFlexBCReference bcRef = myContainer.getChild(JpsFlexBCReferenceImpl.ROLE);
    return bcRef == null ? null : bcRef.resolve();
  }
}
