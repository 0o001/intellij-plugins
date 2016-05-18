package com.intellij.lang.javascript.changesignature;

import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.refactoring.changeSignature.JSChangeSignatureDialog;
import com.intellij.lang.javascript.refactoring.changeSignature.JSMethodDescriptor;
import com.intellij.lang.javascript.refactoring.changeSignature.JSParameterInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.refactoring.changeSignature.LanguageChangeSignatureDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class JSChangeSignatureDetector implements LanguageChangeSignatureDetector {

  @Override
  public ChangeInfo createInitialChangeInfo(@NotNull PsiElement element) {
    JSFunction method = PsiTreeUtil.getParentOfType(element, JSFunction.class, false);
    if (method == null || !isInsideMethodSignature(element, method)) {
      return null;
    }
    if (PsiTreeUtil.hasErrorElements(method.getParameterList())) {
      return null;
    }
    return new JSChangeInfo(method);
  }

  private static boolean isInsideMethodSignature(PsiElement element, @NotNull JSFunction method) {
    TextRange r = getRange(method);
    if (r != null && r.contains(element.getTextOffset())) {
      return true;
    }
    if (element instanceof PsiErrorElement && element.getPrevSibling().getNode().getElementType() == JSTokenTypes.COLON) {
      // function foo():<caret> {}
      return true;
    }
    if (element instanceof PsiWhiteSpace &&
        element.getPrevSibling() instanceof PsiErrorElement &&
        element.getPrevSibling().getPrevSibling().getNode().getElementType() == JSTokenTypes.COLON) {
      // function foo():<caret> {}
      return true;
    }
    return false;
  }

  @Override
  public boolean performChange(ChangeInfo changeInfo, ChangeInfo initialChangeInfo, @NotNull final String oldText, boolean silently) {
    final JSChangeInfo jsChangeInfo = (JSChangeInfo)changeInfo;
    JSMethodDescriptor descriptor = new JSMethodDescriptor(jsChangeInfo.getMethod(), false) {
      @Override
      public String getName() {
        return jsChangeInfo.getNewName();
      }

      @Override
      public List<JSParameterInfo> getParameters() {
        return Arrays.asList(jsChangeInfo.getNewParameters());
      }

      @Override
      public int getParametersCount() {
        return getParameters().size();
      }

      @Override
      public String getVisibility() {
        return jsChangeInfo.getNewVisibility().name();
      }

      @Override
      public JSFunction getMethod() {
        return jsChangeInfo.getMethod();
      }
    };
    JSChangeSignatureDialog d = new JSChangeSignatureDialog(descriptor, changeInfo.getMethod()) {
      @Override
      protected void invokeRefactoring(BaseRefactoringProcessor processor) {
        revertChanges(jsChangeInfo.getMethod(), oldText);
        super.invokeRefactoring(processor);
      }
    };
    return d.showAndGet();
  }

  // TODO generalize
  private static void revertChanges(final PsiElement method, final String oldText) {
    //UndoManager.getInstance(method.getProject()).undoableActionPerformed(new );
    ApplicationManager.getApplication().runWriteAction(() -> {
      final PsiFile file = method.getContainingFile();
      final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(method.getProject());
      final Document document = documentManager.getDocument(file);
      if (document != null) {
        final TextRange textRange = method.getTextRange();
        document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), oldText);
        documentManager.commitDocument(document);
      }
    });
  }

  @Override
  public boolean isChangeSignatureAvailableOnElement(PsiElement element, ChangeInfo currentInfo) {
    if (currentInfo instanceof JSChangeInfo) {
      return element.getNode().getElementType() == JSTokenTypes.IDENTIFIER &&
             Comparing.equal(currentInfo.getMethod(), element.getParent().getParent());
    }
    return false;
  }



  @Nullable
  private static TextRange getRange(PsiElement element) {
    JSFunction f = PsiTreeUtil.getParentOfType(element, JSFunction.class, false);
    if (f == null) return null;
    ASTNode identifier = f.findNameIdentifier();
    if (identifier == null) {
      return null;
    }
    PsiElement e = f.getReturnTypeElement();
    if (e == null) {
      ASTNode colon = f.getNode().findChildByType(JSTokenTypes.COLON);
      if (colon != null) {
        e = colon.getPsi();
      }
    }
    if (e == null) {
      e = f.getParameterList();
    }
    return new TextRange(identifier.getTextRange().getStartOffset(), e.getTextRange().getEndOffset());
  }

  @Override
  public boolean isMoveParameterAvailable(PsiElement parameter, boolean left) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void moveParameter(PsiElement parameter, Editor editor, boolean left) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean ignoreChanges(PsiElement element) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public TextRange getHighlightingRange(ChangeInfo changeInfo) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public String extractSignature(PsiElement child, @NotNull ChangeInfo initialChangeInfo) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ChangeInfo createNextChangeInfo(String signature, @NotNull ChangeInfo currentInfo, String initialName) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
