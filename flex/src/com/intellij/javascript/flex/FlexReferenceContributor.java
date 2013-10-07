package com.intellij.javascript.flex;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.javascript.flex.css.CssClassValueReference;
import com.intellij.javascript.flex.css.CssPropertyValueReference;
import com.intellij.javascript.flex.css.FlexCssPropertyDescriptor;
import com.intellij.javascript.flex.css.FlexCssUtil;
import com.intellij.javascript.flex.mxml.FlexCommonTypeNames;
import com.intellij.javascript.flex.mxml.MxmlJSClass;
import com.intellij.javascript.flex.mxml.schema.AnnotationBackedDescriptorImpl;
import com.intellij.javascript.flex.mxml.schema.CodeContext;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.flex.AnnotationBackedDescriptor;
import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.lang.javascript.flex.ReferenceSupport;
import com.intellij.lang.javascript.flex.actions.newfile.CreateFlexComponentFix;
import com.intellij.lang.javascript.flex.actions.newfile.CreateFlexSkinIntention;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecmal4.JSAttribute;
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeNameValuePair;
import com.intellij.lang.javascript.psi.impl.JSReferenceSet;
import com.intellij.lang.javascript.psi.impl.JSTextReference;
import com.intellij.lang.javascript.psi.util.JSUtils;
import com.intellij.lang.javascript.validation.fixes.CreateClassIntentionWithCallback;
import com.intellij.lang.javascript.validation.fixes.CreateClassOrInterfaceFix;
import com.intellij.lang.javascript.validation.fixes.CreateFlexMobileViewIntentionAndFix;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.css.*;
import com.intellij.psi.css.util.CssConstants;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.AttributeValueSelfReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.BasicAttributeValueReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileBasedUserDataCache;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.*;
import com.intellij.util.text.StringTokenizer;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.util.XmlTagUtil;
import com.intellij.xml.util.XmlUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.flex.build.FlexCompilerConfigFileUtilBase.PATH_ELEMENT;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.patterns.XmlPatterns.*;

/**
 * @author yole
 */
