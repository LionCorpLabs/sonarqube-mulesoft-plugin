package com.lioncorp.sonar.mulesoft;

import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;

/**
 * Base interface for all MuleSoft checks/rules.
 */
public interface MuleSoftCheck {

  /**
   * Scan a MuleSoft file and report issues.
   *
   * @param context the sensor context
   * @param inputFile the file being scanned
   * @param parsedFile the parsed MuleSoft file
   */
  void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile);
}
