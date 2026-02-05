package com.lioncorp.sonar.mulesoft;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

/**
 * Defines the default quality profile for MuleSoft analysis.
 * Activates all 110 rules by default.
 */
public class MuleSoftQualityProfile implements BuiltInQualityProfilesDefinition {

  // All 110 MuleSoft rule IDs
  private static final String[] ALL_RULE_IDS = {
      // Security Vulnerabilities (MS001-MS015)
      "MS001", "MS002", "MS003", "MS004", "MS005", "MS006", "MS007", "MS008", "MS009", "MS010",
      "MS011", "MS012", "MS013", "MS014", "MS015",
      // Security Hotspots (MS016-MS030)
      "MS016", "MS017", "MS018", "MS019", "MS020", "MS021", "MS022", "MS023", "MS024", "MS025",
      "MS026", "MS027", "MS028", "MS029", "MS030",
      // Structure Rules (MS031-MS058)
      "MS031", "MS032", "MS033", "MS034", "MS035", "MS036", "MS037", "MS038", "MS039", "MS040",
      "MS041", "MS042", "MS043", "MS044", "MS045", "MS046", "MS047", "MS048", "MS049", "MS050",
      "MS051", "MS052", "MS053", "MS054", "MS055", "MS056", "MS057", "MS058",
      // Naming Rules (MS059-MS071)
      "MS059", "MS060", "MS061", "MS062", "MS063", "MS064", "MS065", "MS066", "MS067", "MS068",
      "MS069", "MS070", "MS071",
      // Performance Rules (MS072-MS084)
      "MS072", "MS073", "MS074", "MS075", "MS076", "MS077", "MS078", "MS079", "MS080", "MS081",
      "MS082", "MS083", "MS084",
      // Error Handling Rules (MS085-MS096)
      "MS085", "MS086", "MS087", "MS088", "MS089", "MS090", "MS091", "MS092", "MS093", "MS094",
      "MS095", "MS096",
      // Java Integration Rules (MS097-MS110)
      "MS097", "MS098", "MS099", "MS100", "MS101", "MS102", "MS103", "MS104", "MS105", "MS106",
      "MS107", "MS108", "MS109", "MS110"
  };

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
        "MuleSoft Way",
        MuleSoftLanguage.KEY
    );

    profile.setDefault(true);

    // Activate all 110 rules
    for (String ruleId : ALL_RULE_IDS) {
      profile.activateRule(MuleSoftRulesDefinition.REPOSITORY_KEY, ruleId);
    }

    profile.done();
  }
}
