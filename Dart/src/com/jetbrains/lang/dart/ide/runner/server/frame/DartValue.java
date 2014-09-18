package com.jetbrains.lang.dart.ide.runner.server.frame;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Ref;
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
  public static final String NODE_NAME_RESULT = "result";
  public static final String NODE_NAME_EXCEPTION = "exception";
  private static final String NO_RESPONSE_FROM_DART_VM = "<no response from the Dart VM>";

  private final @NotNull DartCommandLineDebugProcess myDebugProcess;
  private final @Nullable VmVariable myVmVariable;
  private @Nullable VmValue myVmValue;
  private final boolean myIsException;

  private Ref<Integer> myListOrMapChildrenAlreadyShown = new Ref<Integer>(0);

  private static final String OBJECT_OF_TYPE_PREFIX = "object of type ";

  public DartValue(final @NotNull DartCommandLineDebugProcess debugProcess, final @NotNull VmVariable vmVariable) {
    super(StringUtil.notNullize(DebuggerUtils.demangleVmName(vmVariable.getName()), "<unknown>"));
    myDebugProcess = debugProcess;
    myVmVariable = vmVariable;
    myIsException = false;
  }

  public DartValue(@NotNull final DartCommandLineDebugProcess debugProcess,
                   @NotNull final String nodeName,
                   @NotNull @SuppressWarnings("NullableProblems") final VmValue vmValue,
                   final boolean isException) {
    super(nodeName);
    myDebugProcess = debugProcess;
    myVmVariable = null;
    myVmValue = vmValue;
    myIsException = isException;
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

        final boolean neverHasChildren = myVmValue.isPrimitive() ||
                                         myVmValue.isNull() ||
                                         myVmValue.isFunction() ||
                                         myVmValue.isList() && myVmValue.getLength() == 0;
        node.setPresentation(getIcon(), presentation, !neverHasChildren);
      }
    });
  }

  private Icon getIcon() {
    if (myIsException) return AllIcons.Debugger.Db_exception_breakpoint;
    if (myVmValue != null && myVmValue.isList()) return AllIcons.Debugger.Db_array;
    if (myVmValue != null && myVmValue.isPrimitive()) return AllIcons.Debugger.Db_primitive;
    if (myVmValue != null && myVmValue.isFunction()) return AllIcons.Nodes.Function;

    return AllIcons.Debugger.Value; // todo m.b. resolve and show corresponding icon?
  }

  @Override
  public void computeChildren(@NotNull final XCompositeNode node) {
    computeChildren(node, myListOrMapChildrenAlreadyShown);
  }

  private void computeChildren(@NotNull final XCompositeNode node, @NotNull final Ref<Integer> listChildrenAlreadyShown) {
    // myVmValue is already calculated in computePresentation()
    if (myVmValue == null) node.addChildren(XValueChildrenList.EMPTY, true);

    // see com.google.dart.tools.debug.core.server.ServerDebugValue#fillInFieldsSync()
    try {
      if (myVmValue.isList()) {
        computeListChildren(node, listChildrenAlreadyShown);
        return;
      }

      myDebugProcess.getVmConnection().evaluateObject(myVmValue.getIsolate(), myVmValue, "this is Iterable", new VmCallback<VmValue>() {
        public void handleResult(final VmResult<VmValue> result) {
          if (node.isObsolete()) {
            return;
          }

          if (result.isError()) {
            node.setErrorMessage(result.getError());
            return;
          }

          if (result.getResult() == null) {
            node.setErrorMessage(NO_RESPONSE_FROM_DART_VM);
            return;
          }

          if ("boolean".equals(result.getResult().getKind()) && "true".equals(result.getResult().getText())) {
            computeIterableChildren(node);
          }
          else {
            computeObjectChildren(node);
          }
        }
      });
    }
    catch (IOException e) {
      DartCommandLineDebugProcess.LOG.error(e);
    }
  }

  private void computeListChildren(@NotNull final XCompositeNode node, @NotNull final Ref<Integer> listChildrenAlreadyShown)
    throws IOException {
    DartCommandLineDebugProcess.LOG.assertTrue(myVmValue != null && myVmValue.isList(), myVmValue);

    final int childrenToShow = Math.min(myVmValue.getLength() - listChildrenAlreadyShown.get(), XCompositeNode.MAX_CHILDREN_TO_SHOW);
    if (childrenToShow == 0) {
      node.addChildren(XValueChildrenList.EMPTY, true);
      return;
    }

    final AtomicInteger handledResponsesAmount = new AtomicInteger(0);

    final List<DartValue> sortedChildren = Collections.synchronizedList(new SortedList<DartValue>(new Comparator<DartValue>() {
      public int compare(DartValue o1, DartValue o2) {
        return StringUtil.naturalCompare(o1.getName(), o2.getName());
      }
    }));

    for (int listIndex = listChildrenAlreadyShown.get(); listIndex < listChildrenAlreadyShown.get() + childrenToShow; listIndex++) {
      final String nodeName = String.valueOf(listIndex);
      myDebugProcess.getVmConnection()
        .getListElements(myVmValue.getIsolate(), myVmValue.getObjectId(), listIndex,
                         new VmCallback<VmValue>() {
                           @Override
                           public void handleResult(final VmResult<VmValue> vmResult) {
                             final int responsesAmount = handledResponsesAmount.addAndGet(1);

                             if (node.isObsolete()) {
                               return;
                             }

                             if (vmResult.isError()) {
                               node.setErrorMessage(vmResult.getError());
                               return;
                             }

                             if (vmResult.getResult() == null) {
                               node.setErrorMessage(NO_RESPONSE_FROM_DART_VM);
                               return;
                             }

                             sortedChildren.add(new DartValue(myDebugProcess, nodeName, vmResult.getResult(), false));

                             if (responsesAmount == childrenToShow) {
                               final XValueChildrenList resultList = new XValueChildrenList(sortedChildren.size());
                               for (DartValue value : sortedChildren) {
                                 resultList.add(value);
                               }

                               node.addChildren(resultList, true);
                               listChildrenAlreadyShown.set(listChildrenAlreadyShown.get() + childrenToShow);

                               if (myVmValue.getLength() > listChildrenAlreadyShown.get()) {
                                 node.tooManyChildren(myVmValue.getLength() - listChildrenAlreadyShown.get());
                               }
                             }
                           }
                         });
    }
  }

  private void computeIterableChildren(@NotNull final XCompositeNode node) {
    DartCommandLineDebugProcess.LOG.assertTrue(myVmValue != null);

    try {
      myDebugProcess.getVmConnection().evaluateObject(myVmValue.getIsolate(), myVmValue, "toList()", new VmCallback<VmValue>() {
        @Override
        public void handleResult(final VmResult<VmValue> result) {
          if (node.isObsolete()) {
            return;
          }

          if (result.isError()) {
            node.setErrorMessage(result.getError());
            return;
          }

          if (result.getResult() == null) {
            node.setErrorMessage(NO_RESPONSE_FROM_DART_VM);
            return;
          }

          // result.getResult() is a list that will return list contents
          new DartValue(myDebugProcess, "fake node", result.getResult(), false).computeChildren(node, myListOrMapChildrenAlreadyShown);
        }
      });
    }
    catch (IOException e) {
      DartCommandLineDebugProcess.LOG.error(e);
    }
  }

  private void computeObjectChildren(@NotNull final XCompositeNode node) {
    DartCommandLineDebugProcess.LOG.assertTrue(myVmValue != null);

    try {
      myDebugProcess.getVmConnection()
        .getObjectProperties(myVmValue.getIsolate(),
                             myVmValue.getObjectId(),
                             new VmCallback<VmObject>() {
                               @Override
                               public void handleResult(final VmResult<VmObject> result) {
                                 if (node.isObsolete()) {
                                   return;
                                 }

                                 if (result.isError()) {
                                   node.setErrorMessage(result.getError());
                                   return;
                                 }

                                 if (result.getResult() == null) {
                                   node.setErrorMessage(NO_RESPONSE_FROM_DART_VM);
                                   return;
                                 }

                                 final List<VmVariable> fields = result.getResult().getFields();
                                 if (fields == null) {
                                   node.addChildren(XValueChildrenList.EMPTY, true);
                                   return;
                                 }

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
}