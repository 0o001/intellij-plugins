// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.cli;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.intellij.javascript.nodejs.library.NodeModulesDirectoryManager;
import com.intellij.javascript.nodejs.packageJson.InstalledPackageVersion;
import com.intellij.javascript.nodejs.packageJson.NodePackageBasicInfo;
import com.intellij.javascript.nodejs.packageJson.NpmRegistryService;
import com.intellij.lang.javascript.service.JSLanguageServiceUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.intellij.util.ObjectUtils.doIfNotNull;
import static com.intellij.util.ObjectUtils.notNull;

public class AngularCliSchematicsRegistryServiceImpl extends AngularCliSchematicsRegistryService {

  private static final String USER_AGENT = "JetBrains IDE";
  private static final String NG_PACKAGES_URL =
    "https://raw.githubusercontent.com/JetBrains/intellij-plugins/master/AngularJS/resources/org/angularjs/cli/ng-packages.json";

  private static final Logger LOG = Logger.getInstance(AngularCliSchematicsRegistryServiceImpl.class);
  private static final int CACHE_EXPIRY = 25 * 60 * 1000; //25 mins
  private static final ExecutorService ourExecutorService =
    AppExecutorUtil.createBoundedApplicationPoolExecutor("Angular CLI Schematics Registry Pool", 5);
  private static final Key<com.intellij.psi.util.CachedValue<List<Schematic>>> SCHEMATICS_PUBLIC =
    new Key<>("angular.cli.schematics.public");
  private static final Key<com.intellij.psi.util.CachedValue<List<Schematic>>> SCHEMATICS_ALL = new Key<>("angular.cli.schematics.all");
  private static final SimpleModificationTracker SCHEMATICS_CACHE_TRACKER = new SimpleModificationTracker();

  private final CachedValue<List<NodePackageBasicInfo>> myNgAddPackages = new CachedValue<>(
    AngularCliSchematicsRegistryServiceImpl::fetchPackagesSupportingNgAdd);
  private final Map<String, Pair<Boolean, Long>> myLocalNgAddPackages = ContainerUtil.newConcurrentMap();
  private final Map<String, CachedValue<Boolean>> myNgAddSupportedCache = ContainerUtil.newConcurrentMap();


  @NotNull
  @Override
  public List<NodePackageBasicInfo> getPackagesSupportingNgAdd(long timeout) {
    return ContainerUtil.notNullize(myNgAddPackages.getValue(timeout));
  }

  @Override
  public boolean supportsNgAdd(@NotNull String packageName, long timeout) {
    return getPackagesSupportingNgAdd(timeout).stream().anyMatch(pkg -> packageName.equals(pkg.getName()));
  }

  @Override
  public boolean supportsNgAdd(@NotNull String packageName,
                               @NotNull String versionOrRange,
                               long timeout) {
    return supportsNgAdd(packageName, timeout)
           && Boolean.TRUE.equals(myNgAddSupportedCache.computeIfAbsent(
      getKey(packageName, versionOrRange),
      k -> new CachedValue<>(() -> checkForNgAddSupport(packageName, versionOrRange))).getValue(timeout));
  }

  @Override
  public boolean supportsNgAdd(@NotNull InstalledPackageVersion version) {
    try {
      if (version.getPackageJson() != null) {
        return myLocalNgAddPackages.compute(version.getPackageJson().getPath(), (key, curValue) -> {
          if (curValue != null && version.getPackageJson().getModificationStamp() == curValue.getSecond()) {
            return curValue;
          }
          try {
            File schematicsCollection = getSchematicsCollection(new File(version.getPackageJson().getPath()));
            return Pair
              .create(schematicsCollection != null && hasNgAddSchematic(schematicsCollection),
                      version.getPackageJson().getModificationStamp());
          }
          catch (IOException e) {
            return Pair.create(false, version.getPackageJson().getModificationStamp());
          }
        }).getFirst();
      }
    }
    catch (Exception e) {
      LOG.info("Failed to retrieve schematics info for " + version.getPackageDir().getName(), e);
    }
    return false;
  }

  @NotNull
  @Override
  public List<Schematic> getSchematics(@NotNull Project project,
                                       @NotNull VirtualFile cliFolder,
                                       boolean includeHidden,
                                       boolean logErrors) {
    return notNull(doIfNotNull(
      ReadAction.compute(() -> PsiManager.getInstance(project).findDirectory(cliFolder)),
      cliDir -> CachedValuesManager.getCachedValue(cliDir, includeHidden ? SCHEMATICS_ALL : SCHEMATICS_PUBLIC, () ->
        CachedValueProvider.Result.create(
          SchematicsLoaderKt.doLoad(cliDir.getProject(), cliDir.getVirtualFile(), includeHidden, logErrors),
          NodeModulesDirectoryManager.getInstance(cliDir.getProject()).getNodeModulesDirChangeTracker(),
          SCHEMATICS_CACHE_TRACKER))), Collections.emptyList());
  }