public class FlexReferenceContributor extends PsiReferenceContributor {
  private static final @NonNls String BINDING_TAG_NAME = "Binding";
  @NonNls private static final String CLASS_TAG_NAME = "class";
  @NonNls private static final String TRANSITION_TAG_NAME = "Transition";
  @NonNls public static final String SOURCE_ATTR_NAME = "source";
  @NonNls public static final String FORMAT_ATTR_NAME = "format";
  @NonNls public static final String FILE_ATTR_VALUE = "File";
  @NonNls public static final String DESTINATION_ATTR_NAME = "destination";
  @NonNls public static final String SET_STYLE_METHOD_NAME = "setStyle";
  private static final String DELIMS = ", ";
  private static final String STYLE_NAME_ATTR_SUFFIX = "StyleName";
  private static final String STYLE_NAME_ATTR = "styleName";
  private static final String SKIN_CLASS_ATTR_NAME = "skinClass";
  private static final String UI_COMPONENT_FQN = "mx.core.UIComponent";
  public static final String CLASS_REFERENCE = "ClassReference";

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(CssString.class).and(new FilterPattern(new ElementFilter() {
      public boolean isAcceptable(Object element, PsiElement context) {
        CssFunction fun = PsiTreeUtil.getParentOfType((PsiElement)element, CssFunction.class);
        String funName;
        return fun != null && (CLASS_REFERENCE.equals(funName = fun.getName()) || "Embed".equals(funName));
      }

      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    })), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull ProcessingContext context) {
        CssFunction fun = PsiTreeUtil.getParentOfType(element, CssFunction.class);
        if (fun != null && "Embed".equals(fun.getName())) {
          // TODO: remove this stuff once css function will have proper psi
          PsiElement prev = PsiTreeUtil.prevLeaf(element);
          if (prev instanceof PsiWhiteSpace) prev = PsiTreeUtil.prevLeaf(prev);
          if (prev != null) prev = PsiTreeUtil.prevLeaf(prev);
          if (prev instanceof PsiWhiteSpace) prev = PsiTreeUtil.prevLeaf(prev);
          // prev.getText() == Embed if element is the first parameter and the name not specified
          if (prev != null && !SOURCE_ATTR_NAME.equals(prev.getText()) && !"Embed".equals(prev.getText())) {
            return PsiReference.EMPTY_ARRAY;
          }
          return ReferenceSupport.getFileRefs(element, element, 1, ReferenceSupport.LookupOptions.EMBEDDED_ASSET);
        }
        final String value = StringUtil.stripQuotesAroundValue(element.getText());
        JSReferenceSet refSet = new JSReferenceSet(element, value, 1, false, false, true);
        if (fun != null && element instanceof CssString) {
          assert CLASS_REFERENCE.equals(fun.getName());
          refSet.setLocalQuickFixProvider(new LocalQuickFixProvider() {
            @Nullable
            @Override
            public LocalQuickFix[] getQuickFixes() {
              if (!JSUtils.isValidClassName(value, true)) {
                return LocalQuickFix.EMPTY_ARRAY;
              }

              CreateClassOrInterfaceFix[] fixes = new CreateClassOrInterfaceFix[]{
                new CreateClassOrInterfaceFix(value, null, element),
                new CreateFlexComponentFix(value, element)
              };
              for (CreateClassOrInterfaceFix fix : fixes) {
                fix.setCreatedClassFqnConsumer(new Consumer<String>() {
                  @Override
                  public void consume(final String newFqn) {
                    ElementManipulators.getManipulator(element).handleContentChange(element, newFqn);
                  }
                });
              }
              return fixes;
            }
          });
        }
        return refSet.getReferences();
      }
    });
    registrar.registerReferenceProvider(PlatformPatterns.psiElement().and(new FilterPattern(new ElementFilter() {
      public boolean isAcceptable(Object element, PsiElement context) {
        if (element instanceof XmlToken || element instanceof CssString) {
          CssTermList cssTermList = PsiTreeUtil.getParentOfType((PsiElement)element, CssTermList.class);
          if (cssTermList != null) {
            CssDeclaration cssDeclaration = PsiTreeUtil.getParentOfType(cssTermList, CssDeclaration.class);
            if (cssDeclaration != null && cssDeclaration.getValue() == cssTermList) {
              if (FlexCssUtil.isStyleNameProperty(cssDeclaration.getPropertyName())) {
                PsiFile file = cssDeclaration.getContainingFile();
                if (file != null) {
                  if (file.getFileType() == CssFileType.INSTANCE) {
                    Module module = findModuleForPsiElement(cssDeclaration);
                    return module != null && ModuleType.get(module) == FlexModuleType.getInstance();
                  }
                  return JavaScriptSupportLoader.isFlexMxmFile(file);
                }
              }
            }
          }
        }
        return false;
      }

      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    })), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        String styleName = CssClassValueReference.getValue(element);
        if (styleName.length() > 0) {
          return new PsiReference[]{new CssClassValueReference(element)};
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    registrar.registerReferenceProvider(PlatformPatterns.psiElement(JSLiteralExpression.class).and(new FilterPattern(new ElementFilter() {
      public boolean isAcceptable(Object element, PsiElement context) {
        PsiElement parent = ((JSLiteralExpression)element).getParent();
        if (parent instanceof JSArgumentList) {
          JSExpression[] arguments = ((JSArgumentList)parent).getArguments();
          if (arguments != null && arguments[0] == element) {
            parent = parent.getParent();
            if (parent instanceof JSCallExpression) {
              JSExpression invokedMethod = ((JSCallExpression)parent).getMethodExpression();
              if (invokedMethod instanceof JSReferenceExpression) {
                String methodName = ((JSReferenceExpression)invokedMethod).getReferencedName();
                if (SET_STYLE_METHOD_NAME.equals(methodName)) {
                  Module module = findModuleForPsiElement(parent);
                  return module != null && ModuleType.get(module) == FlexModuleType.getInstance();
                }
              }
            }
          }
        }
        return false;
      }

      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    })), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        String value = element.getText();
        if (FlexCssUtil.inQuotes(value)) {
          return new PsiReference[]{new CssPropertyValueReference(element)};
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    registrar.registerReferenceProvider(PlatformPatterns.psiElement(JSLiteralExpression.class).and(new FilterPattern(new ElementFilter() {
      public boolean isAcceptable(Object element, PsiElement context) {
        PsiElement parent = ((JSLiteralExpression)element).getParent();
        if (parent instanceof JSAssignmentExpression) {
          PsiElement assignee = parent.getChildren()[0];
          if (assignee instanceof JSDefinitionExpression) {
            JSExpression expression = ((JSDefinitionExpression)assignee).getExpression();
            if (expression instanceof JSReferenceExpression) {
              String refName = ((JSReferenceExpression)expression).getReferencedName();
              if (refName != null && FlexCssUtil.isStyleNameProperty(refName)) {
                Module module = findModuleForPsiElement(parent);
                return module != null && ModuleType.get(module) == FlexModuleType.getInstance();
              }
            }
          }
        }
        return false;
      }

      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    })), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        String value = element.getText();
        if (FlexCssUtil.inQuotes(value)) {
          return new PsiReference[]{new CssClassValueReference(element)};
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    registrar.registerReferenceProvider(PlatformPatterns.psiElement(JSLiteralExpression.class).and(new FilterPattern(new ElementFilter() {
      public boolean isAcceptable(Object element, PsiElement context) {
        PsiElement parent = ((JSLiteralExpression)element).getParent();
        if (parent instanceof JSArgumentList) {
          final JSExpression[] arguments = ((JSArgumentList)parent).getArguments();
          if (arguments != null && arguments.length > 0 && arguments[0] == element) {
            parent = parent.getParent();
            if (parent instanceof JSCallExpression) {
              final JSExpression invokedMethod = ((JSCallExpression)parent).getMethodExpression();
              if (invokedMethod instanceof JSReferenceExpression) {
                final String methodName = ((JSReferenceExpression)invokedMethod).getReferencedName();
                if (methodName != null && FlexCssUtil.isStyleNameMethod(methodName)) {
                  Module module = findModuleForPsiElement(parent);
                  return module != null && ModuleType.get(module) == FlexModuleType.getInstance();
                }
              }
            }
          }
        }
        return false;
      }

      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    })), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        String value = element.getText();
        if (FlexCssUtil.inQuotes(value)) {
          return new PsiReference[]{new CssClassValueReference(element)};
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    final PsiReferenceProvider cssReferenceProvider = CssConstants.CSS_CLASS_OR_ID_KEY_PROVIDER.getProvider();

    registrar.registerReferenceProvider(
      XmlPatterns.xmlAttributeValue().withLocalName(StandardPatterns.or(StandardPatterns.string().endsWith(STYLE_NAME_ATTR_SUFFIX),
                                                                        StandardPatterns.string().equalTo(STYLE_NAME_ATTR)))
        .and(new FilterPattern(new ElementFilter() {
          public boolean isAcceptable(final Object element, final PsiElement context) {
            PsiElement psiElement = (PsiElement)element;
            final PsiFile containingFile = psiElement.getContainingFile();
            return JavaScriptSupportLoader.isFlexMxmFile(containingFile) && !psiElement.textContains('{');
          }

          public boolean isClassAcceptable(final Class hintClass) {
            return true;
          }
        })),
      cssReferenceProvider, PsiReferenceRegistrar.DEFAULT_PRIORITY);

    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, null, new ElementFilter() {
      @Override
      public boolean isAcceptable(Object element, PsiElement context) {
        PsiElement parent = ((PsiElement)element).getParent();
        if (parent instanceof XmlAttribute) {
          if (JavaScriptSupportLoader.isFlexMxmFile(parent.getContainingFile())) {
            XmlAttributeDescriptor descriptor = ((XmlAttribute)parent).getDescriptor();
            if (descriptor instanceof AnnotationBackedDescriptorImpl) {
              String format = ((AnnotationBackedDescriptor)descriptor).getFormat();
              return FlexCssPropertyDescriptor.COLOR_FORMAT.equals(format);
            }
          }
        }
        return false;
      }

      @Override
      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    }, true, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull ProcessingContext context) {
        XmlAttributeValue value = (XmlAttributeValue)element;
        XmlAttribute parent = (XmlAttribute)value.getParent();
        int length = value.getTextLength();
        if (length >= 2) {
          AnnotationBackedDescriptor descriptor = (AnnotationBackedDescriptor)parent.getDescriptor();
          assert descriptor != null;
          if (JSCommonTypeNames.ARRAY_CLASS_NAME.equals(descriptor.getType())) {
            // drop quotes
            String text = element.getText().substring(1, length - 1);
            final List<PsiReference> references = new ArrayList<PsiReference>();
            new ArrayAttributeValueProcessor() {
              @Override
              protected void processElement(int start, int end) {
                references.add(new FlexColorReference(element, new TextRange(start + 1, end + 1)));
              }
            }.process(text);
            return references.toArray(new PsiReference[references.size()]);
          }
          else {
            // inside quotes
            return new PsiReference[]{new FlexColorReference(element, new TextRange(1, length - 1))};
          }
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, null, new ElementFilter() {
      public boolean isAcceptable(final Object element, final PsiElement context) {
        PsiElement parent = ((PsiElement)element).getParent();
        if (!(parent instanceof XmlAttribute) || !((XmlAttribute)parent).isNamespaceDeclaration()) {
          return false;
        }

        final PsiFile containingFile = ((PsiElement)element).getContainingFile();
        if (!JavaScriptSupportLoader.isFlexMxmFile(containingFile)) {
          return false;
        }

        final PsiElement parentParent = parent.getParent();
        if (parentParent instanceof XmlTag && MxmlJSClass.isInsideTagThatAllowsAnyXmlContent((XmlTag)parentParent)) {
          return false;
        }

        return true;
      }

      public boolean isClassAcceptable(final Class hintClass) {
        return true;
      }
    }, true, new PsiReferenceProvider() {
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        final String trimmedText = StringUtil.stripQuotesAroundValue(element.getText());
        if (CodeContext.isPackageBackedNamespace(trimmedText)) {
          final JSReferenceSet referenceSet = new JSReferenceSet(element, trimmedText, 1, false, true, false);
          return referenceSet.getReferences();
        }
        else {
          return PsiReference.EMPTY_ARRAY;
        }
      }
    });

    // source attribute of Binding tag is handled in JSLanguageInjector
    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{DESTINATION_ATTR_NAME}, new ScopeFilter(
      new ParentElementFilter(new AndFilter(XmlTagFilter.INSTANCE, new TagNameFilter(BINDING_TAG_NAME),
                                            new NamespaceFilter(JavaScriptSupportLoader.LANGUAGE_NAMESPACES)), 2)),
                                                       new PsiReferenceProvider() {
                                                         @NotNull
                                                         public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                                                                      @NotNull final ProcessingContext context) {
                                                           final String trimmedText = StringUtil.stripQuotesAroundValue(element.getText());
                                                           final JSReferenceSet referenceSet =
                                                             new JSReferenceSet(element, trimmedText, 1, false);
                                                           return referenceSet.getReferences();
                                                         }
                                                       });

    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{SOURCE_ATTR_NAME}, new ScopeFilter(
      new ParentElementFilter(new AndFilter(XmlTagFilter.INSTANCE, new ElementFilterBase<PsiElement>(PsiElement.class) {
        protected boolean isElementAcceptable(final PsiElement element, final PsiElement context) {
          return JavaScriptSupportLoader.isFlexMxmFile(element.getContainingFile());
        }
      }), 2)), new PsiReferenceProvider() {
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        final XmlAttribute attribute = (XmlAttribute)element.getParent();
        final XmlTag tag = attribute.getParent();
        final String tagName = tag.getLocalName();

        final String trimmedText = StringUtil.stripQuotesAroundValue(element.getText());

        if (JavaScriptSupportLoader.isLanguageNamespace(tag.getNamespace())) {
          if (FlexPredefinedTagNames.SCRIPT.equals(tagName)) {
            return ReferenceSupport.getFileRefs(element, element, 1, ReferenceSupport.LookupOptions.SCRIPT_SOURCE);
          }

          final String[] tagsWithSourceAttr = {
            MxmlJSClass.XML_TAG_NAME, FlexPredefinedTagNames.MODEL,
            JSCommonTypeNames.STRING_CLASS_NAME, JSCommonTypeNames.BOOLEAN_CLASS_NAME,
            JSCommonTypeNames.INT_TYPE_NAME, JSCommonTypeNames.UINT_TYPE_NAME, JSCommonTypeNames.NUMBER_CLASS_NAME
          };

          if (ArrayUtil.contains(tagName, tagsWithSourceAttr)) {
            return ReferenceSupport.getFileRefs(element, element, 1, ReferenceSupport.LookupOptions.XML_AND_MODEL_SOURCE);
          }

          if (FlexPredefinedTagNames.STYLE.equals(tagName)) {
            if (trimmedText.startsWith("http:")) {
              return PsiReference.EMPTY_ARRAY;
            }
            else {
              return ReferenceSupport.getFileRefs(element, element, 1, ReferenceSupport.LookupOptions.STYLE_SOURCE);
            }
          }
        }

        if (element.textContains('{') || element.textContains('@')) {
          return PsiReference.EMPTY_ARRAY;
        }

        final XmlAttributeDescriptor descriptor = attribute.getDescriptor();
        final PsiElement psiElement = descriptor == null ? null : descriptor.getDeclaration();

        if (psiElement instanceof JSFunction) {
          final JSAttribute inspectableAttr = AnnotationBackedDescriptorImpl.findInspectableAttr(psiElement);
          if (inspectableAttr != null) {
            final JSAttributeNameValuePair attributeNameValuePair = inspectableAttr.getValueByName(FORMAT_ATTR_NAME);
            if (attributeNameValuePair != null && FILE_ATTR_VALUE.equals(attributeNameValuePair.getSimpleValue())) {
              return ReferenceSupport.getFileRefs(element, element, 1, ReferenceSupport.LookupOptions.NON_EMBEDDED_ASSET);
            }
          }
        }

        return PsiReference.EMPTY_ARRAY;
      }
    });

    final ElementFilter mxmlElementFilter = new ElementFilter() {
      public boolean isAcceptable(final Object element, final PsiElement context) {
        return JavaScriptSupportLoader.isFlexMxmFile(((PsiElement)element).getContainingFile());
      }

      public boolean isClassAcceptable(final Class hintClass) {
        return true;
      }
    };

    final Function<PsiReference, LocalQuickFix[]> quickFixProvider = new Function<PsiReference, LocalQuickFix[]>() {
      @Nullable
      @Override
      public LocalQuickFix[] fun(PsiReference reference) {
        final PsiElement element = reference.getElement();

        final String classFqn = getTrimmedValueAndRange((XmlElement)element).first;
        final String tagOrAttrName = element instanceof XmlAttributeValue
                                     ? ((XmlAttribute)element.getParent()).getName()
                                     : ((XmlTag)element).getLocalName();


        final CreateClassIntentionWithCallback[] intentions;
        if (SKIN_CLASS_ATTR_NAME.equals(tagOrAttrName)) {
          intentions = new CreateClassIntentionWithCallback[] {new CreateFlexSkinIntention(classFqn, element)};
        }
        else if ("firstView".equals(tagOrAttrName)) {
          intentions = new CreateClassIntentionWithCallback[] {new CreateFlexMobileViewIntentionAndFix(classFqn, element, false)};
        }
        else {
          intentions = new CreateClassIntentionWithCallback[]{
            new CreateClassOrInterfaceFix(classFqn, null, element),
            new CreateFlexComponentFix(classFqn, element)
          };
        }

        for (CreateClassIntentionWithCallback intention : intentions) {
          intention.setCreatedClassFqnConsumer(new Consumer<String>() {
            @Override
            public void consume(final String fqn) {
              if (!element.isValid()) return;

              if (element instanceof XmlAttributeValue) {
                ((XmlAttribute)element.getParent()).setValue(fqn);
              }
              else {
                ((XmlTag)element).getValue().setText(fqn);
              }
            }
          });
        }
        return intentions;
      }
    };

    XmlUtil.registerXmlTagReferenceProvider(registrar, null, mxmlElementFilter, true,
                                            createReferenceProviderForTagOrAttributeExpectingJSClass(quickFixProvider));

    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, null, mxmlElementFilter,
                                                       createReferenceProviderForTagOrAttributeExpectingJSClass(quickFixProvider));

    registrar.registerReferenceProvider(xmlAttribute().withParent(XmlTag.class).with(new PatternCondition<XmlAttribute>("") {
      @Override
      public boolean accepts(@NotNull XmlAttribute xmlAttribute, ProcessingContext context) {
        String attrName = xmlAttribute.getLocalName();
        int dotPos = attrName.indexOf('.');
        if (dotPos == -1) return false;
        return JavaScriptSupportLoader.isFlexMxmFile(xmlAttribute.getContainingFile());
      }
    }), new PsiReferenceProvider() {
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        String attrName = ((XmlAttribute)element).getLocalName();
        int dotPos = attrName.indexOf('.');
        if (dotPos == -1) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new StateReference(element, new TextRange(dotPos + 1, attrName.length()))};
      }
    });

    XmlUtil.registerXmlTagReferenceProvider(registrar, null, new ElementFilterBase<XmlTag>(XmlTag.class) {
                                              protected boolean isElementAcceptable(final XmlTag element, final PsiElement context) {
                                                return element.getName().indexOf('.') != -1 &&
                                                       JavaScriptSupportLoader.isFlexMxmFile(element.getContainingFile());
                                              }
                                            }, false, new PsiReferenceProvider() {
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        final String name = ((XmlTag)element).getName();
        int dotIndex = name.indexOf('.');
        if (dotIndex == -1) return PsiReference.EMPTY_ARRAY;

        final int tagOffset = element.getTextRange().getStartOffset();
        final XmlToken startTagElement = XmlTagUtil.getStartTagNameElement((XmlTag)element);
        final XmlToken endTagElement = XmlTagUtil.getEndTagNameElement((XmlTag)element);
        if (startTagElement != null) {
          if (endTagElement != null && endTagElement.getText().equals(startTagElement.getText())) {
            final int start1 = startTagElement.getTextRange().getStartOffset() - tagOffset;
            final int start2 = endTagElement.getTextRange().getStartOffset() - tagOffset;
            return new PsiReference[]{
              new StateReference(element, new TextRange(start1 + dotIndex + 1, startTagElement.getTextRange().getEndOffset() - tagOffset)),
              new StateReference(element, new TextRange(start2 + dotIndex + 1, endTagElement.getTextRange().getEndOffset() - tagOffset)),
            };
          }
          else {
            final int start = startTagElement.getTextRange().getStartOffset() - tagOffset;
            return new PsiReference[]{
              new StateReference(element, new TextRange(start + dotIndex + 1, startTagElement.getTextRange().getEndOffset() - tagOffset))};
          }
        }

        return PsiReference.EMPTY_ARRAY;
      }
    }
    );

    XmlUtil
      .registerXmlAttributeValueReferenceProvider(registrar, new String[]{"basedOn", "fromState", "toState", FlexStateElementNames.NAME, FlexStateElementNames.STATE_GROUPS},
                                                  new ScopeFilter(new ParentElementFilter(new AndFilter(XmlTagFilter.INSTANCE,
                                                                                                        new NamespaceFilter(
                                                                                                          MxmlJSClass.MXML_URIS)),
                                                                                          2)), new PsiReferenceProvider() {
          @NotNull
          public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
            final PsiElement parent = element.getParent();
            final PsiElement tag = parent.getParent();

            PsiReference ref = null;
            String tagName = ((XmlTag)tag).getLocalName();
            String attrName = ((XmlAttribute)parent).getName();
            String attrValue = ((XmlAttribute)parent).getValue();

            if (attrValue != null && attrValue.contains("{")) return PsiReference.EMPTY_ARRAY;

            if (FlexStateElementNames.NAME.equals(attrName)) {
              if ("State".equals(tagName)) {
                ref = new AttributeValueSelfReference(element);
              }
              else {
                return PsiReference.EMPTY_ARRAY;
              }
            }
            else if ("basedOn".equals(attrName) && element.getTextLength() == 2) {
              return PsiReference.EMPTY_ARRAY;
            }
            else if (FlexStateElementNames.STATE_GROUPS.equals(attrName)) {
              if ("State".equals(tagName)) {
                return buildStateRefs(element, true);
              }
              else {
                return PsiReference.EMPTY_ARRAY;
              }
            }

            if (TRANSITION_TAG_NAME.equals(tagName)) {
              if ((element.textContains('*') &&
                   "*".equals(StringUtil.stripQuotesAroundValue(element.getText()))) ||
                  element.getTextLength() == 2 // empty value for attr, current state
                 ) {
                return PsiReference.EMPTY_ARRAY;
              }
            }

            if (ref == null) {
              ref = new StateReference(element);
            }

            return new PsiReference[]{ref};
          }
        });

    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{FlexStateElementNames.EXCLUDE_FROM, FlexStateElementNames.INCLUDE_IN},
                                                       new ScopeFilter(new ParentElementFilter(new AndFilter(XmlTagFilter.INSTANCE,
                                                                                                             new ElementFilterBase<PsiElement>(
                                                                                                               PsiElement.class) {
                                                                                                               protected boolean isElementAcceptable(
                                                                                                                 final PsiElement element,
                                                                                                                 final PsiElement context) {
                                                                                                                 return JavaScriptSupportLoader
                                                                                                                   .isFlexMxmFile(
                                                                                                                     element.getContainingFile());
                                                                                                               }
                                                                                                             }), 2)),
                                                       new PsiReferenceProvider() {
                                                         @NotNull
                                                         public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                                                                      @NotNull final ProcessingContext context) {

                                                           return buildStateRefs(element, false);
                                                         }
                                                       });

    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{CodeContext.TARGET_ATTR_NAME},
                                                       new ScopeFilter(
                                                         new ParentElementFilter(
                                                           new AndFilter(XmlTagFilter.INSTANCE,
                                                                         new TagNameFilter(CodeContext.REPARENT_TAG_NAME),
                                                                         new NamespaceFilter(JavaScriptSupportLoader.MXML_URI3),
                                                                         new ElementFilterBase<PsiElement>(PsiElement.class) {
                                                                           protected boolean isElementAcceptable(final PsiElement element,
                                                                                                                 final PsiElement context) {
                                                                             return JavaScriptSupportLoader
                                                                               .isFlexMxmFile(element.getContainingFile());
                                                                           }
                                                                         }),
                                                           2)),
                                                       new PsiReferenceProvider() {
                                                         @NotNull
                                                         public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                                                                      @NotNull final ProcessingContext context) {
                                                           return new PsiReference[]{new XmlIdValueReference(element)};
                                                         }
                                                       });

    XmlUtil.registerXmlTagReferenceProvider(registrar, new String[]{PATH_ELEMENT, CLASS_TAG_NAME, "classname", "symbol"}, new ElementFilter() {
      public boolean isAcceptable(final Object element, final PsiElement context) {
        return FlexApplicationComponent.HTTP_WWW_ADOBE_COM_2006_FLEX_CONFIG.equals(((XmlTag)element).getNamespace());
      }

      public boolean isClassAcceptable(final Class hintClass) {
        return true;
      }
    }, true, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        TextRange myRange = ElementManipulators.getValueTextRange(element);
        if (myRange.getStartOffset() == 0) return PsiReference.EMPTY_ARRAY;
        XmlTag tag = (XmlTag)element;
        final String trimmed = tag.getValue().getTrimmedText();
        if (trimmed.indexOf('{') != -1) return PsiReference.EMPTY_ARRAY;

        if (PATH_ELEMENT.equals(tag.getLocalName())) {
          return ReferenceSupport
            .getFileRefs(element, myRange.getStartOffset(), trimmed, ReferenceSupport.LookupOptions.FLEX_COMPILER_CONFIG_PATH_ELEMENT);
        }

        return new JSReferenceSet(element, trimmed, myRange.getStartOffset(), false, false, true).getReferences();
      }
    });

    registrar.registerReferenceProvider(
      xmlAttributeValue(xmlAttribute("class").withParent(xmlTag().withName("component").withParent(xmlTag().withName("componentPackage")))),
      new PsiReferenceProvider() {
        @NotNull
        public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
          TextRange myRange = ElementManipulators.getValueTextRange(element);
          if (myRange.getStartOffset() == 0) return PsiReference.EMPTY_ARRAY;
          final String attrValue = ((XmlAttributeValue)element).getValue();
          return new JSReferenceSet(element, attrValue, myRange.getStartOffset(), false, false, true).getReferences();
        }
      });

    registrar.registerReferenceProvider(psiElement(JSLiteralExpression.class), new FlexPropertyReferenceProvider());
    registrar.registerReferenceProvider(psiElement(JSAttributeNameValuePair.class), new FlexAttributeReferenceProvider());
  }

  private static PsiReferenceProvider createReferenceProviderForTagOrAttributeExpectingJSClass(final Function<PsiReference, LocalQuickFix[]> quickFixProvider) {
    return new PsiReferenceProvider() {
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                   @NotNull final ProcessingContext context) {
        final PsiMetaData descriptor;
        final String name;

        if (element instanceof XmlTag) {
          descriptor = ((XmlTag)element).getDescriptor();
          name = ((XmlTag)element).getLocalName();
        }
        else if (element instanceof XmlAttributeValue) {
          final XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(element, XmlAttribute.class);
          descriptor = xmlAttribute == null ? null : xmlAttribute.getDescriptor();
          name = xmlAttribute == null ? "" : xmlAttribute.getName();
        }
        else {
          assert false : element;
          return PsiReference.EMPTY_ARRAY;
        }

        if (!(descriptor instanceof AnnotationBackedDescriptor)) return PsiReference.EMPTY_ARRAY;

        final String type = ((AnnotationBackedDescriptor)descriptor).getType();
        if (!isClassReferenceType(type)) return PsiReference.EMPTY_ARRAY;

        final Pair<String, TextRange> trimmedValueAndRange = getTrimmedValueAndRange((XmlElement)element);
        if (trimmedValueAndRange.second.getStartOffset() == 0) return PsiReference.EMPTY_ARRAY;
        if (trimmedValueAndRange.first.indexOf('{') != -1 || trimmedValueAndRange.first.indexOf('@') != -1) return PsiReference.EMPTY_ARRAY;

        final JSReferenceSet jsReferenceSet =
          new JSReferenceSet(element, trimmedValueAndRange.first, trimmedValueAndRange.second.getStartOffset(), false, false, true) {
            @Override
            protected JSTextReference createTextReference(String s, int offset, boolean methodRef) {
              return new MyJSTextReference(this, s, offset, methodRef, quickFixProvider);
            }
          };
        if (SKIN_CLASS_ATTR_NAME.equals(name)) {
          jsReferenceSet.setBaseClassFqns(Collections.singletonList(UI_COMPONENT_FQN));
        }
        return jsReferenceSet.getReferences();
      }
    };
  }

  private static class MyJSTextReference extends JSTextReference implements LocalQuickFixProvider {
    private final Function<PsiReference, LocalQuickFix[]> myQuickFixProvider;

    MyJSTextReference(JSReferenceSet set, String s, int offset, boolean methodRef, Function<PsiReference, LocalQuickFix[]> quickFixProvider) {
      super(set, s, offset, methodRef);
      myQuickFixProvider = quickFixProvider;
    }

    @Override
    public LocalQuickFix[] getQuickFixes() {
      if (myQuickFixProvider != null) {
        return myQuickFixProvider.fun(this);
      }
      return super.getQuickFixes();
    }
  }

  public static boolean isClassReferenceType(final String type) {
    return "Class".equals(type) || FlexCommonTypeNames.IFACTORY.equals(type);
  }

  private static Pair<String, TextRange> getTrimmedValueAndRange(final @NotNull XmlElement xmlElement) {
    if (xmlElement instanceof XmlTag) {
      return Pair.create(((XmlTag)xmlElement).getValue().getTrimmedText(), ElementManipulators.getValueTextRange(xmlElement));
    }
    else if (xmlElement instanceof XmlAttributeValue) {
      final String value = ((XmlAttributeValue)xmlElement).getValue();
      final String trimmedText = value.trim();
      final int index = xmlElement.getText().indexOf(trimmedText);
      return index < 0 || trimmedText.length() == 0
             ? Pair.create(value, ((XmlAttributeValue)xmlElement).getValueTextRange())
             : Pair.create(trimmedText, new TextRange(index, index + trimmedText.length()));
    }
    else {
      assert false;
      return Pair.create(null, null);
    }
  }

  private static PsiReference[] buildStateRefs(PsiElement element, boolean stateGroupsOnly) {
    SmartList<PsiReference> refs = new SmartList<PsiReference>();
    StringTokenizer t = new StringTokenizer(StringUtil.stripQuotesAroundValue(element.getText()), DELIMS);

    while (t.hasMoreElements()) {
      String val = t.nextElement();
      int end = t.getCurrentPosition();
      refs.add(new StateReference(element, new TextRange(1 + end - val.length(), 1 + end), stateGroupsOnly));
    }

    return refs.toArray(new PsiReference[refs.size()]);
  }

  public static class StateReference extends BasicAttributeValueReference implements EmptyResolveMessageProvider, PsiPolyVariantReference {
    private static final String DUMMY_STATE_GROUP_TAG = "DummyStateGroupTag";

    private static FileBasedUserDataCache<Map<String, XmlTag>> statesCache = new FileBasedUserDataCache<Map<String, XmlTag>>() {
      public Key<CachedValue<Map<String, XmlTag>>> ourDataKey = Key.create("mx.states");

      @Override
      protected Map<String, XmlTag> doCompute(PsiFile file) {
        final Map<String, XmlTag> tags = new THashMap<String, XmlTag>();

        file.getOriginalFile().accept(new XmlRecursiveElementVisitor() {
          @Override
          public void visitXmlTag(XmlTag tag) {
            super.visitXmlTag(tag);

            if ("State".equals(tag.getLocalName())) {
              String name = tag.getAttributeValue(FlexStateElementNames.NAME);
              if (name != null) tags.put(name, tag);
              String groups = tag.getAttributeValue(FlexStateElementNames.STATE_GROUPS);

              if (groups != null) {
                StringTokenizer tokenizer = new StringTokenizer(groups, DELIMS);
                while (tokenizer.hasMoreElements()) {
                  String s = tokenizer.nextElement();

                  XmlTag cachedTag = tags.get(s);
                  if (cachedTag == null) {
                    PsiFile fromText = PsiFileFactory.getInstance(tag.getProject())
                      .createFileFromText("dummy.mxml", FlexApplicationComponent.MXML,
                                          "<" + DUMMY_STATE_GROUP_TAG + " name=\"" + s + "\" />");
                    cachedTag = ((XmlFile)fromText).getDocument().getRootTag();
                    tags.put(s, cachedTag);
                  }
                }
              }
            }
          }
        });
        return tags;
      }

      @Override
      protected Key<CachedValue<Map<String, XmlTag>>> getKey() {
        return ourDataKey;
      }
    };


    private final boolean myStateGroupsOnly;

    public StateReference(PsiElement element) {
      super(element);
      myStateGroupsOnly = false;
    }

    public StateReference(PsiElement element, TextRange range) {
      this(element, range, false);
    }

    public StateReference(PsiElement element, TextRange range, boolean stateGroupsOnly) {
      super(element, range);
      myStateGroupsOnly = stateGroupsOnly;
    }

    public PsiElement resolve() {
      ResolveResult[] results = multiResolve(false);
      return results.length == 1 ? results[0].getElement() : null;
    }

    @NotNull
    public ResolveResult[] multiResolve(boolean incompleteCode) {
      final List<ResolveResult> result = new ArrayList<ResolveResult>(1);
      process(new StateProcessor() {
        public boolean process(@NotNull final XmlTag t, @NotNull String name) {
          result.add(new ResolveResult() {
            public PsiElement getElement() {
              return t.getAttribute(FlexStateElementNames.NAME).getValueElement();
            }

            public boolean isValidResult() {
              return true;
            }
          });
          return true;
        }

        public String getHint() {
          return getCanonicalText();
        }
      });
      return result.toArray(new ResolveResult[result.size()]);
    }

    interface StateProcessor {
      boolean process(@NotNull XmlTag t, @NotNull String name);

      @Nullable
      String getHint();
    }

    @NotNull
    public Object[] getVariants() {
      final Set<String> list = new THashSet<String>();

      process(new StateProcessor() {
        public boolean process(XmlTag t, @NotNull String name) {
          list.add(name);
          return true;
        }

        public String getHint() {
          return null;
        }
      });

      final PsiElement parent = myElement instanceof XmlAttributeValue ? myElement.getParent() : null;
      final PsiElement tag = parent instanceof XmlAttribute ? parent.getParent() : null;

      if (tag instanceof XmlTag && TRANSITION_TAG_NAME.equals(((XmlTag)tag).getLocalName())) {
        list.add("*");
      }

      return ArrayUtil.toObjectArray(list);
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
      for (ResolveResult r : multiResolve(false)) {
        if (myElement.getManager().areElementsEquivalent(element, r.getElement())) return true;
      }
      return false;
    }

    private boolean process(StateProcessor processor) {
      String s = processor.getHint();

      Map<String, XmlTag> map = statesCache.compute(getElement().getContainingFile());
      if (s == null) {
        for (Map.Entry<String, XmlTag> t : map.entrySet()) {
          XmlTag tag = t.getValue();
          if (myStateGroupsOnly && !DUMMY_STATE_GROUP_TAG.equals(tag.getName())) continue;
          if (!processor.process(tag, t.getKey())) return false;
        }
      }
      else {
        XmlTag tag = map.get(s);
        if (myStateGroupsOnly && !DUMMY_STATE_GROUP_TAG.equals(tag.getName())) return true;
        if (tag != null) return processor.process(tag, s);
      }
      return true;
    }

    public boolean isSoft() {
      return false;
    }

    @NotNull
    public String getUnresolvedMessagePattern() {
      return FlexBundle.message("cannot.resolve.state");
    }

    public PsiElement handleElementRename(final String newElementName) throws IncorrectOperationException {
      if (myElement instanceof XmlTag) {
        final XmlToken startTagNameElement = XmlTagUtil.getStartTagNameElement((XmlTag)myElement);
        if (startTagNameElement != null) {
          final TextRange rangeInTagNameElement = myRange.shiftRight(-(startTagNameElement.getTextOffset() - myElement.getTextOffset()));
          final TextRange startTagNameElementRange = startTagNameElement.getTextRange().shiftRight(-myElement.getTextRange().getStartOffset());
          if (startTagNameElementRange.contains(rangeInTagNameElement)) {
            final StringBuilder newName = new StringBuilder(startTagNameElement.getText());
            newName.replace(rangeInTagNameElement.getStartOffset(), rangeInTagNameElement.getEndOffset(), newElementName);
            ((XmlTag)myElement).setName(newName.toString());
          }
        }
        return myElement;
      }

      return super.handleElementRename(newElementName);
    }
  }
}
