package com.lioncorp.sonar.mulesoft;

import org.sonar.api.resources.AbstractLanguage;

/**
 * Defines the MuleSoft language for SonarQube.
 * This allows SonarQube to recognize and process MuleSoft XML files.
 */
public class MuleSoftLanguage extends AbstractLanguage {

  public static final String NAME = "MuleSoft";
  public static final String KEY = "mulesoft";

  private static final String[] MULESOFT_FILE_SUFFIXES = {"xml"};

  public MuleSoftLanguage() {
    super(KEY, NAME);
  }

  @Override
  public String[] getFileSuffixes() {
    return MULESOFT_FILE_SUFFIXES;
  }
}
