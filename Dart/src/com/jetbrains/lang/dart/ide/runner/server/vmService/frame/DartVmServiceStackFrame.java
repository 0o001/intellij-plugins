package com.jetbrains.lang.dart.ide.runner.server.vmService.frame;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.jetbrains.lang.dart.ide.runner.server.vmService.DartVmServiceDebugProcess;
import org.dartlang.vm.service.element.BoundVariable;
import org.dartlang.vm.service.element.ElementList;
import org.dartlang.vm.service.element.Frame;
import org.dartlang.vm.service.element.InstanceRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DartVmServiceStackFrame extends XStackFrame {

  @NotNull private final DartVmServiceDebugProcess myDebugProcess;
  @NotNull private final String myIsolateId;
  @NotNull private final Frame myVmFrame;
  @Nullable private final InstanceRef myException;
  @Nullable private final XSourcePosition mySourcePosition;

  public DartVmServiceStackFrame(@NotNull final DartVmServiceDebugProcess debugProcess,
                                 @NotNull final String isolateId,
                                 @NotNull final Frame vmFrame,
                                 @Nullable final InstanceRef exception) {
    myDebugProcess = debugProcess;
    myIsolateId = isolateId;
    myVmFrame = vmFrame;
    myException = exception;
    mySourcePosition = debugProcess.getSourcePosition(isolateId, vmFrame.getLocation().getScript(), vmFrame.getLocation().getTokenPos());
  }

  @NotNull
  public String getIsolateId() {
    return myIsolateId;
  }

  @Nullable
  @Override
  public XSourcePosition getSourcePosition() {
    return mySourcePosition;
  }

  @Override
  public void customizePresentation(@NotNull final ColoredTextContainer component) {
    final String name = StringUtil.trimEnd(myVmFrame.getCode().getName(), "="); // trim setter postfix
    component.append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

    if (mySourcePosition != null) {
      final String text = " (" + mySourcePosition.getFile().getName() + ":" + (mySourcePosition.getLine() + 1) + ")";
      component.append(text, SimpleTextAttributes.GRAY_ATTRIBUTES);
    }

    component.setIcon(AllIcons.Debugger.StackFrame);
  }

  @NotNull
  @Override
  public Object getEqualityObject() {
    return myVmFrame.getLocation().getScript().getId() + ":" + myVmFrame.getCode().getId();
  }

  @Override
  public void computeChildren(@NotNull final XCompositeNode node) {
    final ElementList<BoundVariable> vars = myVmFrame.getVars();
    final XValueChildrenList childrenList = new XValueChildrenList(vars.size() + 1);

    if (myException != null) {
      childrenList.add(new DartVmServiceValue(myDebugProcess, myIsolateId, "exception", myException, true));
    }

    for (BoundVariable var : vars) {
      childrenList.add(new DartVmServiceValue(myDebugProcess, myIsolateId, var.getName(), var.getValue()));
    }
    node.addChildren(childrenList, true);
  }

  @Nullable
  @Override
  public XDebuggerEvaluator getEvaluator() {
    return new DartVmServiceEvaluator(myDebugProcess, myIsolateId, myVmFrame);
  }

  public boolean isInDartSdkPatchFile() {
    return mySourcePosition != null && (mySourcePosition.getFile() instanceof LightVirtualFile);
  }
}
