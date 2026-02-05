package com.lioncorp.sonar.mulesoft.utils;

/**
 * Utility class for string operations commonly used in checks.
 * Provides simplified pattern matching and text analysis.
 */
public class StringUtils {

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if text contains any of the given patterns (case-insensitive).
     *
     * @param text     the text to search in
     * @param patterns the patterns to search for
     * @return true if text contains at least one pattern
     */
    public static boolean containsAnyIgnoreCase(String text, String... patterns) {
        if (text == null || patterns == null || patterns.length == 0) {
            return false;
        }

        String lowerText = text.toLowerCase();
        for (String pattern : patterns) {
            if (pattern != null && lowerText.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if text contains all of the given patterns (case-insensitive).
     *
     * @param text     the text to search in
     * @param patterns the patterns that must all be present
     * @return true if text contains all patterns
     */
    public static boolean containsAllIgnoreCase(String text, String... patterns) {
        if (text == null || patterns == null || patterns.length == 0) {
            return false;
        }

        String lowerText = text.toLowerCase();
        for (String pattern : patterns) {
            if (pattern == null || !lowerText.contains(pattern.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if text contains the pattern (case-insensitive).
     *
     * @param text    the text to search in
     * @param pattern the pattern to search for
     * @return true if text contains the pattern
     */
    public static boolean containsIgnoreCase(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        return text.toLowerCase().contains(pattern.toLowerCase());
    }

    /**
     * Count the number of boolean operators in an expression.
     * Counts: and, or, AND, OR, &&, ||
     *
     * @param expression the expression to analyze
     * @return the number of boolean operators found
     */
    public static int countBooleanOperators(String expression) {
        if (expression == null || expression.isEmpty()) {
            return 0;
        }

        int count = 0;
        String lowerExpr = expression.toLowerCase();

        // Count word operators with word boundaries
        count += countOccurrences(lowerExpr, " and ");
        count += countOccurrences(lowerExpr, " or ");

        // Count symbol operators
        count += countOccurrences(expression, "&&");
        count += countOccurrences(expression, "||");

        return count;
    }

    /**
     * Count occurrences of a pattern in text.
     *
     * @param text    the text to search in
     * @param pattern the pattern to count
     * @return the number of occurrences
     */
    public static int countOccurrences(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * Check if a string is null or empty.
     *
     * @param str the string to check
     * @return true if the string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Check if a string is null, empty, or contains only whitespace.
     *
     * @param str the string to check
     * @return true if the string is null, empty, or whitespace only
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Safe toLowerCase operation that handles null.
     *
     * @param str the string to convert
     * @return lowercase string, or empty string if input is null
     */
    public static String lowerCase(String str) {
        return str != null ? str.toLowerCase() : "";
    }

    /**
     * Check if any of the patterns match the entire string (case-insensitive).
     *
     * @param text     the text to match
     * @param patterns the patterns to match against
     * @return true if text equals any pattern (ignoring case)
     */
    public static boolean equalsAnyIgnoreCase(String text, String... patterns) {
        if (text == null || patterns == null) {
            return false;
        }

        for (String pattern : patterns) {
            if (pattern != null && text.equalsIgnoreCase(pattern)) {
                return true;
            }
        }
        return false;
    }
}
