package com.lioncorp.sonar.mulesoft;

import com.lioncorp.sonar.mulesoft.checks.CheckList;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Main sensor for scanning MuleSoft files.
 * This sensor processes each MuleSoft XML file and runs the defined rules against it.
 */
public class MuleSoftSensor implements Sensor {

  private static final Logger LOG = Loggers.get(MuleSoftSensor.class);

  private final Checks<MuleSoftCheck> checks;
  private final FileSystem fileSystem;

  public MuleSoftSensor(FileSystem fileSystem, CheckFactory checkFactory) {
    this.fileSystem = fileSystem;
    this.checks = checkFactory.<MuleSoftCheck>create(MuleSoftRulesDefinition.REPOSITORY_KEY)
        .addAnnotatedChecks(CheckList.getChecks());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .onlyOnLanguage(MuleSoftLanguage.KEY)
        .name("MuleSoft Sensor")
        .onlyOnFileType(InputFile.Type.MAIN);
  }

  @Override
  public void execute(SensorContext context) {
    LOG.info("MuleSoft Sensor starting. Loaded {} checks.", checks.all().size());

    Iterable<InputFile> inputFiles = fileSystem.inputFiles(
        fileSystem.predicates().hasLanguage(MuleSoftLanguage.KEY)
    );

    int fileCount = 0;
    for (InputFile inputFile : inputFiles) {
      fileCount++;
      try {
        scanFile(context, inputFile);
      } catch (Exception e) {
        LOG.error("Error scanning file: {}", inputFile.uri(), e);
      }
    }
    LOG.info("MuleSoft Sensor finished. Scanned {} MuleSoft files.", fileCount);
  }

  private void scanFile(SensorContext context, InputFile inputFile) {
    try {
      String content = inputFile.contents();

      // Check if this is a MuleSoft file by looking for MuleSoft namespaces
      if (!isMuleSoftFile(content)) {
        LOG.debug("Skipping non-MuleSoft XML file: {}", inputFile.filename());
        return;
      }

      LOG.debug("Scanning MuleSoft file: {}", inputFile.filename());

      // Parse the file
      MuleSoftFileParser parser = new MuleSoftFileParser();
      MuleSoftFileParser.ParsedMuleSoftFile parsedFile = parser.parse(content);

      // Run all checks
      for (MuleSoftCheck check : checks.all()) {
        check.scanFile(context, inputFile, parsedFile);
      }

    } catch (IOException e) {
      LOG.error("Error reading file: {}", inputFile.uri(), e);
    }
  }

  private boolean isMuleSoftFile(String content) {
    // Check for MuleSoft namespaces or mule root element
    return content.contains("http://www.mulesoft.org/schema/mule/") ||
           content.contains("xmlns:mule=") ||
           content.contains("<mule");
  }
}
