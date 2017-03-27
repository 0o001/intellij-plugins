package com.intellij.lang.javascript.linter.tslint.fix;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.lang.javascript.linter.tslint.TsLintBundle;
import com.intellij.lang.javascript.linter.tslint.execution.TsLinterError;
import com.intellij.lang.javascript.linter.tslint.highlight.TsLintFixInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;


public class TsLintErrorFixAction extends BaseIntentionAction implements HighPriorityAction {

  @NotNull
  private final TsLinterError myError;
  private final long myModificationStamp;


  public TsLintErrorFixAction(@NotNull TsLinterError error, @NotNull Document document) {
    //noinspection DialogTitleCapitalization
    setText(getFamilyName());
    myError = error;
    myModificationStamp = document.getModificationStamp();
  }

  @NotNull
  @Override
  public String getText() {
    //noinspection DialogTitleCapitalization
    return TsLintBundle.message("tslint.action.fix.problems.current.text");
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return editor != null && editor.getDocument().getModificationStamp() == myModificationStamp && myError.getFixInfo() != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (!isAvailable(project, editor, file)) {
      return;
    }

    TsLintFixInfo info = myError.getFixInfo();
    if (info == null) {
      return;
    }
    WriteCommandAction.runWriteCommandAction(project, getText(), null, () -> {
      Document document = editor.getDocument();
      TsLintFixInfo.TsLintFixReplacements[] replacements = info.innerReplacements;

      if (replacements == null || replacements.length == 0) {
        return;
      }
      Arrays.sort(replacements, Comparator.comparingInt(el -> -el.innerStart));

      for (TsLintFixInfo.TsLintFixReplacements replacement : replacements) {
        int offset = replacement.innerStart;
        if (offset > document.getTextLength()) {
          //incorrect value
          return;
        }

        document.replaceString(offset, offset + replacement.innerLength, StringUtil.notNullize(replacement.innerText));
      }
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });

    DaemonCodeAnalyzer.getInstance(project).restart(file);
  }
}
