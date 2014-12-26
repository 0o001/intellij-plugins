package com.jetbrains.lang.dart.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.lang.dart.psi.DartId;
import com.jetbrains.lang.dart.psi.DartImportOrExportStatement;
import com.jetbrains.lang.dart.psi.DartReference;
import com.jetbrains.lang.dart.resolve.DartResolver;
import com.jetbrains.lang.dart.util.DartClassResolveResult;
import com.jetbrains.lang.dart.util.DartElementGenerator;
import com.jetbrains.lang.dart.util.DartResolveUtil;
import org.jetbrains.annotations.NotNull;

public class DartLibraryComponentReferenceExpressionBase extends DartExpressionImpl implements DartReference, PsiPolyVariantReference {
  public DartLibraryComponentReferenceExpressionBase(ASTNode node) {
    super(node);
  }

  @Override
  public PsiElement getElement() {
    return this;
  }

  @Override
  public PsiReference getReference() {
    return this;
  }

  @Override
  public TextRange getRangeInElement() {
    final TextRange textRange = getTextRange();
    return new TextRange(0, textRange.getEndOffset() - textRange.getStartOffset());
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getText();
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    final DartId identifier = PsiTreeUtil.getChildOfType(this, DartId.class);
    final DartId identifierNew = DartElementGenerator.createIdentifierFromText(getProject(), newElementName);
    if (identifier != null && identifierNew != null) {
      getNode().replaceChild(identifier.getNode(), identifierNew.getNode());
    }
    return this;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    return this;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return resolve() == element;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @Override
  public PsiElement resolve() {
    final ResolveResult[] resolveResults = multiResolve(true);

    return resolveResults.length == 0 ||
           resolveResults.length > 1 ||
           !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    final DartImportOrExportStatement statement = PsiTreeUtil.getParentOfType(this, DartImportOrExportStatement.class);
    final String uri = statement == null ? null : statement.getUriString();
    final VirtualFile vFile = DartResolveUtil.getRealVirtualFile(getContainingFile());
    final VirtualFile importedFile = uri == null || vFile == null ? null
                                                                  : DartResolveUtil.getImportedFile(getProject(), vFile, uri);
    final PsiFile importedPsiFile = importedFile == null ? null : PsiManager.getInstance(getProject()).findFile(importedFile);
    return importedPsiFile == null ? ResolveResult.EMPTY_ARRAY
                                   : DartResolveUtil.toCandidateInfoArray(DartResolver.resolveSimpleReference(importedPsiFile, getText()));
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return ResolveResult.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public DartClassResolveResult resolveDartClass() {
    return DartClassResolveResult.EMPTY;
  }
}