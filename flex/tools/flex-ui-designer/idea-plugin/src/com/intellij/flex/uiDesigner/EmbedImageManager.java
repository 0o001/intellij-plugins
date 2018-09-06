package com.intellij.flex.uiDesigner;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmbedImageManager extends EmbedAssetManager<ImageAssetInfo> {
  public static EmbedImageManager getInstance() {
    return DesignerApplicationManager.getService(EmbedImageManager.class);
  }

  public int add(@NotNull VirtualFile file, @Nullable String mimeType, @NotNull AssetCounter assetCounter) {
    for (ImageAssetInfo asset : assets) {
      if (asset.file.equals(file)) {
        return asset.id;
      }
    }

    assetCounter.imageCount++;
    final int id = idPool.allocate();
    add(new ImageAssetInfo(file, mimeType, id));
    return id;
  }

  @SuppressWarnings("MethodOverloadsMethodOfSuperclass")
  public void remove(@NotNull VirtualFile file) {
    for (ImageAssetInfo asset : assets) {
      if (asset.file.equals(file)) {
        remove(asset.id);
      }
    }
  }
}

class ImageAssetInfo extends EmbedAssetInfo {
  public final String mimeType;

  ImageAssetInfo(VirtualFile file, @Nullable String mimeType, int id) {
    super(file, id);
    this.mimeType = mimeType;
  }
}