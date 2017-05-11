package com.jetbrains.actionscript.profiler.base;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public abstract class LazyNode extends DefaultMutableTreeNode {
  private boolean myChildrenLoaded;

  @Override
  public int getChildCount() {
    loadChildren();
    return super.getChildCount();
  }

  @Override
  public TreeNode getChildAt(int index) {
    loadChildren();
    return super.getChildAt(index);
  }

  private void loadChildren() {
    if (!myChildrenLoaded) {
      myChildrenLoaded = true;
      doLoadChildren();
    }
  }

  protected abstract void doLoadChildren();
}
