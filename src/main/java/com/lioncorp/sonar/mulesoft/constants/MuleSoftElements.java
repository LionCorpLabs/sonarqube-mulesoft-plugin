package com.lioncorp.sonar.mulesoft.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Constants for MuleSoft XML element names and categories.
 * Centralizes element type checking logic used across multiple checks.
 */
public class MuleSoftElements {

    private MuleSoftElements() {
        // Constants class - prevent instantiation
    }

    // Flow structures
    public static final String FLOW = "flow";
    public static final String SUB_FLOW = "sub-flow";
    public static final String FLOW_REF = "flow-ref";

    // Conditional structures
    public static final String CHOICE = "choice";
    public static final String WHEN = "when";
    public static final String OTHERWISE = "otherwise";

    // Loop structures
    public static final String FOREACH = "foreach";
    public static final String FOR_EACH = "for-each";
    public static final String WHILE = "while";
    public static final String UNTIL_SUCCESSFUL = "until-successful";

    // Async structures
    public static final String ASYNC = "async";
    public static final String SCATTER_GATHER = "scatter-gather";

    // Error handling
    public static final String ERROR_HANDLER = "error-handler";
    public static final String ON_ERROR_CONTINUE = "on-error-continue";
    public static final String ON_ERROR_PROPAGATE = "on-error-propagate";
    public static final String TRY = "try";

    // Scope and transaction
    public static final String SCOPE = "scope";
    public static final String TRANSACTION = "transaction";
    public static final String BATCH_JOB = "batch:job";
    public static final String BATCH_STEP = "batch:step";

    // Data transformation
    public static final String TRANSFORM = "ee:transform";
    public static final String SET_PAYLOAD = "set-payload";
    public static final String SET_VARIABLE = "set-variable";

    // HTTP elements
    public static final String HTTP_LISTENER = "http:listener";
    public static final String HTTP_REQUEST = "http:request";
    public static final String HTTP_REQUEST_CONFIG = "http:request-config";

    // Database elements
    public static final String DB_SELECT = "db:select";
    public static final String DB_INSERT = "db:insert";
    public static final String DB_UPDATE = "db:update";
    public static final String DB_DELETE = "db:delete";
    public static final String DB_BULK_INSERT = "db:bulk-insert";

    // Logging
    public static final String LOGGER = "logger";

    // Element categories
    private static final Set<String> CONDITIONAL_ELEMENTS = new HashSet<>(Arrays.asList(
            CHOICE, WHEN, OTHERWISE
    ));

    private static final Set<String> LOOP_ELEMENTS = new HashSet<>(Arrays.asList(
            FOREACH, FOR_EACH, WHILE, UNTIL_SUCCESSFUL
    ));

    private static final Set<String> ASYNC_ELEMENTS = new HashSet<>(Arrays.asList(
            ASYNC, SCATTER_GATHER
    ));

    private static final Set<String> ERROR_HANDLING_ELEMENTS = new HashSet<>(Arrays.asList(
            ERROR_HANDLER, ON_ERROR_CONTINUE, ON_ERROR_PROPAGATE, TRY
    ));

    private static final Set<String> NESTING_ELEMENTS = new HashSet<>(Arrays.asList(
            CHOICE, WHEN, OTHERWISE, FOREACH, FOR_EACH, WHILE,
            TRY, SCOPE, ASYNC, UNTIL_SUCCESSFUL, SCATTER_GATHER
    ));

    private static final Set<String> FLOW_STRUCTURES = new HashSet<>(Arrays.asList(
            FLOW, SUB_FLOW
    ));

    /**
     * Check if a tag name represents a conditional structure.
     *
     * @param tagName the tag name to check
     * @return true if the element is a conditional structure
     */
    public static boolean isConditional(String tagName) {
        return tagName != null && (CONDITIONAL_ELEMENTS.contains(tagName) || tagName.contains("if"));
    }

    /**
     * Check if a tag name represents a loop structure.
     *
     * @param tagName the tag name to check
     * @return true if the element is a loop structure
     */
    public static boolean isLoop(String tagName) {
        return tagName != null && LOOP_ELEMENTS.contains(tagName);
    }

    /**
     * Check if a tag name represents an async structure.
     *
     * @param tagName the tag name to check
     * @return true if the element is an async structure
     */
    public static boolean isAsync(String tagName) {
        return tagName != null && ASYNC_ELEMENTS.contains(tagName);
    }

    /**
     * Check if a tag name represents an error handling structure.
     *
     * @param tagName the tag name to check
     * @return true if the element is an error handling structure
     */
    public static boolean isErrorHandling(String tagName) {
        return tagName != null && ERROR_HANDLING_ELEMENTS.contains(tagName);
    }

    /**
     * Check if a tag name represents a nesting element (increases complexity/depth).
     *
     * @param tagName the tag name to check
     * @return true if the element is a nesting element
     */
    public static boolean isNestingElement(String tagName) {
        return tagName != null && NESTING_ELEMENTS.contains(tagName);
    }

    /**
     * Check if a tag name represents a flow structure.
     *
     * @param tagName the tag name to check
     * @return true if the element is a flow or sub-flow
     */
    public static boolean isFlowStructure(String tagName) {
        return tagName != null && FLOW_STRUCTURES.contains(tagName);
    }

    /**
     * Check if a tag name represents an HTTP element.
     *
     * @param tagName the tag name to check
     * @return true if the element is HTTP-related
     */
    public static boolean isHttpElement(String tagName) {
        return tagName != null && tagName.startsWith("http:");
    }

    /**
     * Check if a tag name represents a database element.
     *
     * @param tagName the tag name to check
     * @return true if the element is database-related
     */
    public static boolean isDatabaseElement(String tagName) {
        return tagName != null && tagName.startsWith("db:");
    }

    /**
     * Check if a tag name represents a batch processing element.
     *
     * @param tagName the tag name to check
     * @return true if the element is batch-related
     */
    public static boolean isBatchElement(String tagName) {
        return tagName != null && tagName.startsWith("batch:");
    }
}
