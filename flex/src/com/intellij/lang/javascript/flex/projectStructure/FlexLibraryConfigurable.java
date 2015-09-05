package com.intellij.lang.javascript.flex.projectStructure;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.roots.impl.RootModelImpl;
import com.intellij.openapi.roots.impl.libraries.JarDirectories;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibraryConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class FlexLibraryConfigurable extends LibraryConfigurable {

  private final LibraryEx myLibraryForPresentation = new LibraryEx() {
    public String getName() {
      return getLibraryEditor().getName();
    }

    @NotNull
    public String[] getUrls(@NotNull final OrderRootType rootType) {
      return getLibraryEditor().getUrls(rootType);
    }

    @NotNull
    public VirtualFile[] getFiles(@NotNull final OrderRootType rootType) {
      return getLibraryEditor().getFiles(rootType);
    }

    @NotNull
    public ModifiableModelEx getModifiableModel() {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public String[] getExcludedRootUrls() {
      throw new UnsupportedOperationException("'getExcludedRootUrls' not implemented in " + getClass().getName());
    }

    @NotNull
    @Override
    public VirtualFile[] getExcludedRoots() {
      throw new UnsupportedOperationException("'getExcludedRootFiles' not implemented in " + getClass().getName());
    }

    public LibraryTable getTable() {
      return getEditableObject().getTable();
    }

    @NotNull
    public RootProvider getRootProvider() {
      throw new UnsupportedOperationException();
    }

    public boolean isJarDirectory(@NotNull final String url) {
      return getLibraryEditor().isJarDirectory(url, JarDirectories.DEFAULT_JAR_DIRECTORY_TYPE);
    }

    public boolean isJarDirectory(@NotNull final String url, @NotNull final OrderRootType rootType) {
      return getLibraryEditor().isJarDirectory(url, rootType);
    }

    public boolean isValid(@NotNull final String url, @NotNull final OrderRootType rootType) {
      return getLibraryEditor().isValid(url, rootType);
    }

    public void dispose() {
    }

    public void readExternal(final Element element) throws InvalidDataException {
    }

    public void writeExternal(final Element element) throws WriteExternalException {
    }

    public Library cloneLibrary(final RootModelImpl rootModel) {
      return this;
    }

    public List<String> getInvalidRootUrls(final OrderRootType type) {
      return Collections.<String>emptyList();
    }

    public boolean isDisposed() {
      return false;
    }

    @Override
    public PersistentLibraryKind<?> getKind() {
      return ((LibraryEx)getEditableObject()).getKind();
    }

    public LibraryProperties getProperties() {
      return ((LibraryEx)getEditableObject()).getProperties();
    }
  };

  public FlexLibraryConfigurable(final Library library, final StructureConfigurableContext context, final Runnable treeUpdater) {
    super(context.createModifiableModelProvider(library.getTable().getTableLevel()), library, context, treeUpdater);
  }

  public LibraryEx getLibraryForPresentation() {
    return myLibraryForPresentation;
  }

  public String getHelpTopic() {
    return "reference.settingsdialog.project.structure.library";
  }
}
