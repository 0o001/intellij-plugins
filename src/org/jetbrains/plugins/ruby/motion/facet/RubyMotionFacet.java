package org.jetbrains.plugins.ruby.motion.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.ActionRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.gem.GemModificationUtil;
import org.jetbrains.plugins.ruby.motion.RubyMotionUtil;
import org.jetbrains.plugins.ruby.motion.paramdefs.RubyMotionParamdefsProvider;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RFile;
import org.jetbrains.plugins.ruby.tasks.rake.RakeUtilBase;
import org.jetbrains.plugins.ruby.utils.IdeaInternalUtil;
import org.jetbrains.plugins.ruby.utils.VirtualFileUtil;

/**
 * @author Dennis.Ushakov
 */
public class RubyMotionFacet extends Facet<RubyMotionFacetConfiguration> {
  public RubyMotionFacet(@NotNull final FacetType facetType,
                  @NotNull final Module module,
                  @NotNull final String name,
                  @NotNull final RubyMotionFacetConfiguration configuration,
                  final Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  @Override
  public void initFacet() {
    PsiManager.getInstance(getModule().getProject()).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
      @Override
      public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        resetCachesIfNeeded(event);
      }

      @Override
      public void childAdded(@NotNull PsiTreeChangeEvent event) {
        resetCachesIfNeeded(event);
      }

      @Override
      public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        resetCachesIfNeeded(event);
      }

      @Override
      public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        resetCachesIfNeeded(event);
      }

      @Override
      public void childMoved(@NotNull PsiTreeChangeEvent event) {
        resetCachesIfNeeded(event);
      }
    });
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      RubyMotionParamdefsProvider.ensureParamdefsLoaded();
    }
  }

  private void resetCachesIfNeeded(final PsiTreeChangeEvent event) {
    final PsiFile file = event.getFile();
    if (file instanceof RFile && RakeUtilBase.RAKE_FILE.equals(file.getName())) {
      RubyMotionUtil.getInstance().resetSdkAndFrameworks(getModule());
    }
  }

  @Nullable
  public static RubyMotionFacet getInstance(@NotNull final Module module) {
    if (module.isDisposed()) {
      return null;
    }
    return FacetManager.getInstance(module).getFacetByType(RubyMotionFacetType.ID);
  }

  public static void updateMotionLibrary(final ModifiableRootModel model) {
    IdeaInternalUtil.runInsideWriteAction(new ActionRunner.InterruptibleRunnable() {
      public void run() throws Exception {
        boolean librarySeen = false;
        for (OrderEntry entry : model.getOrderEntries()) {
          if (entry instanceof LibraryOrderEntry) {
            final String libraryName = ((LibraryOrderEntry)entry).getLibraryName();
            if (RubyMotionUtil.RUBY_MOTION_LIBRARY.equals(libraryName)) {
              librarySeen = true;
              break;
            }
          }
        }
        if (!librarySeen) {
          Library library = LibraryTablesRegistrar.getInstance().getLibraryTable().getLibraryByName(RubyMotionUtil.RUBY_MOTION_LIBRARY);
          if (library == null) {
            // we just create new project library
            library = createLibrary();
          }
          if (library != null) {
            final LibraryOrderEntry libraryOrderEntry = model.addLibraryEntry(library);
            libraryOrderEntry.setScope(DependencyScope.PROVIDED);
          }
        }
      }
    });
  }

  @Nullable
  private static Library createLibrary() {
    final VirtualFile motion = VirtualFileUtil.findFileBy(RubyMotionUtil.getInstance().getRubyMotionPath());
    if (motion == null) return null;

    final LibraryTable.ModifiableModel model = GemModificationUtil.getLibraryTableModifiableModel();
    final Library library = model.createLibrary(RubyMotionUtil.RUBY_MOTION_LIBRARY);
    final Library.ModifiableModel libModel = library.getModifiableModel();
    for (VirtualFile child : motion.getChildren()) {
      if (child != null) {
        libModel.addRoot(child, OrderRootType.CLASSES);
        libModel.addRoot(child, OrderRootType.SOURCES);
      }
    }
    libModel.commit();
    model.commit();
    return library;
  }
}
