package com.lioncorp.sonar.mulesoft;

import org.sonar.api.Plugin;

/**
 * Main entry point for the MuleSoft SonarQube plugin.
 * This plugin analyzes MuleSoft XML files with embedded Java code.
 */
public class MuleSoftPlugin implements Plugin {

  @Override
  public void define(Context context) {
    // Register MuleSoft as its own language
    context.addExtension(MuleSoftLanguage.class);

    // File sensor for scanning
    context.addExtension(MuleSoftSensor.class);

    // Rules
    context.addExtension(MuleSoftRulesDefinition.class);
    context.addExtension(MuleSoftQualityProfile.class);
  }
}
