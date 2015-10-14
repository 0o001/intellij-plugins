package com.jetbrains.lang.dart.ide.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.Consumer;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;
import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService;
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression;
import com.jetbrains.lang.dart.psi.DartUriBasedDirective;
import com.jetbrains.lang.dart.psi.DartUriElement;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.util.DartResolveUtil;
import org.apache.commons.lang3.StringUtils;
import org.dartlang.analysis.server.protocol.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class DartServerCompletionContributor extends CompletionContributor {
  public DartServerCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().withLanguage(DartLanguage.INSTANCE), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull final CompletionParameters parameters,
                                    @NotNull final ProcessingContext context,
                                    @NotNull final CompletionResultSet originalResultSet) {
        final VirtualFile file = DartResolveUtil.getRealVirtualFile(parameters.getOriginalFile());
        if (file == null) return;

        final Project project = parameters.getOriginalFile().getProject();

        final DartSdk sdk = DartSdk.getDartSdk(project);
        if (sdk == null || !DartAnalysisServerService.isDartSdkVersionSufficient(sdk)) return;

        DartAnalysisServerService.getInstance().updateFilesContent();

        final String completionId =
          DartAnalysisServerService.getInstance().completion_getSuggestions(file.getPath(), parameters.getOffset());
        if (completionId == null) return;

        final String uriPrefix = getPrefixIfCompletingUri(parameters);
        final CompletionResultSet resultSet = uriPrefix != null ? originalResultSet.withPrefixMatcher(uriPrefix) : originalResultSet;

        DartAnalysisServerService.getInstance().addCompletions(completionId, new Consumer<CompletionSuggestion>() {
          @Override
          public void consume(CompletionSuggestion suggestion) {
            final LookupElement lookupElement = createLookupElement(project, suggestion);
            resultSet.addElement(lookupElement);
          }
        });
      }
    });
  }

  @Nullable
  private static String getPrefixIfCompletingUri(@NotNull final CompletionParameters parameters) {
    final PsiElement psiElement = parameters.getOriginalPosition();
    final PsiElement parent = psiElement != null ? psiElement.getParent() : null;
    final PsiElement parentParent = parent instanceof DartStringLiteralExpression ? parent.getParent() : null;
    final PsiElement parentParentParent = parentParent instanceof DartUriElement ? parentParent.getParent() : null;
    if (parentParentParent instanceof DartUriBasedDirective) {
      final int uriStringOffset = ((DartUriBasedDirective)parentParentParent).getUriStringOffset();
      if (parameters.getOffset() >= parentParent.getTextOffset() + uriStringOffset) {
        return parentParent.getText().substring(uriStringOffset, parameters.getOffset() - parentParent.getTextOffset());
      }
    }
    return null;
  }

  @Override
  public void beforeCompletion(@NotNull final CompletionInitializationContext context) {
    final PsiElement psiElement = context.getFile().findElementAt(context.getStartOffset());
    final PsiElement parent = psiElement != null ? psiElement.getParent() : null;
    final PsiElement parentParent = parent instanceof DartStringLiteralExpression ? parent.getParent() : null;
    final PsiElement parentParentParent = parentParent instanceof DartUriElement ? parentParent.getParent() : null;
    if (parentParentParent instanceof DartUriBasedDirective) {
      final String uri = ((DartUriBasedDirective)parentParentParent).getUriString();
      final int uriOffset = ((DartUriBasedDirective)parentParentParent).getUriStringOffset();
      context.setReplacementOffset(parentParent.getTextOffset() + uriOffset + uri.length());
    }
  }

  private static Icon applyOverlay(Icon base, boolean condition, Icon overlay) {
    if (condition) {
      return new LayeredIcon(base, overlay);
    }
    return base;
  }

  private static Icon applyVisibility(Icon base, boolean isPrivate) {
    RowIcon result = new RowIcon(2);
    result.setIcon(base, 0);
    Icon visibility = isPrivate ? PlatformIcons.PRIVATE_ICON : PlatformIcons.PUBLIC_ICON;
    result.setIcon(visibility, 1);
    return result;
  }

  private static LookupElement createLookupElement(@NotNull final Project project, @NotNull final CompletionSuggestion suggestion) {
    final Element element = suggestion.getElement();
    final Location location = element == null ? null : element.getLocation();
    final DartLookupObject lookupObject = new DartLookupObject(project, location, suggestion.getRelevance());

    LookupElementBuilder lookup = LookupElementBuilder.create(lookupObject, suggestion.getCompletion());

    // keywords are bold
    if (suggestion.getKind().equals(CompletionSuggestionKind.KEYWORD)) {
      lookup = lookup.bold();
    }

    boolean shouldSetSelection = true;
    if (element != null) {
      // @deprecated
      if (element.isDeprecated()) {
        lookup = lookup.strikeout();
      }
      // append type parameters
      final String typeParameters = element.getTypeParameters();
      if (typeParameters != null) {
        lookup = lookup.appendTailText(typeParameters, false);
      }
      // append parameters
      final String parameters = element.getParameters();
      if (parameters != null) {
        lookup = lookup.appendTailText(parameters, false);
      }
      // append return type
      final String returnType = element.getReturnType();
      if (!StringUtils.isEmpty(returnType)) {
        lookup = lookup.withTypeText(returnType, true);
      }
      // icon
      Icon icon = getBaseImage(element);
      if (icon != null) {
        icon = applyVisibility(icon, element.isPrivate());
        icon = applyOverlay(icon, element.isFinal(), AllIcons.Nodes.FinalMark);
        icon = applyOverlay(icon, element.isConst(), AllIcons.Nodes.FinalMark);
        lookup = lookup.withIcon(icon);
      }
      // Prepare for typing arguments, if any.
      if (CompletionSuggestionKind.INVOCATION.equals(suggestion.getKind())) {
        shouldSetSelection = false;
        final List<String> parameterNames = suggestion.getParameterNames();
        if (parameterNames != null) {
          lookup = lookup.withInsertHandler(new InsertHandler<LookupElement>() {
            @Override
            public void handleInsert(InsertionContext context, LookupElement item) {
              if (parameterNames.isEmpty()) {
                ParenthesesInsertHandler.NO_PARAMETERS.handleInsert(context, item);
              }
              else {
                ParenthesesInsertHandler.WITH_PARAMETERS.handleInsert(context, item);
                // Show parameters popup.
                final Editor editor = context.getEditor();
                final PsiElement psiElement = lookupObject.getElement();
                AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, psiElement);
              }
            }
          });
        }
      }
    }

    // Use selection offset / length.
    if (shouldSetSelection) {
      lookup = lookup.withInsertHandler(new InsertHandler<LookupElement>() {
        @Override
        public void handleInsert(InsertionContext context, LookupElement item) {
          final Editor editor = context.getEditor();
          final int startOffset = context.getStartOffset() + suggestion.getSelectionOffset();
          final int endOffset = startOffset + suggestion.getSelectionLength();
          editor.getCaretModel().moveToOffset(startOffset);
          if (endOffset > startOffset) {
            editor.getSelectionModel().setSelection(startOffset, endOffset);
          }
        }
      });
    }

    return lookup;
  }

  private static Icon getBaseImage(Element element) {
    final String elementKind = element.getKind();
    if (elementKind.equals(ElementKind.CLASS) || elementKind.equals(ElementKind.CLASS_TYPE_ALIAS)) {
      if (element.isAbstract()) {
        return AllIcons.Nodes.AbstractClass;
      }
      return AllIcons.Nodes.Class;
    }
    else if (elementKind.equals(ElementKind.ENUM)) {
      return AllIcons.Nodes.Enum;
    }
    else if (elementKind.equals(ElementKind.ENUM_CONSTANT) || elementKind.equals(ElementKind.FIELD)) {
      return AllIcons.Nodes.Field;
    }
    else if (elementKind.equals(ElementKind.COMPILATION_UNIT)) {
      return PlatformIcons.FILE_ICON;
    }
    else if (elementKind.equals(ElementKind.CONSTRUCTOR)) {
      return AllIcons.Nodes.ClassInitializer;
    }
    else if (elementKind.equals(ElementKind.GETTER)) {
      return element.isTopLevelOrStatic() ? AllIcons.Nodes.PropertyReadStatic : AllIcons.Nodes.PropertyRead;
    }
    else if (elementKind.equals(ElementKind.SETTER)) {
      return element.isTopLevelOrStatic() ? AllIcons.Nodes.PropertyWriteStatic : AllIcons.Nodes.PropertyWrite;
    }
    else if (elementKind.equals(ElementKind.METHOD)) {
      if (element.isAbstract()) {
        return AllIcons.Nodes.AbstractMethod;
      }
      return AllIcons.Nodes.Method;
    }
    else if (elementKind.equals(ElementKind.FUNCTION)) {
      return AllIcons.Nodes.Function;
    }
    else if (elementKind.equals(ElementKind.FUNCTION_TYPE_ALIAS)) {
      return AllIcons.Nodes.Annotationtype;
    }
    else if (elementKind.equals(ElementKind.TOP_LEVEL_VARIABLE)) {
      return AllIcons.Nodes.Variable;
    }
    else {
      return null;
    }
  }
}
