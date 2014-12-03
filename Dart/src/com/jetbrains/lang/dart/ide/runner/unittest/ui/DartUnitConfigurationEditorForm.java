package com.jetbrains.lang.dart.ide.runner.unittest.ui;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.server.ui.DartCommandLineConfigurationEditorForm;
import com.jetbrains.lang.dart.ide.runner.unittest.DartTestLocationProvider;
import com.jetbrains.lang.dart.ide.runner.unittest.DartUnitRunConfiguration;
import com.jetbrains.lang.dart.ide.runner.unittest.DartUnitRunConfigurationProducer;
import com.jetbrains.lang.dart.ide.runner.unittest.DartUnitRunnerParameters;
import com.jetbrains.lang.dart.psi.DartCallExpression;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.jetbrains.lang.dart.ide.runner.unittest.DartUnitRunnerParameters.Scope;

public class DartUnitConfigurationEditorForm extends SettingsEditor<DartUnitRunConfiguration> {

  private class TestModel {
    private final List<DartCallExpression> myGroupCalls = new ArrayList<DartCallExpression>();
    private final List<DartCallExpression> myTestCalls = new ArrayList<DartCallExpression>();
    private final String myFilePath;

    TestModel(String filePath) {
      myFilePath = filePath;
      init();
    }

    private void init() {
      final String filePath = myFileField.getText();
      if (filePath != null) {
        final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null) {
          final PsiFile testFile = PsiManager.getInstance(myProject).findFile(file);
          if (testFile != null) {
            PsiElementProcessor<PsiElement> collector = new PsiElementProcessor<PsiElement>() {
              @Override
              public boolean execute(@NotNull final PsiElement element) {
                if (element instanceof DartCallExpression) {
                  DartCallExpression expression = (DartCallExpression)element;
                  if (DartUnitRunConfigurationProducer.isTest(expression)) {
                    myTestCalls.add(expression);
                  }
                  else if (DartUnitRunConfigurationProducer.isGroup(expression)) {
                    myGroupCalls.add(expression);
                  }
                }
                return true;
              }
            };

            PsiTreeUtil.processElements(testFile, collector);
          }
        }
      }
    }

    boolean includes(Scope scope, final String testLabel) {

      final Predicate<DartCallExpression> callExpressionPredicate = new Predicate<DartCallExpression>() {
        @Override
        public boolean apply(final DartCallExpression expression) {
          return DartTestLocationProvider.testLabelMatches(expression, testLabel);
        }
      };

      return scope == Scope.METHOD ?
             !Collections2.filter(myTestCalls, callExpressionPredicate).isEmpty() :
             !Collections2.filter(myGroupCalls, callExpressionPredicate).isEmpty();
    }

