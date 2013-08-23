package com.jetbrains.lang.dart.ide.watcher;

import com.intellij.ide.macro.FileDirMacro;
import com.intellij.ide.macro.FileNameMacro;
import com.intellij.ide.macro.FilePathMacro;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.watcher.config.BackgroundTaskConsumer;
import com.intellij.plugins.watcher.model.TaskOptions;
import com.intellij.psi.PsiBundle;
import com.intellij.psi.PsiFile;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.ide.settings.DartSettingsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class Dart2JSTaskConsumer extends BackgroundTaskConsumer {
  @Override
  public boolean isAvailable(PsiFile file) {
    // do not suggest for file.
    return false;
  }

  @NotNull
  @Override
  public TaskOptions getOptionsTemplate() {
    TaskOptions options = new TaskOptions();
    options.setName("Dart2JS");
    final VirtualFile dart2JS = DartSettingsUtil.getSettings().getDart2JS();
    if (dart2JS != null) {
      options.setProgram(dart2JS.getPath());
    }
    options.setDescription("Compiles .dart files into .js files");
    options.setFileExtension(DartFileType.DEFAULT_EXTENSION);
    options.setScopeName(PsiBundle.message("psi.search.scope.project"));

    options.setArguments("--out=$" + new FilePathMacro().getName() + "$.js $" + new FilePathMacro().getName() + "$");
    options.setWorkingDir("$" + new FileDirMacro().getName() + "$");

    options.setOutput("$" + new FileNameMacro().getName() + "$.js:$" +
                      new FileNameMacro().getName() + "$.js.map:$" +
                      new FileNameMacro().getName() + "$.js.deps");
    options.setTrackOnlyRoot(true);
    options.setImmediateSync(false);

    return options;
  }
}
