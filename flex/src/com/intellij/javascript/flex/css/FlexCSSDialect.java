package com.intellij.javascript.flex.css;


import com.intellij.lang.css.CssDialect;
import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;

/**
 * User: anna
 * Date: 5/15/13
 */
public class FlexCSSDialect extends CssDialect {
  @Override
  public String getName() {
    return "FLEX";
  }

  @Override
  public String getDisplayName() {
    return FlexBundle.message("css.flex.dialect.name");
  }

  @Override
  public boolean isDefault(Module module) {
    return ModuleType.get(module) == FlexModuleType.getInstance();
  }

  @Override
  public String getDefaultDescription() {
    return "Flex CSS is used for all CSS files in a Flash module";
  }

  public static CssDialect getInstance() {
    return CssDialect.EP_NAME.findExtension(FlexCSSDialect.class);
  }
}
