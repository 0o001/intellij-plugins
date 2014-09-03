package com.github.masahirosuzuka.PhoneGapIntelliJPlugin;

import com.github.masahirosuzuka.PhoneGapIntelliJPlugin.settings.ui.PhoneGapConfigurable;
import com.intellij.lang.Language;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * If we have "JavaScript" dependency
 * then configuration  will be added to "JavaScript.Phonegap/Cordova"
 * If there is no the dependency we should use this provider for adding configuration in common list
 */
public class PhoneGapConfigurationProvider extends ConfigurableProvider {

  private Project myProject;

  public PhoneGapConfigurationProvider(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Configurable createConfigurable() {
    return Language.findLanguageByID("JavaScript") != null ? null : new PhoneGapConfigurable(myProject);
  }

  @Override
  public boolean isConfigurableProvided() {
    return Language.findLanguageByID("JavaScript") == null;
  }
}
