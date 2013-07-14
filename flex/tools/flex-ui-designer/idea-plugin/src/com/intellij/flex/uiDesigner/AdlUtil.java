package com.intellij.flex.uiDesigner;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.compiler.options.CompileStepBeforeRunNoErrorCheck;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.flex.uiDesigner.debug.FlexRunner;
import com.intellij.flex.uiDesigner.io.IOUtil;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfigurationManager;
import com.intellij.lang.javascript.flex.run.RemoteFlashRunConfiguration;
import com.intellij.lang.javascript.flex.run.RemoteFlashRunConfigurationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AdlUtil {
  private static final Logger LOG = Logger.getInstance(AdlUtil.class.getName());

  private static final String MAC_AIR_RUNTIME_DEFAULT_PATH = "/Library/Frameworks";
  @NonNls
  private static final Pattern INFO_PLIST_VERSION_PATTERN = Pattern.compile("<key>CFBundleVersion</key>\\s*<string>(.*)</string>");

  private AdlUtil() {
  }

  private static final String[] ADL_EXIT_CODE_TO_TEXT = {"Successful launch. ADL exits after the AIR application exits.",
    "Successful invocation of an already running AIR application. ADL exits immediately.",
    "Usage error. The arguments supplied to ADL are incorrect.", "The runtime cannot be found.",
    "The runtime cannot be started. Often, this occurs because the version or patch level specified in the application does not match the version or patch level of the runtime.",
    "An error of unknown cause occurred.", "The application descriptor file cannot be found.",
    "The contents of the application descriptor are not valid. This error usually indicates that the XML is not well formed.",
    "The main application content file (specified in the <content> element of the application descriptor file) cannot be found.",
    "The main application content file is not a valid SWF or HTML file."};

  public static String describeAdlExit(int exitCode) {
    String exitCodeDescription = exitCode < ADL_EXIT_CODE_TO_TEXT.length ? ADL_EXIT_CODE_TO_TEXT[exitCode] : "Unknown exit code.";
    return "ADL exited with error code " + exitCode + ". " + exitCodeDescription + " OS: " + SystemInfo.OS_NAME;
  }

  // http://kb2.adobe.com/cps/407/kb407625.html
  public static void runDebugger(final Module module, final Runnable postTask) throws ExecutionException {
    final Project project = module.getProject();
    final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final RunnerAndConfigurationSettings settings =
      runManager.createConfiguration("FlashUIDesigner", RemoteFlashRunConfigurationType.getFactory());

    final RemoteFlashRunConfiguration configuration = (RemoteFlashRunConfiguration)settings.getConfiguration();
    RunManagerEx.disableTasks(project, settings.getConfiguration(), CompileStepBeforeRun.ID, CompileStepBeforeRunNoErrorCheck.ID);

    final DefaultDebugExecutor executor = new DefaultDebugExecutor();
    ProgramRunner.Callback callback = new ProgramRunner.Callback() {
      @Override
      public void processStarted(final RunContentDescriptor descriptor) {
        final ProcessHandler processHandler = descriptor.getProcessHandler();
        assert processHandler != null;
        DesignerApplication application = DesignerApplicationManager.getApplication();
        if (application != null) {
          Disposer.register(application, new Disposable() {
            @Override
            public void dispose() {
              if (!project.isDisposed()) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    ExecutionManager.getInstance(project).getContentManager().removeRunContent(executor, descriptor);
                  }
                });
              }

              processHandler.destroyProcess();
            }
          });

          postTask.run();
        }
      }
    };

    FlexBuildConfiguration buildConfiguration = FlexBuildConfigurationManager.getInstance(module).getActiveConfiguration();
    configuration.getRunnerParameters().setModuleName(module.getName());
    configuration.getRunnerParameters().setBCName(buildConfiguration.getName());

    final FlexRunner runner = new FlexRunner(callback, buildConfiguration);
    runner.execute(executor, new ExecutionEnvironment(runner, settings, project));
  }

  public static AdlProcessHandler runAdl(AdlRunConfiguration runConfiguration,
                                         String descriptor,
                                         final @Nullable Consumer<Integer> adlExitHandler) throws ExecutionException {
    return runAdl(runConfiguration, descriptor, null, adlExitHandler);
  }

  public static AdlProcessHandler runAdl(AdlRunConfiguration runConfiguration,
                                         String descriptor,
                                         @Nullable String root,
                                         final @Nullable Consumer<Integer> adlExitHandler)
    throws ExecutionException {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setRedirectErrorStream(true);

    commandLine.setExePath(runConfiguration.adlPath);
    if (runConfiguration.runtime != null) {
      commandLine.addParameters("-runtime", runConfiguration.runtime);
    }

    // see http://confluence.jetbrains.net/display/IDEA/Flex+UI+Designer about nodebug
    //if (!runConfiguration.debug) {
    //  command.add("-nodebug");
    //}

    commandLine.addParameters(descriptor);
    if (root != null) {
      commandLine.addParameter(root);
    }

    if (runConfiguration.arguments != null) {
      commandLine.addParameter("--");
      commandLine.addParameters(runConfiguration.arguments);
    }

    LOG.info(commandLine.getCommandLineString());
    return new AdlProcessHandler(commandLine, adlExitHandler);
  }

  static boolean checkRuntime(String runtimePath) throws IOException {
    return !StringUtil.isEmpty(runtimePath) && new File(runtimePath).isDirectory() &&
           (!SystemInfo.isMac || checkMacRuntimeVersion(runtimePath));
  }

  static boolean checkAdl(String adlPath) {
    return !StringUtil.isEmpty(adlPath) && new File(adlPath).exists();
  }

  // http://kb2.adobe.com/cps/407/kb407625.html
  @Nullable
  static String findInstalledRuntime() throws IOException {
    if (SystemInfo.isMac && checkMacRuntimeVersion(MAC_AIR_RUNTIME_DEFAULT_PATH)) {
      return MAC_AIR_RUNTIME_DEFAULT_PATH;
    }
    return null;
  }

  private static boolean checkMacRuntimeVersion(String runtime) throws IOException {
    File info = new File(runtime, "Adobe AIR.framework/Resources/Info.plist");
    if (!info.exists()) {
      LOG.info(runtime + " is not valid runtime because Info.plist not found");
      return false;
    }

    Matcher m = INFO_PLIST_VERSION_PATTERN.matcher(IOUtil.getCharSequence(info));
    final boolean result = m.find() && StringUtil.compareVersionNumbers(m.group(1), "2.6") >= 0;
    if (!result) {
      LOG.info(runtime + " is not valid runtime because version is not suitable");
    }
    return result;
  }

  public static class AdlRunConfiguration {
    private final String adlPath;
    private final @Nullable String runtime;

    private final String runtimeVersion;

    public @Nullable List<String> arguments;

    AdlRunConfiguration(String adlPath, @Nullable String runtime, String runtimeVersion) {
      this.adlPath = adlPath;
      this.runtime = runtime;
      this.runtimeVersion = runtimeVersion;
    }

    @Override
    public String toString() {
      return "adlRunConfiguration";
    }

    public String getRuntimeVersion() {
      return runtimeVersion;
    }
  }

  static class AdlProcessHandler extends OSProcessHandler {
    public Consumer<Integer> adlExitHandler;

    private AdlProcessHandler(GeneralCommandLine commandLine, Consumer<Integer> adlExitHandler) throws ExecutionException {
      super(commandLine.createProcess());
      this.adlExitHandler = adlExitHandler;
      startNotify();
    }

    @Override
    public void notifyTextAvailable(String text, Key outputType) {
      LOG.debug("[adl output/error stream]: " + text);
    }

    @Override
    protected boolean shouldDestroyProcessRecursively() {
      return true;
    }

    @Override
    protected void doDestroyProcess() {
      super.doDestroyProcess();

      callExitHandler(0);
    }

    @Override
    protected void onOSProcessTerminated(int exitCode) {
      callExitHandler(exitCode);
    }

    private void callExitHandler(int exitCode) {
      if (adlExitHandler != null) {
        adlExitHandler.consume(exitCode);
        adlExitHandler = null;
      }
    }
  }
}