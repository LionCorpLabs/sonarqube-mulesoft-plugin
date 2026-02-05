package com.lioncorp.sonar.mulesoft.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Constants and thresholds used across multiple check classes.
 * Centralizes configuration values to enable easy tuning and consistency.
 */
public class CheckConstants {

    private CheckConstants() {
        // Constants class - prevent instantiation
    }

    // Complexity and size thresholds
    public static final int COMPLEXITY_THRESHOLD = 15;
    public static final int MAX_NESTING_LEVEL = 3;
    public static final int MAX_FLOW_COMPONENTS = 15;
    public static final int GOD_FLOW_THRESHOLD = 20;
    public static final int MAX_BOOLEAN_OPERATORS = 3;
    public static final int MAX_VARIABLES = 10;
    public static final int MAX_FLOW_REF_CALLS = 5;
    public static final int MAX_CHOICE_BRANCHES = 7;
    public static final int MAX_PARAMETER_COUNT = 5;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_LOGGER_COUNT = 3;

    // Performance thresholds
    public static final int LARGE_DATASET_THRESHOLD = 1000;
    public static final int BATCH_SIZE_THRESHOLD = 100;

    // HTTP status codes
    public static final Set<Integer> SAFE_HTTP_STATUS_CODES = new HashSet<>(Arrays.asList(
            // 1xx Informational
            100,
            // 2xx Success
            200, 201, 202, 204,
            // 3xx Redirection
            301, 302, 303, 304,
            // 4xx Client errors
            400, 401, 403, 404,
            // 5xx Server errors
            500, 502, 503, 504
    ));

    // Common safe numeric values
    public static final Set<Integer> SAFE_MAGIC_NUMBERS = new HashSet<>(Arrays.asList(
            -1, 0, 1, 2, 10, 100, 1000, 10000, 100000,
            // Add HTTP status codes
            200, 201, 204, 400, 401, 403, 404, 500
    ));

    // Security patterns
    public static final String[] WEAK_CRYPTO_ALGORITHMS = {
            "DES", "3DES", "RC2", "RC4", "MD2", "MD4", "MD5", "SHA1"
    };

    public static final String[] SECURE_CRYPTO_ALGORITHMS = {
            "AES", "SHA256", "SHA-256", "SHA384", "SHA-384", "SHA512", "SHA-512"
    };

    public static final String[] CLEARTEXT_PROTOCOLS = {
            "http://", "ftp://", "telnet://", "smtp://", "ldap://"
    };

    public static final String[] SECURE_PROTOCOLS = {
            "https://", "ftps://", "ssh://", "smtps://", "ldaps://"
    };

    public static final String[] SENSITIVE_DATA_KEYWORDS = {
            "password", "passwd", "pwd", "secret", "token", "apikey", "api_key",
            "access_token", "auth", "credential", "private_key", "ssn", "social_security"
    };

    public static final String[] SQL_INJECTION_PATTERNS = {
            "select", "insert", "update", "delete", "drop", "create", "alter",
            "exec", "execute", "union", "where", "from"
    };

    public static final String[] COMMAND_INJECTION_INDICATORS = {
            "Runtime.getRuntime", "ProcessBuilder", "exec", "system", "cmd.exe", "/bin/sh"
    };

    public static final String[] PATH_TRAVERSAL_PATTERNS = {
            "../", "..\\", "..", "%2e%2e", "%252e", "..%2f", "..%5c"
    };

    // Naming conventions
    public static final String[] VAGUE_NAMES = {
            "process", "handle", "do", "main", "execute", "run", "test",
            "flow1", "flow2", "flow3", "temp", "sample", "example", "foo", "bar"
    };

    public static final String[] COMMON_ABBREVIATIONS = {
            "msg", "req", "res", "resp", "cfg", "config", "src", "dst",
            "num", "str", "obj", "arr", "val", "var"
    };

    // Database operation types
    public static final String[] DATABASE_OPERATIONS = {
            "db:select", "db:insert", "db:update", "db:delete",
            "db:bulk-insert", "db:stored-procedure"
    };

    // Resource types that require proper closing
    public static final String[] JAVA_CLOSEABLE_RESOURCES = {
            "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
            "BufferedReader", "BufferedWriter", "Connection", "Statement",
            "PreparedStatement", "ResultSet", "Socket", "ServerSocket",
            "InputStream", "OutputStream", "Reader", "Writer"
    };

    /**
     * Check if a number is considered a safe magic number.
     *
     * @param value the number to check
     * @return true if the number is in the safe list
     */
    public static boolean isSafeMagicNumber(int value) {
        return SAFE_MAGIC_NUMBERS.contains(value);
    }

    /**
     * Check if an HTTP status code is valid/common.
     *
     * @param statusCode the HTTP status code
     * @return true if the status code is in the common list
     */
    public static boolean isCommonHttpStatusCode(int statusCode) {
        return SAFE_HTTP_STATUS_CODES.contains(statusCode);
    }

    /**
     * Check if an algorithm name is considered weak.
     *
     * @param algorithm the algorithm name
     * @return true if the algorithm is weak
     */
    public static boolean isWeakCryptoAlgorithm(String algorithm) {
        if (algorithm == null) {
            return false;
        }
        String upperAlgo = algorithm.toUpperCase();
        for (String weak : WEAK_CRYPTO_ALGORITHMS) {
            if (upperAlgo.contains(weak)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a protocol is cleartext (insecure).
     *
     * @param protocol the protocol string
     * @return true if the protocol is cleartext
     */
    public static boolean isCleartextProtocol(String protocol) {
        if (protocol == null) {
            return false;
        }
        String lowerProtocol = protocol.toLowerCase();
        for (String cleartext : CLEARTEXT_PROTOCOLS) {
            if (lowerProtocol.contains(cleartext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a keyword indicates sensitive data.
     *
     * @param keyword the keyword to check
     * @return true if the keyword is sensitive
     */
    public static boolean isSensitiveKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }
        String lowerKeyword = keyword.toLowerCase();
        for (String sensitive : SENSITIVE_DATA_KEYWORDS) {
            if (lowerKeyword.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }
}