  @Override
  public void clearProjectSchematicsCache() {
    SCHEMATICS_CACHE_TRACKER.incModificationCount();
  }

  @NotNull
  private static List<NodePackageBasicInfo> fetchPackagesSupportingNgAdd() {
    try {
      RequestBuilder builder = HttpRequests.request(NG_PACKAGES_URL);
      builder.userAgent(USER_AGENT);
      builder.gzip(true);
      return readNgAddPackages(builder.readString(null));
    }
    catch (IOException e) {
      LOG.info("Failed to load current list of ng-add compatible packages.", e);
      try (InputStream is = AngularCliSchematicsRegistryServiceImpl.class.getResourceAsStream("ng-packages.json")) {
        return readNgAddPackages(FileUtil.loadTextAndClose(new InputStreamReader(is, StandardCharsets.UTF_8)));
      }
      catch (Exception e1) {
        LOG.error("Failed to load list of ng-add compatible packages from static file.", e1);
      }
    }
    return Collections.emptyList();
  }

  @NotNull
  private static List<NodePackageBasicInfo> readNgAddPackages(@NotNull String content) {
    JsonObject contents = (JsonObject)new JsonParser().parse(content);
    return Collections.unmodifiableList(
      ContainerUtil.map(
        contents.get("ng-add").getAsJsonObject().entrySet(),
        e -> new NodePackageBasicInfo(e.getKey(), e.getValue().getAsString())));
  }

  @Nullable
  private static File getSchematicsCollection(@NotNull File packageJson) throws IOException {
    try (JsonReader reader = new JsonReader(new FileReader(packageJson))) {
      reader.beginObject();
      while (reader.hasNext()) {
        String key = reader.nextName();
        if (key.equals("schematics")) {
          String path = reader.nextString();
          return Paths.get(packageJson.getParent(), path).normalize().toAbsolutePath().toFile();
        }
        else {
          reader.skipValue();
        }
      }
      return null;
    }
  }

  private static boolean hasNgAddSchematic(@NotNull File schematicsCollection) throws IOException {
    try (JsonReader reader = new JsonReader(new FileReader(schematicsCollection))) {
      return hasNgAddSchematic(reader);
    }
  }

  public static boolean hasNgAddSchematic(@NotNull JsonReader reader) throws IOException {
    reader.setLenient(true);
    reader.beginObject();
    while (reader.hasNext()) {
      String key = reader.nextName();
      if ("schematics".equals(key)) {
        reader.beginObject();
        while (reader.hasNext()) {
          String schematicName = reader.nextName();
          if (schematicName.equals("ng-add")) {
            return true;
          }
          reader.skipValue();
        }
        reader.endObject();
      }
      else {
        reader.skipValue();
      }
    }
    reader.endObject();
    return false;
  }

  private static boolean checkForNgAddSupport(@NotNull String packageName, @NotNull String versionOrRange) {
    try {
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      JsonObject pkgJson = NpmRegistryService.getInstance().fetchPackageJson(packageName, versionOrRange, indicator);
      return pkgJson != null && pkgJson.get("schematics") != null;
    }
    catch (Exception e) {
      LOG.info(e);
    }
    return false;
  }

  private static String getKey(@NotNull String packageName,
                               @NotNull String version) {
    return packageName + "@" + version;
  }

  private static class CachedValue<T> {

    private long myUpdateTime;
    private Future<T> myCacheComputation;
    private T myCachedValue;
    private final Callable<T> myValueSupplier;

    CachedValue(Callable<T> valueSupplier) {
      this.myValueSupplier = valueSupplier;
    }

    protected synchronized boolean isCacheExpired() {
      return myUpdateTime + CACHE_EXPIRY <= System.currentTimeMillis();
    }

    @SuppressWarnings("SynchronizeOnThis")
    @Nullable
    public T getValue(long timeout) {
      Future<T> cacheComputation;
      synchronized (this) {
        if (myCachedValue != null && !isCacheExpired()) {
          return myCachedValue;
        }
        if (myCacheComputation == null) {
          myCachedValue = null;
          myCacheComputation = ourExecutorService.submit(myValueSupplier);
        }
        cacheComputation = myCacheComputation;
      }
      T result = JSLanguageServiceUtil.awaitFuture(cacheComputation, timeout, 10,
                                                   null, false, null);
      synchronized (this) {
        if (myCacheComputation != null && myCacheComputation.isDone()) {
          try {
            result = myCachedValue = myCacheComputation.get();
          }
          catch (InterruptedException | CancellationException ex) {
            //ignore
          }
          catch (ExecutionException e) {
            LOG.error(e);
          }
          myCacheComputation = null;
          myUpdateTime = System.currentTimeMillis();
        }
      }
      return result;
    }
  }
}
