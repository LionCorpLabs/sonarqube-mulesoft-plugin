package com.lioncorp.sonar.mulesoft.utils;

import java.util.regex.Pattern;

/**
 * Utility class for pattern matching and regex operations.
 */
public class PatternMatcher {

  private PatternMatcher() {
    // Utility class
  }

  // Naming pattern constants
  private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z]+([A-Z][a-z0-9]*)*$");
  private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z]+(-[a-z0-9]+)*$");
  private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z]+(_[a-z0-9]+)*$");
  private static final Pattern VAGUE_NAME = Pattern.compile("^(test|temp|tmp|flow[0-9]*|data|example|sample|demo)[0-9]*$", Pattern.CASE_INSENSITIVE);

  // Credential patterns
  private static final Pattern CREDENTIAL_ATTR_PATTERN = Pattern.compile(
      "(password|passwd|pwd|secret|token|apikey|api_key|access_key|private_key|auth|credential)",
      Pattern.CASE_INSENSITIVE
  );

  // Property placeholder patterns
  private static final Pattern PROPERTY_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}");
  private static final Pattern MULE_EXPRESSION = Pattern.compile("#\\[[^\\]]+\\]");
  private static final Pattern SECURE_PROPERTY = Pattern.compile("\\$\\{secure::[^}]+\\}");

  // IP address pattern
  private static final Pattern IP_ADDRESS = Pattern.compile(
      "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"
  );

  // URL patterns
  private static final Pattern HTTP_URL = Pattern.compile(
      "https?://[^\\s]+",
      Pattern.CASE_INSENSITIVE
  );

  // Sensitive data patterns
  private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
  private static final Pattern CREDIT_CARD = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
  private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

  // Sensitive field name patterns
  private static final Pattern SENSITIVE_FIELD = Pattern.compile(
      "(password|passwd|pwd|secret|token|apikey|api_key|access_key|private_key|auth|credential|ssn|" +
      "social_security|credit_card|card_number|pin|bankaccount|account_number|routing_number|" +
      "private_data|confidential|sensitive|health|medical|biometric|dob|date_of_birth)",
      Pattern.CASE_INSENSITIVE
  );

  // SQL injection patterns
  private static final Pattern SQL_CONCAT = Pattern.compile(
      "(SELECT|INSERT|UPDATE|DELETE|FROM|WHERE).*\\+.*",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Check if a string matches camelCase naming convention.
   */
  public static boolean isCamelCase(String str) {
    return str != null && CAMEL_CASE.matcher(str).matches();
  }

  /**
   * Check if a string matches kebab-case naming convention.
   */
  public static boolean isKebabCase(String str) {
    return str != null && KEBAB_CASE.matcher(str).matches();
  }

  /**
   * Check if a string matches snake_case naming convention.
   */
  public static boolean isSnakeCase(String str) {
    return str != null && SNAKE_CASE.matcher(str).matches();
  }

  /**
   * Check if a name is vague or generic.
   */
  public static boolean isVagueName(String name) {
    return name != null && VAGUE_NAME.matcher(name).matches();
  }

  /**
   * Check if an attribute name suggests it contains credentials.
   */
  public static boolean isCredentialAttribute(String attributeName) {
    return attributeName != null && CREDENTIAL_ATTR_PATTERN.matcher(attributeName).find();
  }

  /**
   * Check if a value is a property placeholder like ${property}.
   */
  public static boolean isPropertyPlaceholder(String value) {
    return value != null && PROPERTY_PLACEHOLDER.matcher(value).find();
  }

  /**
   * Check if a value is a Mule expression like #[expression].
   */
  public static boolean isMuleExpression(String value) {
    return value != null && MULE_EXPRESSION.matcher(value).find();
  }

  /**
   * Check if a value is a secure property placeholder like ${secure::property}.
   */
  public static boolean isSecureProperty(String value) {
    return value != null && SECURE_PROPERTY.matcher(value).find();
  }

  /**
   * Check if a value is externalized (property or expression).
   */
  public static boolean isExternalized(String value) {
    return isPropertyPlaceholder(value) || isMuleExpression(value);
  }

  /**
   * Check if a string contains an IP address.
   */
  public static boolean containsIPAddress(String str) {
    return str != null && IP_ADDRESS.matcher(str).find();
  }

  /**
   * Check if a string contains an HTTP/HTTPS URL.
   */
  public static boolean containsHttpUrl(String str) {
    return str != null && HTTP_URL.matcher(str).find();
  }

  /**
   * Check if a string might contain a Social Security Number.
   */
  public static boolean containsSSN(String str) {
    return str != null && SSN.matcher(str).find();
  }

  /**
   * Check if a string might contain a credit card number.
   */
  public static boolean containsCreditCard(String str) {
    return str != null && CREDIT_CARD.matcher(str).find();
  }

  /**
   * Check if a string contains an email address.
   */
  public static boolean containsEmail(String str) {
    return str != null && EMAIL.matcher(str).find();
  }

  /**
   * Check if a string contains potential sensitive data.
   */
  public static boolean containsSensitiveData(String str) {
    return containsSSN(str) || containsCreditCard(str) || containsEmail(str);
  }

  /**
   * Check if a field name suggests it contains sensitive data.
   */
  public static boolean isSensitiveField(String fieldName) {
    return fieldName != null && SENSITIVE_FIELD.matcher(fieldName).find();
  }

  /**
   * Check if a string might be vulnerable to SQL injection (string concatenation pattern).
   */
  public static boolean hasSQLConcatenationPattern(String str) {
    return str != null && SQL_CONCAT.matcher(str).find();
  }

  /**
   * Count the number of excessive abbreviations in a name (3+ consecutive abbreviations).
   */
  public static boolean hasExcessiveAbbreviations(String name) {
    if (name == null) {
      return false;
    }
    // Count segments that are 1-2 chars long
    String[] segments = name.split("[-_]");
    int shortSegments = 0;
    for (String segment : segments) {
      if (segment.length() <= 2) {
        shortSegments++;
      }
    }
    return shortSegments >= 3;
  }

  /**
   * Check if name follows verb-noun pattern.
   */
  public static boolean followsVerbNounPattern(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }

    // Common verbs used in flow names
    String[] commonVerbs = {
        "get", "post", "put", "delete", "create", "update", "remove", "fetch",
        "process", "handle", "validate", "transform", "send", "receive",
        "calculate", "generate", "parse", "format", "convert", "execute"
    };

    String lowerName = name.toLowerCase();
    for (String verb : commonVerbs) {
      if (lowerName.startsWith(verb + "-") || lowerName.startsWith(verb + "_")) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if a value looks like a hardcoded credential (not a placeholder).
   */
  public static boolean isHardcodedValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      return false;
    }

    // Not hardcoded if it's a placeholder or expression
    return !isExternalized(value);
  }

  /**
   * Check if a protocol is cleartext (insecure).
   */
  public static boolean isCleartextProtocol(String protocol) {
    if (protocol == null) {
      return false;
    }
    String lower = protocol.toLowerCase();
    return lower.equals("http") || lower.equals("ftp") ||
           lower.equals("telnet") || lower.contains("plain");
  }

  /**
   * Check if encryption algorithm is weak.
   */
  public static boolean isWeakEncryption(String algorithm) {
    if (algorithm == null) {
      return false;
    }
    String lower = algorithm.toLowerCase();
    return lower.contains("des") && !lower.contains("aes") || // DES but not AES
           lower.contains("md5") ||
           lower.equals("sha1") ||
           lower.equals("sha-1");
  }

  /**
   * Count the number of boolean operators in an expression.
   */
  public static int countBooleanOperators(String expression) {
    if (expression == null) {
      return 0;
    }
    int count = 0;
    count += countOccurrences(expression, "&&");
    count += countOccurrences(expression, "||");
    count += countOccurrences(expression, " and ");
    count += countOccurrences(expression, " or ");
    return count;
  }

  /**
   * Count occurrences of a substring.
   */
  private static int countOccurrences(String str, String substring) {
    int count = 0;
    int index = 0;
    while ((index = str.indexOf(substring, index)) != -1) {
      count++;
      index += substring.length();
    }
    return count;
  }

  /**
   * Detect mixed naming conventions in a single name.
   */
  public static boolean hasMixedNamingConvention(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }

    boolean hasHyphen = name.contains("-");
    boolean hasUnderscore = name.contains("_");
    boolean hasUpperCase = !name.equals(name.toLowerCase());

    // Mixed if it has more than one style
    int styleCount = (hasHyphen ? 1 : 0) + (hasUnderscore ? 1 : 0) + (hasUpperCase ? 1 : 0);
    return styleCount > 1;
  }
}
