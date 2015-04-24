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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.lang.dart.sdk.DartConfigurable;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkGlobalLibUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DartServerRootsHandler {
  private final Set<Project> myTrackedProjects = new THashSet<Project>();
  private final List<String> myIncludedRoots = new SmartList<String>();
  private final List<String> myExcludedRoots = new SmartList<String>();
  private final Map<String, String> myPackageRoots = new SmartHashMap<String, String>();

  public DartServerRootsHandler() {
    // ProjectManagerListener.projectClosed() is not called in unittest mode, that's why ProjectLifecycleListener is used - it is called always
    final MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
    busConnection.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener.Adapter() {
      @Override
      public void afterProjectClosed(@NotNull final Project project) {
        if (myTrackedProjects.remove(project)) {
          updateRoots();
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

          newPackageRoots.putAll(DartConfigurable.getModulePathAndPackageRoot(module));

          for (ContentEntry contentEntry : ModuleRootManager.getInstance(module).getContentEntries()) {
            newIncludedRoots.add(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(contentEntry.getUrl())));

            for (String excludedUrl : contentEntry.getExcludeFolderUrls()) {
              newExcludedRoots.add(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(excludedUrl)));
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
}
