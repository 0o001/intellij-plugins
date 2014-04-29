package com.jetbrains.lang.dart.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PairConsumer;
import com.jetbrains.lang.dart.ide.index.DartLibraryIndex;
import com.jetbrains.lang.dart.sdk.DartConfigurable;
import com.jetbrains.lang.dart.sdk.DartSdk;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jetbrains.lang.dart.util.PubspecYamlUtil.*;

class DartUrlResolverImpl extends DartUrlResolver {

  private final @NotNull Project myProject;
  private final @Nullable DartSdk myDartSdk;
  private final @Nullable VirtualFile myPubspecYamlFile;
  private final @NotNull List<VirtualFile> myPackageRoots = new ArrayList<VirtualFile>();
  private final @NotNull Map<String, VirtualFile> myLivePackageNameToDirMap = new THashMap<String, VirtualFile>();

  DartUrlResolverImpl(final @NotNull Project project, final @NotNull VirtualFile contextFile) {
    myProject = project;
    myDartSdk = DartSdk.getGlobalDartSdk();
    myPubspecYamlFile = initPackageRootsAndReturnPubspecYamlFile(contextFile);
    initLivePackageNameToDirMap();
  }

  @Nullable
  public VirtualFile getPubspecYamlFile() {
    return myPubspecYamlFile;
  }

  @NotNull
  public VirtualFile[] getPackageRoots() {
    return myPackageRoots.toArray(new VirtualFile[myPackageRoots.size()]);
  }

  public void processLivePackages(final @NotNull PairConsumer<String, VirtualFile> packageNameAndDirConsumer) {
    for (Map.Entry<String, VirtualFile> entry : myLivePackageNameToDirMap.entrySet()) {
      packageNameAndDirConsumer.consume(entry.getKey(), entry.getValue());
    }
  }

  @Nullable
  public VirtualFile findFileByDartUrl(final @NotNull String url) {
    if (url.startsWith(DART_PREFIX)) {
      return findFileInDartSdkLibFolder(myProject, myDartSdk, url);
    }

    if (url.startsWith(PACKAGE_PREFIX)) {
      final String packageRelPath = url.substring(PACKAGE_PREFIX.length());

      final int slashIndex = packageRelPath.indexOf('/');
      final String packageName = slashIndex > 0 ? packageRelPath.substring(0, slashIndex) : packageRelPath;
      final VirtualFile packageDir = StringUtil.isEmpty(packageName) ? null : myLivePackageNameToDirMap.get(packageName);
      if (packageDir != null) {
        return slashIndex > 0 ? packageDir.findFileByRelativePath(packageRelPath.substring(slashIndex + 1)) : packageDir;
      }

      for (final VirtualFile packageRoot : myPackageRoots) {
        final VirtualFile file = packageRoot.findFileByRelativePath(packageRelPath);
        if (file != null) {
          return file;
        }
      }
    }

    if (url.startsWith(FILE_PREFIX)) {
      final String path = StringUtil.trimLeading(url.substring(FILE_PREFIX.length()), '/');
      return LocalFileSystem.getInstance().findFileByPath(SystemInfo.isWindows ? path : ("/" + path));
    }

    return null;
  }

  @NotNull
  public String getDartUrlForFile(final @NotNull VirtualFile file) {
    String result = null;

    if (myDartSdk != null) result = getUrlIfFileFromSdkLib(myProject, file, myDartSdk);
    if (result != null) return result;

    result = getUrlIfFileFromLivePackage(file, myLivePackageNameToDirMap);
    if (result != null) return result;

    result = getUrlIfFileFromPackageRoot(file, myPackageRoots);
    if (result != null) return result;

    // see com.google.dart.tools.debug.core.server.ServerBreakpointManager#getAbsoluteUrlForResource()
    return new File(file.getPath()).toURI().toString();
  }

  @Nullable
  private static String getUrlIfFileFromSdkLib(final @NotNull Project project,
                                               final @NotNull VirtualFile file,
                                               final @NotNull DartSdk sdk) {
    final VirtualFile sdkLibFolder = LocalFileSystem.getInstance().findFileByPath(sdk.getHomePath() + "/lib");
    final String relativeToSdkLibFolder = sdkLibFolder == null ? null : VfsUtilCore.getRelativePath(file, sdkLibFolder, '/');
    final String sdkLibName = relativeToSdkLibFolder == null
                              ? null
                              : DartLibraryIndex.getStandardLibraryNameByRelativePath(project, relativeToSdkLibFolder);
    return sdkLibName != null
           ? DART_PREFIX + sdkLibName
           : relativeToSdkLibFolder != null
             ? DART_PREFIX + relativeToSdkLibFolder
             : null;
  }

