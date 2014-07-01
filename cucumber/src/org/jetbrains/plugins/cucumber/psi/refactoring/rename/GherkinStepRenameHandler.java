package org.jetbrains.plugins.cucumber.psi.refactoring.rename;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.RenameDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;

/**
 * User: avokin
 * Date: 19/06/14
 */
public class GherkinStepRenameHandler extends PsiElementRenameHandler {
  @Override
  public boolean isAvailableOnDataContext(DataContext dataContext) {
    PsiElement element = getGherkinStep(dataContext);
    return element != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    final GherkinStep step = getGherkinStep(dataContext);
    if (step == null) {
      return;
    }

    RenameDialog.showRenameDialog(dataContext, new CucumberStepRenameDialog(project, step, null, editor));
  }

  @Override
  public boolean isRenaming(DataContext dataContext) {
    PsiElement element = getGherkinStep(dataContext);
    return element != null;
  }

  @Nullable
  public GherkinStep getGherkinStep(@Nullable final DataContext context) {
    PsiElement element = null;
    if (context == null) return null;
    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (editor != null) {
      final PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(context);
      if (psiFile != null) {
        element = psiFile.findElementAt(editor.getCaretModel().getOffset());
      }
    }
    if (element == null) {
      element = CommonDataKeys.PSI_ELEMENT.getData(context);
    }
    return element instanceof GherkinStep ? (GherkinStep)element : PsiTreeUtil.getParentOfType(element, GherkinStep.class);
  }
}
