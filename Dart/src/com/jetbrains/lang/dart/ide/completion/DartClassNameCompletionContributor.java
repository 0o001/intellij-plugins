package com.jetbrains.lang.dart.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Condition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.lang.dart.DartComponentType;
import com.jetbrains.lang.dart.ide.index.DartComponentInfo;
import com.jetbrains.lang.dart.psi.ClassNameScopeProcessor;
import com.jetbrains.lang.dart.psi.DartComponentName;
import com.jetbrains.lang.dart.psi.DartId;
import com.jetbrains.lang.dart.psi.DartNormalFormalParameter;
import com.jetbrains.lang.dart.util.DartResolveUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author: Fedor.Korotkov
 */
public class DartClassNameCompletionContributor extends CompletionContributor {
  public DartClassNameCompletionContributor() {
    final PsiElementPattern.Capture<PsiElement> idInComponentName =
      psiElement().withSuperParent(1, DartId.class).withSuperParent(2, DartComponentName.class);
    extend(CompletionType.BASIC,
           idInComponentName.withSuperParent(3, DartNormalFormalParameter.class),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final Set<DartComponentName> suggestedVariants = new THashSet<DartComponentName>();
               final ClassNameScopeProcessor processor = new ClassNameScopeProcessor(suggestedVariants);

               DartResolveUtil.treeWalkUpAndTopLevelDeclarations(parameters.getPosition(), processor);

               for (DartComponentName variant : suggestedVariants) {
                 result.addElement(LookupElementBuilder.create(variant));
               }
               if (parameters.getInvocationCount() > 1) {
                 DartGlobalVariantsCompletionHelper.addAdditionalGlobalVariants(
                   result, parameters.getPosition(), suggestedVariants,
                   new Condition<DartComponentInfo>() {
                     @Override
                     public boolean value(DartComponentInfo info) {
                       return info.getType() == DartComponentType.CLASS || info.getType() == DartComponentType.INTERFACE;
                     }
                   }
                 );
               }
             }
           });
  }
}
