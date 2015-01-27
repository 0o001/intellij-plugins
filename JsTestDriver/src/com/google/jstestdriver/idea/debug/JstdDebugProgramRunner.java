package com.google.jstestdriver.idea.debug;

import com.google.jstestdriver.idea.TestRunner;
import com.google.jstestdriver.idea.execution.JstdRunConfiguration;
import com.google.jstestdriver.idea.execution.JstdRunProfileState;
import com.google.jstestdriver.idea.execution.JstdRunProgramRunner;
import com.google.jstestdriver.idea.execution.settings.JstdRunSettings;
import com.google.jstestdriver.idea.server.JstdServer;
import com.google.jstestdriver.idea.server.JstdServerRegistry;
import com.google.jstestdriver.idea.server.ui.JstdToolWindowManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.RunProfileStarter;
import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.AsyncGenericProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.browsers.BrowserFamily;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.javascript.debugger.execution.RemoteDebuggingFileFinder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.util.NullableConsumer;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.javascript.debugger.JavaScriptDebugEngine;
import com.jetbrains.javascript.debugger.JavaScriptDebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.connection.VmConnection;

import java.io.File;
import java.io.PrintWriter;

/**
 * @author Sergey Simonchik
 */
public class JstdDebugProgramRunner extends AsyncGenericProgramRunner {
  private static final String DEBUG_RUNNER_ID = JstdDebugProgramRunner.class.getSimpleName();
  private static Boolean IS_AVAILABLE_CACHE = null;

  @NotNull
  @Override
  public String getRunnerId() {
    return DEBUG_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof JstdRunConfiguration;
  }

  public static boolean isAvailable() {
    Boolean isAvailable = IS_AVAILABLE_CACHE;
    if (isAvailable != null) {
      return isAvailable;
    }
    RunnerRegistry registry = RunnerRegistry.getInstance();
    isAvailable = registry.findRunnerById(DEBUG_RUNNER_ID) != null;
    IS_AVAILABLE_CACHE = isAvailable;
    return isAvailable;
  }

  @NotNull
  @Override
  protected AsyncResult<RunProfileStarter> prepare(@NotNull final ExecutionEnvironment environment, @NotNull RunProfileState state) throws ExecutionException {
    JstdRunProfileState jstdState = JstdRunProfileState.cast(state);
    final JstdRunSettings runSettings = jstdState.getRunSettings();
    if (runSettings.isExternalServerType()) {
      throw new ExecutionException("Local JsTestDriver server running in IDE required for tests debugging");
    }
    JstdToolWindowManager jstdToolWindowManager = JstdToolWindowManager.getInstance(environment.getProject());
    jstdToolWindowManager.setAvailable(true);
    JstdServer server = JstdServerRegistry.getInstance().getServer();
    final AsyncResult<RunProfileStarter> result = new AsyncResult<RunProfileStarter>();
    if (server != null && !server.isStopped()) {
      prepareWithServer(environment.getProject(), result, server, runSettings);
      return result;
    }
    jstdToolWindowManager.restartServer(new NullableConsumer<JstdServer>() {
      @Override
      public void consume(@Nullable JstdServer server) {
        if (server != null) {
          prepareWithServer(environment.getProject(), result, server, runSettings);
        }
        else {
          result.setDone(null);
        }
      }
    });
    return result;
  }

  private static void prepareWithServer(@NotNull final Project project,
                                        @NotNull final AsyncResult<RunProfileStarter> result,
                                        @NotNull final JstdServer server,
                                        @NotNull final JstdRunSettings runSettings) {
    if (server.isReadyForRunningTests()) {
      final JstdDebugBrowserInfo debugBrowserInfo = JstdDebugBrowserInfo.build(server, runSettings);
      if (debugBrowserInfo != null) {
        ActionCallback prepareDebuggerCallback = debugBrowserInfo.getDebugEngine().prepareDebugger(project, debugBrowserInfo.getBrowser());
        prepareDebuggerCallback.notifyWhenRejected(result).doWhenDone(new Runnable() {
          @Override
          public void run() {
            result.setDone(new MyDebugStarter(server, debugBrowserInfo));
          }
        });
      }
      else {
        result.setDone(new RunProfileStarter() {
          @Nullable
          @Override
          public RunContentDescriptor execute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
            throw new ExecutionException("Please capture Chrome or Firefox and try again");
          }
        });
      }
    }
    else {
      result.setDone(new JstdRunProgramRunner.JstdRunStarter(server, true));
    }
  }

  private static class MyDebugStarter extends RunProfileStarter {
    private final JstdServer myServer;
    private final JstdDebugBrowserInfo myDebugBrowserInfo;

    private MyDebugStarter(@NotNull JstdServer server, @NotNull JstdDebugBrowserInfo debugBrowserInfo) {
      myServer = server;
      myDebugBrowserInfo = debugBrowserInfo;
    }

    @Nullable
    @Override
    public RunContentDescriptor execute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
      final WebBrowser browser = myDebugBrowserInfo.getBrowser();
      final Url url;
      if (browser.getFamily().equals(BrowserFamily.CHROME)) {
        url = Urls.newHttpUrl("127.0.0.1:" + myDebugBrowserInfo.getServerSettings().getPort(), myDebugBrowserInfo.getPath());
      }
      else {
        url = null;
      }
      FileDocumentManager.getInstance().saveAllDocuments();
      JstdRunProfileState jstdState = JstdRunProfileState.cast(state);
      final ExecutionResult executionResult = jstdState.executeWithServer(myServer);

      File configFile = new File(jstdState.getRunSettings().getConfigFile());
      final RemoteDebuggingFileFinder fileFinder = new JstdDebuggingFileFinderProvider(configFile, myServer).provideFileFinder();
      XDebugSession session = XDebuggerManager.getInstance(environment.getProject()).startSession(environment, new XDebugProcessStarter() {
        @Override
        @NotNull
        public XDebugProcess start(@NotNull XDebugSession session) {
          JavaScriptDebugEngine debugEngine = myDebugBrowserInfo.getDebugEngine();
          JavaScriptDebugProcess<? extends VmConnection> process = debugEngine.createDebugProcess(session, browser, fileFinder, url, executionResult, false);
          process.setElementsInspectorEnabled(false);
          return process;
        }
      });

      // must be here, after all breakpoints were queued
      ((JavaScriptDebugProcess)session.getDebugProcess()).getConnection().executeOnStart(new Runnable() {
        @Override
        public void run() {
          Runnable runnable = new Runnable() {
            @Override
            public void run() {
              resumeJstdClientRunning(executionResult.getProcessHandler());
            }
          };

          if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            ApplicationManager.getApplication().executeOnPooledThread(runnable);
          }
          else {
            runnable.run();
          }
        }
      });
      return session.getRunContentDescriptor();
    }
  }

  private static void resumeJstdClientRunning(@NotNull ProcessHandler processHandler) {
    // process's input stream will be closed on process termination
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed", "ConstantConditions"})
    PrintWriter writer = new PrintWriter(processHandler.getProcessInput());
    writer.println(TestRunner.DEBUG_SESSION_STARTED);
    writer.flush();
  }
}
