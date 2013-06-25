package com.intellij.flex.uiDesigner.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.registry.Registry;

public class DebugDesignViewAction extends RunDesignViewAction {
  @Override
  protected boolean isDebug() {
    return true;
  }

  @Override
  public void update(AnActionEvent event) {
    if (Registry.is("show.flex.debug.design.view")) {
      super.update(event);
    } else {
      event.getPresentation().setVisible(false);
    }
  }
}
