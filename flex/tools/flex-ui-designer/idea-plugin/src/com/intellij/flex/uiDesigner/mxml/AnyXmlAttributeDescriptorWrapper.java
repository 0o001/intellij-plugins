package com.intellij.flex.uiDesigner.mxml;

import com.intellij.lang.javascript.flex.AnnotationBackedDescriptor;
import com.intellij.lang.javascript.psi.JSCommonTypeNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class AnyXmlAttributeDescriptorWrapper implements AnnotationBackedDescriptor {
  private final PsiMetaData descriptor;

  public AnyXmlAttributeDescriptorWrapper(PsiMetaData descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  public String getType() {
    return JSCommonTypeNames.ANY_TYPE;
  }

  @Override
  public String getFormat() {
    return null;
  }

  @Override
  public String getArrayType() {
    return null;
  }

  @Override
  public boolean isPredefined() {
    return false;
  }

  @Override
  public boolean isAllowsPercentage() {
    return false;
  }

  @Override
  public boolean isStyle() {
    return false;
  }

  @Override
  public String getPercentProxy() {
    return null;
  }

  @Override
  public boolean isRichTextContent() {
    return false;
  }

  @Override
  public boolean isCollapseWhiteSpace() {
    return false;
  }

  @Override
  public boolean isDeferredInstance() {
    return false;
  }

  @Override
  public boolean contentIsArrayable() {
    return true;
  }

  @Override
  public String getTypeName() {
    return "Function";
  }

  @Override
  public Icon getIcon() {
    return PlatformIcons.PROPERTY_ICON;
  }

  @Override
  public boolean isRequired() {
    return false;
  }

  @Override
  public boolean isFixed() {
    return false;
  }

  @Override
  public boolean hasIdType() {
    return false;
  }

  @Override
  public boolean hasIdRefType() {
    return false;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public boolean isEnumerated() {
    return false;
  }

  @Override
  public String[] getEnumeratedValues() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public String validateValue(XmlElement xmlElement, String s) {
    return null;
  }

  @Override
  public String getQualifiedName() {
    return descriptor.getName();
  }

  @Override
  public String getDefaultName() {
    return descriptor.getName();
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag xmlTag) {
    return null;
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag xmlTag, XmlTag xmlTag1) {
    return null;
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag xmlTag) {
    return null;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String s, @Nullable XmlTag xmlTag) {
    return null;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute xmlAttribute) {
    return null;
  }

  @Override
  public XmlNSDescriptor getNSDescriptor() {
    return null;
  }

  @Override
  public XmlElementsGroup getTopGroup() {
    return null;
  }

  @Override
  public int getContentType() {
    return CONTENT_TYPE_UNKNOWN;
  }

  @Override
  public PsiElement getDeclaration() {
    return null;
  }

  @Override
  public String getName(PsiElement context) {
    return descriptor.getName(context);
  }

  @Override
  public String getName() {
    return descriptor.getName();
  }

  @Override
  public void init(PsiElement element) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Object[] getDependencies() {
    return descriptor.getDependencies();
  }

  @Override
  public boolean requiresCdataBracesInContext(@NotNull XmlTag xmlTag) {
    return false;
  }
}
