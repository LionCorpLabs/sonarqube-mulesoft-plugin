package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.MuleSoftCheck;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * Base class for all MuleSoft checks.
 * Provides common functionality for issue reporting and reduces code duplication.
 */
public abstract class BaseCheck implements MuleSoftCheck {

    protected static final String REPOSITORY_KEY = "mulesoft";

    /**
     * Returns the rule key for this check.
     * Each check must implement this to provide its unique rule identifier.
     *
     * @return the rule key (e.g., "MS001", "MS002")
     */
    protected abstract String getRuleKey();

    /**
     * Reports an issue without a specific line number.
     * The issue will be reported at the file level.
     *
     * @param context    the sensor context
     * @param inputFile  the file being analyzed
     * @param message    the issue message
     */
    protected void reportIssue(SensorContext context, InputFile inputFile, String message) {
        NewIssue issue = context.newIssue();
        NewIssueLocation location = issue.newLocation()
                .on(inputFile)
                .message(message);
        issue
                .at(location)
                .forRule(RuleKey.of(REPOSITORY_KEY, getRuleKey()))
                .save();
    }

    /**
     * Reports an issue at a specific line number.
     *
     * @param context    the sensor context
     * @param inputFile  the file being analyzed
     * @param message    the issue message
     * @param lineNumber the line number where the issue occurs
     */
    protected void reportIssue(SensorContext context, InputFile inputFile, String message, int lineNumber) {
        try {
            NewIssue issue = context.newIssue();
            NewIssueLocation location = issue.newLocation()
                    .on(inputFile)
                    .at(inputFile.selectLine(lineNumber))
                    .message(message);
            issue
                    .at(location)
                    .forRule(RuleKey.of(REPOSITORY_KEY, getRuleKey()))
                    .save();
        } catch (IllegalArgumentException e) {
            // Line number is invalid, report at file level
            reportIssue(context, inputFile, message);
        }
    }
}
