package com.intellij.javascript.karma.coverage;

import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageRunner;
import com.intellij.coverage.CoverageSuite;
import com.intellij.coverage.SimpleCoverageAnnotator;
import com.intellij.javascript.karma.KarmaConfig;
import com.intellij.javascript.karma.server.KarmaServer;
import com.intellij.javascript.testFramework.coverage.CoverageSerializationUtils;
import com.intellij.javascript.testFramework.coverage.LcovCoverageReport;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Sergey Simonchik
 */
public class KarmaCoverageRunner extends CoverageRunner {

  private static final Logger LOG = Logger.getInstance(KarmaCoverageRunner.class);
  private KarmaServer myKarmaServer;

  @Override
  public ProjectData loadCoverageData(@NotNull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
    KarmaConfig karmaConfig = null;
    if (myKarmaServer != null) {
      karmaConfig = myKarmaServer.getKarmaConfig();
    }
    String basePath = null;
    if (karmaConfig != null) {
      basePath = karmaConfig.getBasePath();
    }
    if (basePath != null) {
      File basePathDir = new File(basePath);
      if (basePathDir.isAbsolute() && basePathDir.isDirectory()) {
        try {
          return readProjectData(sessionDataFile, basePathDir);
        }
        catch (Exception e) {
          LOG.warn("Can't read coverage data", e);
        }
      }
    }
    return null;
  }

  public void setKarmaServer(@NotNull KarmaServer karmaServer) {
    myKarmaServer = karmaServer;
  }

  @NotNull
  private static ProjectData readProjectData(@NotNull File dataFile, @NotNull File basePath) throws IOException {
    ProjectData projectData = new ProjectData();
    LcovCoverageReport report = CoverageSerializationUtils.readLCOV(basePath, dataFile);
    for (Map.Entry<String, List<LcovCoverageReport.LineHits>> entry : report.getInfo().entrySet()) {
      String filePath = SimpleCoverageAnnotator.getFilePath(entry.getKey());
      ClassData classData = projectData.getOrCreateClassData(filePath);
      int max = 0;
      List<LcovCoverageReport.LineHits> lineHitsList = entry.getValue();
      if (lineHitsList.size() > 0) {
        LcovCoverageReport.LineHits lastLineHits = lineHitsList.get(lineHitsList.size() - 1);
        max = lastLineHits.getLineNumber();
      }
      LineData[] lines = new LineData[max + 1];
      for (LcovCoverageReport.LineHits lineHits : lineHitsList) {
        LineData lineData = new LineData(lineHits.getLineNumber(), null);
        lineData.setHits(lineHits.getHits());
        lines[lineHits.getLineNumber()] = lineData;
      }
      classData.setLines(lines);
    }
    return projectData;
  }

  @Override
  public String getPresentableName() {
    return "KarmaPresentableName";
  }

  @Override
  public String getId() {
    return KarmaCoverageEngine.ID;
  }

  @Override
  public String getDataFileExtension() {
    return "dat";
  }

  @Override
  public boolean acceptsCoverageEngine(@NotNull CoverageEngine engine) {
    return engine instanceof KarmaCoverageEngine;
  }
}
