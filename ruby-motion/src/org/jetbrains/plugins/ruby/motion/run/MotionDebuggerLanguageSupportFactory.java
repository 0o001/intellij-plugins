package org.jetbrains.plugins.ruby.motion.run;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrDebuggerLanguageSupportFactory;
import com.jetbrains.cidr.execution.debugger.CidrEvaluator;
import com.jetbrains.cidr.execution.debugger.CidrStackFrame;
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.debugger.impl.RubyDebuggerEditorsProvider;
import org.jetbrains.plugins.ruby.ruby.lang.RubyFileType;
import org.jetbrains.plugins.ruby.ruby.run.configuration.AbstractRubyRunConfiguration;

/**
 * @author Dennis.Ushakov
 */
public class MotionDebuggerLanguageSupportFactory extends CidrDebuggerLanguageSupportFactory {
  @Nullable
  @Override
  public XDebuggerEditorsProvider createEditor(RunProfile profile) {
    if (profile instanceof AbstractRubyRunConfiguration) {
      return new RubyDebuggerEditorsProvider();
    }
    return null;
  }

  @Nullable
  @Override
  public XDebuggerEditorsProvider createEditor(XBreakpoint<? extends XBreakpointProperties> breakpoint) {
    if (breakpoint instanceof XLineBreakpoint) {
      final String extension = FileUtilRt.getExtension(((XLineBreakpoint)breakpoint).getShortFilePath());
      if (FileTypeManager.getInstance().getFileTypeByExtension(extension) == RubyFileType.RUBY) {
        return new RubyDebuggerEditorsProvider();
      }
    }
    return null;
  }

  @Override
  public CidrDebuggerTypesHelper createTypesHelper(CidrDebugProcess process) {
    return new MotionDebuggerTypesHelper(process);
  }

  @Nullable
  @Override
  public CidrEvaluator createEvaluator(@NotNull CidrStackFrame frame) {
    return null;
  }
}
