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
import org.dartlang.vm.service.consumer.GetObjectConsumer;
import org.dartlang.vm.service.element.*;
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
    if (myException != null) {
      final DartVmServiceValue exception = new DartVmServiceValue(myDebugProcess, myIsolateId, "exception", myException, null, true);
      node.addChildren(XValueChildrenList.singleton(exception), false);
    }

    final ElementList<BoundVariable> vars = myVmFrame.getVars();

    if (vars.size() > 0 && "this".equals(vars.get(0).getName())) {
      addChildrenOfFirstVarAndOtherVars(node, vars);
    }
    else {
      addVars(node, vars, 0);
    }
  }

  private void addChildrenOfFirstVarAndOtherVars(@NotNull final XCompositeNode node, @NotNull final ElementList<BoundVariable> vars) {
    myDebugProcess.getVmServiceWrapper().getObject(myIsolateId, vars.get(0).getValue().getId(), new GetObjectConsumer() {
      @Override
      public void received(Obj obj) {
        DartVmServiceValue.addFields(myDebugProcess, node, myIsolateId, ((Instance)obj).getFields());
        addVars(node, vars, 1);
      }

      @Override
      public void received(Sentinel sentinel) {
        node.setErrorMessage(sentinel.getValueAsString());
      }

      @Override
      public void onError(RPCError error) {
        node.setErrorMessage(error.getMessage());
      }
    });
  }

  private void addVars(@NotNull final XCompositeNode node, @NotNull final ElementList<BoundVariable> vars, final int from) {
    final XValueChildrenList childrenList = new XValueChildrenList(vars.size() - from);

    for (int i = from; i < vars.size(); i++) {
      final InstanceRef value = vars.get(i).getValue();
      if (value != null) {
        childrenList.add(new DartVmServiceValue(myDebugProcess, myIsolateId, vars.get(i).getName(), value, null, false));
      }
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