    boolean appliesTo(final String path) {
      return ObjectUtils.equals(myFilePath, path);
    }
  }


  private JPanel myMainPanel;
  private JComboBox myScopeCombo;
  private JLabel myTestFileLabel;
  private TextFieldWithBrowseButton myFileField;
  private JLabel myTestNameLabel;
  private JTextField myTestNameField;
  private RawCommandLineEditor myVMOptions;
  private JBCheckBox myCheckedModeCheckBox;
  private RawCommandLineEditor myArguments;
  private TextFieldWithBrowseButton myWorkingDirectory;
  private EnvironmentVariablesComponent myEnvironmentVariables;
  private final Project myProject;
  private TestModel myCachedModel;


  public DartUnitConfigurationEditorForm(Project project) {
    DartCommandLineConfigurationEditorForm.initDartFileTextWithBrowse(project, myFileField);
    myProject = project;

    myWorkingDirectory.addBrowseFolderListener(ExecutionBundle.message("select.working.directory.message"), null, project,
                                               FileChooserDescriptorFactory.createSingleFolderDescriptor());

    myScopeCombo.setModel(new EnumComboBoxModel<Scope>(Scope.class));
    myScopeCombo.setRenderer(new ListCellRendererWrapper<Scope>() {
      @Override
      public void customize(final JList list, final Scope value, final int index, final boolean selected, final boolean hasFocus) {
        setText(StringUtil.capitalize(value.toString().toLowerCase(Locale.US)));
      }
    });

    myScopeCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onScopeChanged();
        onTestNameChanged(); // Scope changes can invalidate test label
      }
    });

    myTestNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(final DocumentEvent e) {
        onTestNameChanged();
      }
    });

    myVMOptions.setDialogCaption(DartBundle.message("config.vmoptions.caption"));
    myArguments.setDialogCaption(DartBundle.message("config.progargs.caption"));

    // 'Environment variables' is the widest label, anchored by myTestFileLabel
    myTestFileLabel.setPreferredSize(myEnvironmentVariables.getLabel().getPreferredSize());
    myEnvironmentVariables.setAnchor(myTestFileLabel);
  }

  @Override
  protected void resetEditorFrom(DartUnitRunConfiguration configuration) {
    final DartUnitRunnerParameters parameters = configuration.getRunnerParameters();

    myScopeCombo.setSelectedItem(parameters.getScope());
    myFileField.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getFilePath())));
    myTestNameField.setText(parameters.getScope() == Scope.ALL ? "" : StringUtil.notNullize(parameters.getTestName()));
    myArguments.setText(StringUtil.notNullize(parameters.getArguments()));
    myVMOptions.setText(StringUtil.notNullize(parameters.getVMOptions()));
    myCheckedModeCheckBox.setSelected(parameters.isCheckedMode());
    myWorkingDirectory.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getWorkingDirectory())));
    myEnvironmentVariables.setEnvs(parameters.getEnvs());
    myEnvironmentVariables.setPassParentEnvs(parameters.isIncludeParentEnvs());

    onScopeChanged();
  }

  @Override
  protected void applyEditorTo(DartUnitRunConfiguration configuration) throws ConfigurationException {
    final DartUnitRunnerParameters parameters = configuration.getRunnerParameters();

    final Scope scope = (Scope)myScopeCombo.getSelectedItem();
    parameters.setScope(scope);
    parameters.setFilePath(StringUtil.nullize(FileUtil.toSystemIndependentName(myFileField.getText().trim()), true));
    parameters.setTestName(scope == Scope.ALL ? null : StringUtil.nullize(myTestNameField.getText()));
    parameters.setArguments(StringUtil.nullize(myArguments.getText(), true));
    parameters.setVMOptions(StringUtil.nullize(myVMOptions.getText(), true));
    parameters.setCheckedMode(myCheckedModeCheckBox.isSelected());
    parameters.setWorkingDirectory(StringUtil.nullize(FileUtil.toSystemIndependentName(myWorkingDirectory.getText().trim()), true));
    parameters.setEnvs(myEnvironmentVariables.getEnvs());
    parameters.setIncludeParentEnvs(myEnvironmentVariables.isPassParentEnvs());
  }

  private void onScopeChanged() {
    final Scope scope = (Scope)myScopeCombo.getSelectedItem();
    myTestNameLabel.setVisible(scope == Scope.GROUP || scope == Scope.METHOD);
    myTestNameField.setVisible(scope == Scope.GROUP || scope == Scope.METHOD);
    myTestNameLabel.setText(scope == Scope.GROUP
                            ? DartBundle.message("dart.unit.group.name")
                            : DartBundle.message("dart.unit.method.name"));
  }

  private void onTestNameChanged() {

    final String filePath = myFileField.getText();
    if (filePath == null || filePath.isEmpty()) {
      return;
    }

    final Scope scope = (Scope)myScopeCombo.getSelectedItem();
    final String testLabel = myTestNameField.getText();

    if (myCachedModel == null || !myCachedModel.appliesTo(filePath)) {
      myCachedModel = new TestModel(filePath);
    }

    if (!myCachedModel.includes(scope, myTestNameField.getText())) {
      myTestNameField.setForeground(JBColor.RED);
      myTestNameField.setToolTipText(DartBundle.message("config.test.unfound.label.warning", testLabel));
    } else {
      myTestNameField.setForeground(UIUtil.getFieldForegroundColor());
      myTestNameField.setToolTipText(null);
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
