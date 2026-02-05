package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Inconsistent naming conventions across flows.
 */
@Rule(key = "MS051")
public class InconsistentNamingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS051";
  }

  private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
  private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z][a-z0-9-]*$");
  private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9_]*$");
  private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.flows.size() < 2) {
      return;
    }

    Map<NamingStyle, Integer> styleUsage = analyzeNamingStyles(parsedFile.flows);

    if (styleUsage.size() > 1) {
      int totalFlows = parsedFile.flows.size();
      NamingStyle dominantStyle = findDominantStyle(styleUsage);
      int dominantCount = styleUsage.get(dominantStyle);

      if (dominantCount < totalFlows * 0.8) {
        reportIssue(context, inputFile,
            "Inconsistent naming convention detected across flows. " +
            "Multiple naming styles found: " + formatStyleUsage(styleUsage) + ". " +
            "Choose one convention (e.g., kebab-case: 'get-customer-data') and apply it consistently.");
      }
    }

    checkFlowNamingConsistency(context, inputFile, parsedFile.flows);
  }

  private Map<NamingStyle, Integer> analyzeNamingStyles(List<MuleSoftFileParser.MuleSoftFlow> flows) {
    Map<NamingStyle, Integer> styleCounts = new EnumMap<>(NamingStyle.class);

    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (flow.name == null || flow.name.isEmpty()) {
        continue;
      }

      NamingStyle style = detectNamingStyle(flow.name);
      styleCounts.put(style, styleCounts.getOrDefault(style, 0) + 1);
    }

    return styleCounts;
  }

  private NamingStyle detectNamingStyle(String name) {
    if (CAMEL_CASE.matcher(name).matches()) {
      return NamingStyle.CAMEL_CASE;
    } else if (PASCAL_CASE.matcher(name).matches()) {
      return NamingStyle.PASCAL_CASE;
    } else if (KEBAB_CASE.matcher(name).matches()) {
      return NamingStyle.KEBAB_CASE;
    } else if (SNAKE_CASE.matcher(name).matches()) {
      return NamingStyle.SNAKE_CASE;
    } else {
      return NamingStyle.MIXED;
    }
  }

  private NamingStyle findDominantStyle(Map<NamingStyle, Integer> styleUsage) {
    return styleUsage.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(NamingStyle.MIXED);
  }

  private String formatStyleUsage(Map<NamingStyle, Integer> styleUsage) {
    List<String> parts = new ArrayList<>();
    for (Map.Entry<NamingStyle, Integer> entry : styleUsage.entrySet()) {
      parts.add(entry.getKey().getDisplayName() + " (" + entry.getValue() + ")");
    }
    return String.join(", ", parts);
  }

  private void checkFlowNamingConsistency(SensorContext context, InputFile inputFile,
                                           List<MuleSoftFileParser.MuleSoftFlow> flows) {
    Map<String, List<String>> prefixGroups = new HashMap<>();

    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (flow.name == null || flow.name.isEmpty()) {
        continue;
      }

      String prefix = extractPrefix(flow.name);
      if (prefix != null) {
        prefixGroups.computeIfAbsent(prefix, k -> new ArrayList<>()).add(flow.name);
      }
    }

    for (Map.Entry<String, List<String>> entry : prefixGroups.entrySet()) {
      if (entry.getValue().size() >= 3) {
        Set<NamingStyle> styles = new HashSet<>();
        for (String flowName : entry.getValue()) {
          styles.add(detectNamingStyle(flowName));
        }

        if (styles.size() > 1) {
          reportIssue(context, inputFile,
              "Flows with prefix '" + entry.getKey() + "' use inconsistent naming styles. " +
              "Related flows should follow the same naming convention.");
        }
      }
    }
  }

  private String extractPrefix(String flowName) {
    String[] parts = flowName.split("[-_]");
    if (parts.length > 1) {
      return parts[0];
    }

    if (flowName.length() > 3 && Character.isUpperCase(flowName.charAt(1))) {
      for (int i = 1; i < flowName.length(); i++) {
        if (Character.isUpperCase(flowName.charAt(i))) {
          return flowName.substring(0, i);
        }
      }
    }

    return null;
  }

  private enum NamingStyle {
    CAMEL_CASE("camelCase"),
    PASCAL_CASE("PascalCase"),
    KEBAB_CASE("kebab-case"),
    SNAKE_CASE("snake_case"),
    MIXED("mixed");

    private final String displayName;

    NamingStyle(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