  @Nullable
  private static String getUrlIfFileFromLivePackage(final @NotNull VirtualFile file,
                                                    final @NotNull Map<String, VirtualFile> livePackageNameToDirMap) {
    for (Map.Entry<String, VirtualFile> entry : livePackageNameToDirMap.entrySet()) {
      final String packageName = entry.getKey();
      final VirtualFile packageDir = entry.getValue();
      final String relPath = VfsUtilCore.getRelativePath(file, packageDir, '/');
      if (relPath != null) {
        return PACKAGE_PREFIX + packageName + "/" + relPath;
      }
    }
    return null;
  }

  @Nullable
  private static String getUrlIfFileFromPackageRoot(final @NotNull VirtualFile file, final @NotNull List<VirtualFile> packageRoots) {
    for (VirtualFile packageRoot : packageRoots) {
      final String relPath = VfsUtilCore.getRelativePath(file, packageRoot, '/');
      if (relPath != null) {
        return PACKAGE_PREFIX + relPath;
      }
    }
    return null;
  }

  @Nullable
  private VirtualFile initPackageRootsAndReturnPubspecYamlFile(final @NotNull VirtualFile contextFile) {
    final Module module = ModuleUtilCore.findModuleForFile(contextFile, myProject);
    if (module == null) return null;

    final VirtualFile[] customPackageRoots = DartConfigurable.getCustomPackageRoots(module);
    if (customPackageRoots.length > 0) {
      Collections.addAll(myPackageRoots, customPackageRoots);
      return null;
    }

    final VirtualFile pubspecYamlFile = findPubspecYamlFile(myProject, contextFile);
    final VirtualFile parentFolder = pubspecYamlFile == null ? null : pubspecYamlFile.getParent();
    final VirtualFile packagesFolder = parentFolder == null ? null : parentFolder.findChild(PACKAGES_FOLDER_NAME);
    if (packagesFolder != null && packagesFolder.isDirectory()) {
      myPackageRoots.add(packagesFolder);
    }

    return pubspecYamlFile;
  }

  private static VirtualFile findPubspecYamlFile(final @NotNull Project project, final @NotNull VirtualFile contextFile) {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    VirtualFile parent = contextFile;
    while ((parent = parent.getParent()) != null && fileIndex.isInContent(parent)) {
      final VirtualFile file = parent.findChild(PUBSPEC_YAML);
      if (file != null && !file.isDirectory()) return file;
    }

    return null;
  }

  private void initLivePackageNameToDirMap() {
    if (myPubspecYamlFile == null) return;

    final VirtualFile baseDir = myPubspecYamlFile.getParent();
    final Map<String, Object> yamlInfo = getPubspecYamlInfo(myPubspecYamlFile);
    if (baseDir != null && yamlInfo != null) {
      fillLivePackageNameToDirMap(myLivePackageNameToDirMap, baseDir, yamlInfo);
    }
  }

  private static void fillLivePackageNameToDirMap(final @NotNull Map<String, VirtualFile> packageNameToDirMap,
                                                  final @NotNull VirtualFile baseDir,
                                                  final @NotNull Map<String, Object> yamlInfo) {
    final Object name = yamlInfo.get(NAME);
    final VirtualFile libFolder = baseDir.findChild(LIB_DIRECTORY_NAME);
    if (name instanceof String && libFolder != null && libFolder.isDirectory()) {
      packageNameToDirMap.put((String)name, libFolder);
    }

    addPathPackagesToMap(packageNameToDirMap, yamlInfo.get(DEPENDENCIES), baseDir);
    addPathPackagesToMap(packageNameToDirMap, yamlInfo.get(DEV_DEPENDENCIES), baseDir);
  }

  // Path packages: https://www.dartlang.org/tools/pub/dependencies.html#path-packages
  private static void addPathPackagesToMap(final @NotNull Map<String, VirtualFile> packageNameToDirMap,
                                           final @Nullable Object yamlDep,
                                           final @NotNull VirtualFile baseDir) {
    // see com.google.dart.tools.core.pub.PubspecModel#processDependencies
    if (!(yamlDep instanceof Map)) return;

    //noinspection unchecked
    for (Map.Entry<String, Object> packageEntry : ((Map<String, Object>)yamlDep).entrySet()) {
      final String packageName = packageEntry.getKey();

      final Object packageEntryValue = packageEntry.getValue();
      if (packageEntryValue instanceof Map) {
        final Object pathObj = ((Map)packageEntryValue).get(PATH);
        if (pathObj instanceof String) {
          final VirtualFile packageFolder = VfsUtilCore.findRelativeFile(pathObj + "/" + LIB_DIRECTORY_NAME, baseDir);
          if (packageFolder != null && packageFolder.isDirectory()) {
            packageNameToDirMap.put(packageName, packageFolder);
          }
        }
      }
    }
  }
}
