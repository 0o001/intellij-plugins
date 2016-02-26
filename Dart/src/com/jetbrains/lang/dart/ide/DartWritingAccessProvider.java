package com.jetbrains.lang.dart.ide;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import com.intellij.psi.PsiFile;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

import static com.jetbrains.lang.dart.util.PubspecYamlUtil.PUBSPEC_YAML;

public class DartWritingAccessProvider extends WritingAccessProvider {

  private final Project myProject;

  public DartWritingAccessProvider(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Collection<VirtualFile> requestWriting(VirtualFile... files) {
    return Collections.emptyList();
  }

  @Override
  public boolean isPotentiallyWritable(@NotNull VirtualFile file) {
    if (DartFileType.INSTANCE != file.getFileType()) return true;
    return !isInDartSdkOrDartPackagesFolder(myProject, file);
  }

  public static boolean isInDartSdkOrDartPackagesFolder(final @NotNull PsiFile psiFile) {
    final VirtualFile vFile = psiFile.getOriginalFile().getVirtualFile();
    return vFile != null && isInDartSdkOrDartPackagesFolder(psiFile.getProject(), vFile);
  }

  public static boolean isInDartSdkOrDartPackagesFolder(final @NotNull Project project, final @NotNull VirtualFile file) {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return !fileIndex.isInContent(file) || fileIndex.isExcluded(file) || isInDartPackagesFolder(fileIndex, file);
  }

  private static boolean isInDartPackagesFolder(final ProjectFileIndex fileIndex, final VirtualFile file) {
    VirtualFile parent = file;
    while ((parent = parent.getParent()) != null && fileIndex.isInContent(parent)) {
      if (DartUrlResolver.PACKAGES_FOLDER_NAME.equals(parent.getName())) {
        return VfsUtilCore.findRelativeFile("../" + PUBSPEC_YAML, parent) != null;
      }
    }

    return false;
  }
}
