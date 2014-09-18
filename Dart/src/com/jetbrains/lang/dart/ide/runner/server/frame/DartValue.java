package com.jetbrains.lang.dart.ide.runner.server.frame;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.SortedList;
import com.intellij.xdebugger.frame.*;
import com.intellij.xdebugger.frame.presentation.XNumericValuePresentation;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineDebugProcess;
import com.jetbrains.lang.dart.ide.runner.server.google.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// todo navigate to source, type
public class DartValue extends XNamedValue {
  private final @NotNull DartCommandLineDebugProcess myDebugProcess;
  private final @Nullable VmVariable myVmVariable;
  private @Nullable VmValue myVmValue;

  private int myListOrMapChildrenAlreadyShown = 0;

  private static final String OBJECT_OF_TYPE_PREFIX = "object of type ";

  public DartValue(final @NotNull DartCommandLineDebugProcess debugProcess, final @NotNull VmVariable vmVariable) {
    super(StringUtil.notNullize(DebuggerUtils.demangleVmName(vmVariable.getName()), "<unknown>"));
    myDebugProcess = debugProcess;
    myVmVariable = vmVariable;
  }

  public DartValue(@NotNull final DartCommandLineDebugProcess debugProcess,
                   @NotNull final String nodeName,
                   @NotNull @SuppressWarnings("NullableProblems") final VmValue vmValue) {
    super(nodeName);
    myDebugProcess = debugProcess;
    myVmVariable = null;
    myVmValue = vmValue;
  }

  @Override
  public void computePresentation(final @NotNull XValueNode node, final @NotNull XValuePlace place) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      public void run() {
        if (node.isObsolete()) return;

        if (myVmValue == null && myVmVariable != null) {
          myVmValue = myVmVariable.getValue();
        }

        if (myVmValue == null) {
          node.setPresentation(AllIcons.Debugger.Value, null, "<no value>", false);
          return;
        }

        final String value = StringUtil.notNullize(myVmValue.getText(), "null");
        final XValuePresentation presentation;

        final int objectId = myVmValue.getObjectId();
        final String objectIdPostfix = /*objectId == 0 ? "" :*/ "[id=" + objectId + "]"; // 0 is also a valid id

        if (myVmValue.isNull()) {
          presentation = new XRegularValuePresentation("null", null);
        }
        else if (myVmValue.isString()) {
          presentation = new XStringValuePresentation(StringUtil.stripQuotesAroundValue(value));
        }
        else if (myVmValue.isNumber()) {
          presentation = new XNumericValuePresentation(value);
        }
        else if ("boolean".equals(myVmValue.getKind())) {
          presentation = new XRegularValuePresentation(value, null);
        }
        else if (myVmValue.isList()) {
          presentation = new XRegularValuePresentation("size = " + myVmValue.getLength(), "List" + objectIdPostfix);
        }
        else {
          if (value.startsWith(OBJECT_OF_TYPE_PREFIX)) {
            presentation = new XRegularValuePresentation("", value.substring(OBJECT_OF_TYPE_PREFIX.length()) + objectIdPostfix);
          }
          else {
            presentation = new XRegularValuePresentation(value, DebuggerUtils.demangleVmName(myVmValue.getKind()) + objectIdPostfix);
          }
        }

        final boolean neverHasChildren = myVmValue.isPrimitive() || myVmValue.isNull() || myVmValue.isFunction();
        node.setPresentation(getIcon(myVmValue), presentation, !neverHasChildren);
      }
    });
  }

  private static Icon getIcon(final @NotNull VmValue vmValue) {
    if (vmValue.isList()) return AllIcons.Debugger.Db_array;
    if (vmValue.isPrimitive()) return AllIcons.Debugger.Db_primitive;
    if (vmValue.isFunction()) return AllIcons.Nodes.Function;

    return AllIcons.Debugger.Value; // todo m.b. resolve and show corresponding icon?
  }

  @Override
  public void computeChildren(final @NotNull XCompositeNode node) {
    // myVmValue is already calculated in computePresentation()
    if (myVmValue == null) node.addChildren(XValueChildrenList.EMPTY, true);

    // see com.google.dart.tools.debug.core.server.ServerDebugValue#fillInFieldsSync()
    try {
      if (myVmValue.isList()) {
        computeListChildren(node);
        return;
      }

      myDebugProcess.getVmConnection()
        .getObjectProperties(myVmValue.getIsolate(),
                             myVmValue.getObjectId(),
                             new VmCallback<VmObject>() {
                               @Override
                               public void handleResult(final VmResult<VmObject> result) {
                                 if (node.isObsolete()) return;

                                 final VmObject vmObject = result == null ? null : result.getResult();
                                 final List<VmVariable> fields = vmObject == null ? null : vmObject.getFields();

                                 if (fields == null || result.isError()) return;

                                 // todo sort somehow?
                                 final XValueChildrenList childrenList = new XValueChildrenList(fields.size());
                                 for (final VmVariable field : fields) {
                                   childrenList.add(new DartValue(myDebugProcess, field));
                                 }

                                 node.addChildren(childrenList, true);
                               }
                             }
        );
    }
    catch (IOException e) {
      DartCommandLineDebugProcess.LOG.error(e);
    }
  }

  private void computeListChildren(@NotNull final XCompositeNode node) throws IOException {
    DartCommandLineDebugProcess.LOG.assertTrue(myVmValue != null && myVmValue.isList(), myVmValue);

    final int childrenToShow = Math.min(myVmValue.getLength() - myListOrMapChildrenAlreadyShown, XCompositeNode.MAX_CHILDREN_TO_SHOW);
    final AtomicInteger handledResponsesAmount = new AtomicInteger(0);

    final List<DartValue> sortedChildren = Collections.synchronizedList(new SortedList<DartValue>(new Comparator<DartValue>() {
      public int compare(DartValue o1, DartValue o2) {
        return StringUtil.naturalCompare(o1.getName(), o2.getName());
      }
    }));

    for (int listIndex = myListOrMapChildrenAlreadyShown; listIndex < myListOrMapChildrenAlreadyShown + childrenToShow; listIndex++) {
      final String nodeName = String.valueOf(listIndex);
      myDebugProcess.getVmConnection()
        .getListElements(myVmValue.getIsolate(), myVmValue.getObjectId(), listIndex,
                         new VmCallback<VmValue>() {
                           @Override
                           public void handleResult(VmResult<VmValue> vmResult) {
                             final int responsesAmount = handledResponsesAmount.addAndGet(1);

                             if (node.isObsolete()) {
                               return;
                             }

                             if (vmResult.isError()) {
                               node.setErrorMessage(vmResult.getError());
                               return;
                             }

                             if (vmResult.getResult() == null) {
                               node.setErrorMessage("<no response from the Dart VM>");
                               return;
                             }

                             sortedChildren.add(new DartValue(myDebugProcess, nodeName, vmResult.getResult()));

                             if (responsesAmount == childrenToShow) {
                               final XValueChildrenList resultList = new XValueChildrenList(sortedChildren.size());
                               for (DartValue value : sortedChildren) {
                                 resultList.add(value);
                               }

                               node.addChildren(resultList, true);
                               myListOrMapChildrenAlreadyShown += childrenToShow;

                               if (myVmValue.getLength() > myListOrMapChildrenAlreadyShown) {
                                 node.tooManyChildren(myVmValue.getLength() - myListOrMapChildrenAlreadyShown);
                               }
                             }
                           }
                         });
    }
  }
}