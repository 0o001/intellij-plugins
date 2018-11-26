package com.intellij.prettierjs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.lang.javascript.library.JSLibraryUtil;
import com.intellij.lang.javascript.psi.util.JSProjectUtil;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrettierCodeStyleEditorNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel>
  implements DumbAware {

  private static final Key<EditorNotificationPanel> KEY = Key.create("prettier.codestyle.notification.panel");
  private static final String NOTIFICATION_DISMISSED_KEY = "prettier.import.notification.dismissed";
  private final PropertiesComponent myPropertiesComponent;
  private final EditorNotifications myEditorNotifications;

  public PrettierCodeStyleEditorNotificationProvider(PropertiesComponent propertiesComponent,
                                                     Project project,
                                                     EditorNotifications editorNotifications, 
                                                     PsiManager psiManager) {
    myPropertiesComponent = propertiesComponent;
    myEditorNotifications = editorNotifications;
    psiManager.addPsiTreeChangeListener(new PsiTreeAnyChangeAbstractAdapter() {
      @Override
      protected void onChange(@Nullable PsiFile file) {
        if (file == null) return;
        final VirtualFile vFile = file.getViewProvider().getVirtualFile();
        if (PrettierUtil.isConfigFileOrPackageJson(vFile) && !alreadyDismissed()){
          myEditorNotifications.updateNotifications(vFile);
        }
      }
    }, project);
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (!(fileEditor instanceof TextEditor)) return null;
    final Project project = ((TextEditor)fileEditor).getEditor().getProject();
    if (project == null) {
      return null;
    }
    if (!file.isWritable() || JSProjectUtil.isInLibrary(file, project) || JSLibraryUtil.isProbableLibraryFile(file)) {
      return null;
    }
    if (alreadyDismissed()) {
      return null;
    }

    PrettierUtil.Config config = null;
    if (PrettierUtil.isConfigFile(file)) {
      config = PrettierUtil.parseConfig(project, file);
    }
    if (PackageJsonUtil.isPackageJsonFile(file)) {
      //if package.json is currently opened, but there is a neighboring config file
      VirtualFile configVFile = PrettierUtil.findSingleConfigInDirectory(file.getParent());
      if (configVFile != null) {
        config = PrettierUtil.parseConfig(project, configVFile);
        file = configVFile;
      }
      else {
        config = PrettierUtil.parseConfig(project, file);
      }
    }
    if (config == null) {
      return null;
    }
    if (PrettierCompatibleCodeStyleInstaller.isInstalled(project, config)) {
      return null;
    }
    final EditorNotificationPanel panel = new EditorNotificationPanel(EditorColors.GUTTER_BACKGROUND);
    panel.setText(PrettierBundle.message("editor.notification.title"));

    PrettierUtil.Config finalConfig = config;
    VirtualFile finalFile = file;
    panel.createActionLabel(PrettierBundle.message("editor.notification.yes.text"),
                            () -> {
                              PrettierCompatibleCodeStyleInstaller.install(project, finalFile, finalConfig, false);
                              myEditorNotifications.updateAllNotifications();
                            });
    panel.createActionLabel(PrettierBundle.message("editor.notification.no.text"), () -> {
      myEditorNotifications.updateAllNotifications();
      myPropertiesComponent.setValue(NOTIFICATION_DISMISSED_KEY, true);
    });

    return panel;
  }

  private boolean alreadyDismissed() {
    return myPropertiesComponent.getBoolean(NOTIFICATION_DISMISSED_KEY);
  }
}
