package com.jetbrains.profiler;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProfileFileTypeFactory extends FileTypeFactory {
  final static FileType instance = new FileType() {
    @NotNull
    public String getName() {
      return "Snapshot";
    }

    @NotNull
    public String getDescription() {
      return "Profiler Snapshot";
    }

    @NotNull
    public String getDefaultExtension() {
      return "";
    }

    public Icon getIcon() {
      return AllIcons.Actions.ProfileCPU;
    }

    public boolean isBinary() {
      return true;
    }

    public boolean isReadOnly() {
      return false;
    }

    public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
      return null;
    }
  };
  
  @Override
  public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
    fileTypeConsumer.consume(instance, "snapshot");
  }
}
