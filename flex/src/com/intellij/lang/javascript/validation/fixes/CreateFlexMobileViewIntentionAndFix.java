package com.intellij.lang.javascript.validation.fixes;

import com.intellij.lang.javascript.JSBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class CreateFlexMobileViewIntentionAndFix extends CreateMxmlFileIntentionBase {
  private final boolean myAskForPackage;

  @NotNull
  public String getText() {
    return JSBundle.message("create.mobile.view", myClassName);
  }

  public CreateFlexMobileViewIntentionAndFix(final String classFqn, final PsiElement element, final boolean askForPackage) {
    super(classFqn, element);
    myAskForPackage = askForPackage;
  }

  protected String getFileText() {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
           "<s:View xmlns:fx=\"http://ns.adobe.com/mxml/2009\" xmlns:s=\"library://ns.adobe.com/flex/spark\">\n" +
           "\n" +
           "</s:View>\n";
  }

  protected Pair<String, PsiDirectory> getFileTextAndDir(final @NotNull Module module) {
    if (!myAskForPackage) return super.getFileTextAndDir(module);

    final PsiDirectory targetDirectory;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      targetDirectory = ApplicationManager.getApplication().runWriteAction(new Computable<PsiDirectory>() {
        public PsiDirectory compute() {
          return CreateClassOrInterfaceFix.findOrCreateDirectory("foo", myElement);
        }
      });
    }
    else {
      final ChoosePackageDialog dialog = new ChoosePackageDialog(module, getText(), myPackageName, myElement.getContainingFile());
      if (!dialog.showAndGet()) {
        return Pair.empty();
      }

      targetDirectory = dialog.getTargetDirectory();
    }

    return Pair.create(getFileText(), targetDirectory);
  }
}
