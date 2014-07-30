package com.github.masahirosuzuka.PhoneGapIntelliJPlugin;


import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class PhoneGapBundle extends AbstractBundle {

  public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
    return ourInstance.getMessage(key, params);
  }

  @NonNls public static final String BUNDLE = "com.github.masahirosuzuka.PhoneGapIntelliJPlugin.PhoneGapBundle";
  private static final PhoneGapBundle ourInstance = new PhoneGapBundle();

  private PhoneGapBundle() {
    super(BUNDLE);
  }
}
