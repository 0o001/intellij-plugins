package com.jetbrains.lang.dart.analyzer;

import com.intellij.ProjectTopics;
import com.intellij.codeInspection.SmartHashMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.lang.dart.DartProjectComponent;
import com.jetbrains.lang.dart.sdk.DartConfigurable;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkGlobalLibUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jetbrains.lang.dart.util.PubspecYamlUtil.PUBSPEC_YAML;

public class DartServerRootsHandler {
  private final Set<Project> myTrackedProjects = new THashSet<Project>();
  private final List<String> myIncludedRoots = new SmartList<String>();
  private final List<String> myExcludedRoots = new SmartList<String>();
  private final Map<String, String> myPackageRoots = new THashMap<String, String>();

  public DartServerRootsHandler() {
    // ProjectManagerListener.projectClosed() is not called in unittest mode, that's why ProjectLifecycleListener is used - it is called always
    final MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
    busConnection.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener.Adapter() {
      @Override
      public void afterProjectClosed(@NotNull final Project project) {
        if (myTrackedProjects.remove(project)) {
          updateRoots();
          DartAnalysisServerService.getInstance().updateVisibleFiles();
        }
      }
    });
  }

  public void reset() {
    myTrackedProjects.clear();
    myIncludedRoots.clear();
    myExcludedRoots.clear();
  }

  public void ensureProjectServed(@NotNull final Project project) {
    if (myTrackedProjects.contains(project)) return;

    myTrackedProjects.add(project);
    updateRoots();
    DartAnalysisServerService.getInstance().updateVisibleFiles();

    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
      @Override
      public void rootsChanged(final ModuleRootEvent event) {
        updateRoots();
      }
    });
  }

  public Set<Project> getTrackedProjects() {
    return myTrackedProjects;
  }

  private void updateRoots() {
    final DartSdk sdk = DartSdk.getGlobalDartSdk();
    final List<String> newIncludedRoots = new SmartList<String>();
    final List<String> newExcludedRoots = new SmartList<String>();
    final Map<String, String> newPackageRoots = new SmartHashMap<String, String>();

    if (sdk != null) {
      for (Project project : myTrackedProjects) {
        for (Module module : DartSdkGlobalLibUtil.getModulesWithDartSdkGlobalLibAttached(project, sdk.getGlobalLibName())) {

          newPackageRoots.putAll(DartConfigurable.getContentRootPathToCustomPackageRootMap(module));

          final Set<String> excludedPackageSymlinkUrls = getExcludedPackageSymlinkUrls(module);

          for (ContentEntry contentEntry : ModuleRootManager.getInstance(module).getContentEntries()) {
            newIncludedRoots.add(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(contentEntry.getUrl())));

            for (String excludedUrl : contentEntry.getExcludeFolderUrls()) {
              if (!excludedPackageSymlinkUrls.contains(excludedUrl)) {
                newExcludedRoots.add(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(excludedUrl)));
              }
            }
          }
        }
      }
    }

    if (!myIncludedRoots.equals(newIncludedRoots) || !myExcludedRoots.equals(newExcludedRoots) || !myPackageRoots.equals(newPackageRoots)) {
      myIncludedRoots.clear();
      myExcludedRoots.clear();
      myPackageRoots.clear();

      if (DartAnalysisServerService.getInstance().updateRoots(newIncludedRoots, newExcludedRoots, newPackageRoots)) {
        myIncludedRoots.addAll(newIncludedRoots);
        myExcludedRoots.addAll(newExcludedRoots);
        myPackageRoots.putAll(newPackageRoots);
      }
    }
  }

  private static Set<String> getExcludedPackageSymlinkUrls(@NotNull final Module module) {
    final Set<String> result = new THashSet<String>();

    final Collection<VirtualFile> pubspecYamlFiles =
      FilenameIndex.getVirtualFilesByName(module.getProject(), PUBSPEC_YAML, module.getModuleContentScope());

    for (VirtualFile pubspecYamlFile : pubspecYamlFiles) {
      result.addAll(DartProjectComponent.collectFolderUrlsToExclude(module, pubspecYamlFile, false));
    }

    return result;
  }
}
