package org.angularjs.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import org.angularjs.settings.AngularJSConfig;

/**
 * @author Dennis.Ushakov
 */
public class AngularBracesInterpolationTypedHandler extends TypedHandlerDelegate {
  @Override
  public Result beforeCharTyped(char c, Project project, Editor editor, PsiFile file, FileType fileType) {
    if (file.getViewProvider() instanceof MultiplePsiFilesPerDocumentFileViewProvider ||
        DumbService.isDumb(project)) return Result.CONTINUE;

    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return Result.DEFAULT;

    // we should use AngularJSBracesUtil here
    if (file.getFileType() == HtmlFileType.INSTANCE) {
      if (c == '{') {
        if (!AngularJSBracesUtil.DEFAULT_START.equals(AngularJSBracesUtil.getInjectionStart(project)) ||
            !AngularJSBracesUtil.DEFAULT_END.equals(AngularJSBracesUtil.getInjectionEnd(project))) return Result.CONTINUE;
        boolean addWhiteSpaceBetweenBraces = AngularJSConfig.getInstance().INSERT_WHITESPACE;
        int offset = editor.getCaretModel().getOffset();
        String chars = editor.getDocument().getText();
        if (offset > 0 && (chars.charAt(offset - 1)) == '{') {
          if (offset < 2 || (chars.charAt(offset - 2)) != '{') {
            if (alreadyHasEnding(chars, offset)) {
              return Result.CONTINUE;
            }
            else {
              String interpolation = addWhiteSpaceBetweenBraces ? "{  }" : "{}";

              if (offset == chars.length() || (offset < chars.length() && chars.charAt(offset) != '}')) {
                interpolation += "}";
              }

              EditorModificationUtil.insertStringAtCaret(editor, interpolation, true, addWhiteSpaceBetweenBraces ? 2 : 1);
              return Result.STOP;
            }
          }
        }
      }
    }

    return Result.CONTINUE;
  }

  private static boolean alreadyHasEnding(String chars, int offset) {
    int i = offset;
    while (i < chars.length() && (chars.charAt(i) != '{' && chars.charAt(i) != '}' && chars.charAt(i) != '\n')) {
      i++;
    }
    return i + 1 < chars.length() && chars.charAt(i) == '}' && chars.charAt(i + 1) == '}';
  }
}
