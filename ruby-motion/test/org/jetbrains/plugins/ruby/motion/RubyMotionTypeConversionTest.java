package org.jetbrains.plugins.ruby.motion;

import org.jetbrains.plugins.ruby.motion.symbols.MotionSymbolUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolFilterFactory;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.*;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.collections.RArrayType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.impl.REmptyType;

/**
 * @author Dennis.Ushakov
 */
public class RubyMotionTypeConversionTest extends RubyMotionLightFixtureTestCase {
  @Override
  protected String getTestDataRelativePath() {
    return "testApp";
  }

  public void testBoolean() {
    defaultConfigure();
    RType type = MotionSymbolUtil.getTypeByName(getModule(), "bool");
    assertEquals("TrueClass or FalseClass", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "BOOL");
    assertEquals("TrueClass or FalseClass", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "bool*");
    assertInstanceOf(type, RArrayType.class);
    assertEquals("TrueClass or FalseClass", ((RArrayType)type).getItemType().getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "BOOL*");
    assertInstanceOf(type, RArrayType.class);
    assertEquals("TrueClass or FalseClass", ((RArrayType)type).getItemType().getPresentableName());
  }

  public void testVoid() {
    defaultConfigure();
    RType type = MotionSymbolUtil.getTypeByName(getModule(), "void");
    assertEquals(REmptyType.INSTANCE, type);
    type = MotionSymbolUtil.getTypeByName(getModule(), "void*");
    assertInstanceOf(type, RArrayType.class);
    assertEquals(REmptyType.INSTANCE, ((RArrayType)type).getItemType());
  }

  public void testFloat() {
    defaultConfigure();
    RType type = MotionSymbolUtil.getTypeByName(getModule(), "float");
    assertEquals("Float", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "double");
    assertEquals("Float", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "CGFloat");
    assertEquals("Float", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "Float32");
    assertEquals("Float", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "Float64");
    assertEquals("Float", type.getPresentableName());
  }

  public void testInt() {
    defaultConfigure();
    RType type = MotionSymbolUtil.getTypeByName(getModule(), "int");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "char");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "short");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "long");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "long long");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "unsigned int");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "unsigned char");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "unsigned short");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "unsigned long");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "unsigned long long");
    assertEquals("Fixnum", type.getPresentableName());

    type = MotionSymbolUtil.getTypeByName(getModule(), "Byte");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "SignedByte");
    assertEquals("Fixnum", type.getPresentableName());

    type = MotionSymbolUtil.getTypeByName(getModule(), "Int16");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "SInt16");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "UInt16");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "Int64");
    assertEquals("Fixnum", type.getPresentableName());

    type = MotionSymbolUtil.getTypeByName(getModule(), "NSInteger");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "NSUInteger");
    assertEquals("Fixnum", type.getPresentableName());

    type = MotionSymbolUtil.getTypeByName(getModule(), "int32_t");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "uint64_t");
    assertEquals("Fixnum", type.getPresentableName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "size_t");
    assertEquals("Fixnum", type.getPresentableName());
  }

  public void testNSObject() {
    defaultConfigure();
    RType type = MotionSymbolUtil.getTypeByName(getModule(), "NSObject*");
    assertInstanceOf(type, RSymbolType.class);
    assertEquals("NSObject", ((RSymbolType)type).getSymbol().getName());
    type = MotionSymbolUtil.getTypeByName(getModule(), "NSObject**");
    assertInstanceOf(type, RArrayType.class);
    final RType itemType = ((RArrayType)type).getItemType();
    assertInstanceOf(itemType, RSymbolType.class);
    assertEquals("NSObject", ((RSymbolType)itemType).getSymbol().getName());
  }


  public void testSuperTypes() {
    defaultConfigure();
    assertHasMembers(CoreTypes.String, "lowercaseString", "appendFormat");
    assertHasMembers(CoreTypes.Array, "sortedArrayHint", "removeObjectIdenticalTo");
    assertHasMembers(CoreTypes.Numeric, "initWithLong");
    assertHasMembers(CoreTypes.Hash, "descriptionInStringsFileFormat", "addEntriesFromDictionary");
    assertHasMembers(CoreTypes.Time, "dateByAddingTimeInterval");
  }

  private void assertHasMembers(final String className, final String... members) {
    final RType type = RTypeFactory.createTypeByFQN(getProject(), className, Context.MIXED_PRIVATE);
    for (String member : members) {
      assertNotNull("Does not have member " + member, type.getMemberForName(member, SymbolFilterFactory.EMPTY_FILTER, myFixture.getFile()));
    }
  }
}
