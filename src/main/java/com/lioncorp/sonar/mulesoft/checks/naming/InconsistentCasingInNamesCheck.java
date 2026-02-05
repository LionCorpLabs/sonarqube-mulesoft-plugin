package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Inconsistent casing.
 */
@Rule(key = "MS061")
public class InconsistentCasingInNamesCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS061";
  }
  private static final String KEBAB_CASE = "kebab-case";
  private static final String SNAKE_CASE = "snake_case";
  private static final String CAMEL_CASE = "camelCase";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    String dominantStyle = determineDominantCasingStyle(parsedFile.flows);
    if (dominantStyle != null) {
      checkFlowNamingConsistency(context, inputFile, parsedFile.flows, dominantStyle);
    }
  }

  private void checkFlowNamingConsistency(SensorContext context, InputFile inputFile,
                                          java.util.List<MuleSoftFileParser.MuleSoftFlow> flows, String dominantStyle) {
    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (flow.name != null && !flow.name.isEmpty()) {
        String actualStyle = detectCasingStyle(flow.name);
        if (!dominantStyle.equals(actualStyle)) {
          String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
          reportIssue(context, inputFile,
              flowType + " '" + flow.name + "' uses " + actualStyle + " but the project predominantly uses " + dominantStyle + ". Maintain consistent naming conventions.");
        }
      }
    }
  }

  private String determineDominantCasingStyle(java.util.List<MuleSoftFileParser.MuleSoftFlow> flows) {
    int kebabCount = 0;
    int snakeCount = 0;
    int camelCount = 0;

    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (flow.name != null && !flow.name.isEmpty()) {
        String style = detectCasingStyle(flow.name);
        switch (style) {
          case KEBAB_CASE:
            kebabCount++;
            break;
          case SNAKE_CASE:
            snakeCount++;
            break;
          case CAMEL_CASE:
            camelCount++;
            break;
          default:
            // Ignore unknown styles
            break;
        }
      }
    }

    return getDominantStyle(kebabCount, snakeCount, camelCount);
  }

  private String getDominantStyle(int kebabCount, int snakeCount, int camelCount) {
    int maxCount = Math.max(kebabCount, Math.max(snakeCount, camelCount));
    if (maxCount == 0) {
      return null;
    }
    if (maxCount == kebabCount) {
      return KEBAB_CASE;
    } else if (maxCount == snakeCount) {
      return SNAKE_CASE;
    } else {
      return CAMEL_CASE;
    }
  }

  private String detectCasingStyle(String name) {
    if (name.contains("-")) {
      return KEBAB_CASE;
    } else if (name.contains("_")) {
      return SNAKE_CASE;
    } else if (name.matches(".*[a-z][A-Z].*")) {
      return CAMEL_CASE;
    }
    return "unknown";
  }
}
