package com.lioncorp.sonar.mulesoft.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for security-related pattern detection.
 */
public class SecurityPatterns {

  private SecurityPatterns() {
    // Utility class
  }

  // Dangerous Java classes that pose security risks
  private static final Set<String> DANGEROUS_CLASSES = new HashSet<>(Arrays.asList(
      "java.lang.Runtime",
      "java.lang.ProcessBuilder",
      "java.io.File",
      "java.io.FileReader",
      "java.io.FileWriter",
      "java.io.FileInputStream",
      "java.io.FileOutputStream",
      "java.net.Socket",
      "java.net.ServerSocket",
      "java.net.URL",
      "java.net.URLClassLoader",
      "javax.script.ScriptEngine",
      "javax.script.ScriptEngineManager",
      "groovy.lang.GroovyShell",
      "groovy.lang.GroovyClassLoader",
      "java.lang.Class", // Dynamic class loading
      "java.lang.reflect.Method",
      "sun.misc.Unsafe"
  ));

  // Dangerous methods that can execute arbitrary code
  private static final Set<String> DANGEROUS_METHODS = new HashSet<>(Arrays.asList(
      "exec",
      "start",
      "getRuntime",
      "eval",
      "invoke",
      "loadClass",
      "forName",
      "newInstance"
  ));

  // File I/O related classes
  private static final Set<String> FILE_IO_CLASSES = new HashSet<>(Arrays.asList(
      "java.io.File",
      "java.io.FileReader",
      "java.io.FileWriter",
      "java.io.FileInputStream",
      "java.io.FileOutputStream",
      "java.nio.file.Files",
      "java.nio.file.Paths",
      "java.nio.file.Path"
  ));

  // Network-related classes
  private static final Set<String> NETWORK_CLASSES = new HashSet<>(Arrays.asList(
      "java.net.Socket",
      "java.net.ServerSocket",
      "java.net.URL",
      "java.net.URLConnection",
      "java.net.HttpURLConnection"
  ));

  // Script engine classes
  private static final Set<String> SCRIPT_ENGINE_CLASSES = new HashSet<>(Arrays.asList(
      "javax.script.ScriptEngine",
      "javax.script.ScriptEngineManager",
      "groovy.lang.GroovyShell",
      "groovy.lang.GroovyClassLoader",
      "org.mozilla.javascript.Context"
  ));

  // Reflection-related classes
  private static final Set<String> REFLECTION_CLASSES = new HashSet<>(Arrays.asList(
      "java.lang.Class",
      "java.lang.reflect.Method",
      "java.lang.reflect.Field",
      "java.lang.reflect.Constructor"
  ));

  // Weak cryptographic algorithms
  private static final Set<String> WEAK_CRYPTO_ALGORITHMS = new HashSet<>(Arrays.asList(
      "DES",
      "3DES",
      "TripleDES",
      "MD5",
      "SHA1",
      "SHA-1",
      "RC2",
      "RC4",
      "ARCFOUR"
  ));

  // Strong cryptographic algorithms (safe)
  private static final Set<String> STRONG_CRYPTO_ALGORITHMS = new HashSet<>(Arrays.asList(
      "AES",
      "AES-256",
      "AES-128",
      "RSA",
      "SHA-256",
      "SHA-384",
      "SHA-512",
      "SHA256",
      "SHA384",
      "SHA512"
  ));

  // Cleartext protocols
  private static final Set<String> CLEARTEXT_PROTOCOLS = new HashSet<>(Arrays.asList(
      "HTTP",
      "FTP",
      "TELNET",
      "SMTP",
      "LDAP",
      "POP3",
      "IMAP"
  ));

  // Secure protocols
  private static final Set<String> SECURE_PROTOCOLS = new HashSet<>(Arrays.asList(
      "HTTPS",
      "FTPS",
      "SFTP",
      "SSH",
      "TLS",
      "SSL",
      "SMTPS",
      "LDAPS",
      "POP3S",
      "IMAPS"
  ));

  /**
   * Check if a Java class is considered dangerous from a security perspective.
   */
  public static boolean isDangerousClass(String className) {
    if (className == null) {
      return false;
    }
    return DANGEROUS_CLASSES.contains(className) ||
           className.startsWith("sun.misc.") ||
           className.startsWith("com.sun.");
  }

  /**
   * Check if a method name is potentially dangerous.
   */
  public static boolean isDangerousMethod(String methodName) {
    return methodName != null && DANGEROUS_METHODS.contains(methodName);
  }

  /**
   * Check if a class is related to file I/O operations.
   */
  public static boolean isFileIOClass(String className) {
    return className != null && FILE_IO_CLASSES.contains(className);
  }

  /**
   * Check if a class is related to network operations.
   */
  public static boolean isNetworkClass(String className) {
    return className != null && NETWORK_CLASSES.contains(className);
  }

  /**
   * Check if a class is a script engine (can execute arbitrary code).
   */
  public static boolean isScriptEngineClass(String className) {
    return className != null && SCRIPT_ENGINE_CLASSES.contains(className);
  }

  /**
   * Check if a class is reflection-related.
   */
  public static boolean isReflectionClass(String className) {
    return className != null && REFLECTION_CLASSES.contains(className);
  }

  /**
   * Check if an encryption algorithm is considered weak.
   */
  public static boolean isWeakCryptoAlgorithm(String algorithm) {
    if (algorithm == null) {
      return false;
    }
    String upper = algorithm.toUpperCase();
    for (String weak : WEAK_CRYPTO_ALGORITHMS) {
      if (upper.contains(weak)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if an encryption algorithm is considered strong.
   */
  public static boolean isStrongCryptoAlgorithm(String algorithm) {
    if (algorithm == null) {
      return false;
    }
    String upper = algorithm.toUpperCase();
    for (String strong : STRONG_CRYPTO_ALGORITHMS) {
      if (upper.contains(strong)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a protocol is cleartext (unencrypted).
   */
  public static boolean isCleartextProtocol(String protocol) {
    if (protocol == null) {
      return false;
    }
    return CLEARTEXT_PROTOCOLS.contains(protocol.toUpperCase());
  }

  /**
   * Check if a protocol is secure (encrypted).
   */
  public static boolean isSecureProtocol(String protocol) {
    if (protocol == null) {
      return false;
    }
    return SECURE_PROTOCOLS.contains(protocol.toUpperCase());
  }

  /**
   * Check if a string contains SQL injection patterns.
   */
  public static boolean hasSQLInjectionRisk(String code) {
    if (code == null) {
      return false;
    }
    String lower = code.toLowerCase();
    // Look for string concatenation with SQL keywords
    return (lower.contains("select") || lower.contains("insert") ||
            lower.contains("update") || lower.contains("delete")) &&
           (code.contains("+") || code.contains("concat") ||
            lower.contains("\"$") || lower.contains("'$"));
  }

  /**
   * Check if a string contains command injection patterns.
   */
  public static boolean hasCommandInjectionRisk(String code) {
    if (code == null) {
      return false;
    }
    // Look for Runtime.exec or ProcessBuilder with dynamic input
    return (code.contains("Runtime") && code.contains("exec")) ||
           (code.contains("ProcessBuilder") && code.contains("new")) ||
           (code.contains("bash -c") || code.contains("cmd.exe") ||
            code.contains("/bin/sh"));
  }

  /**
   * Check if a string contains path traversal patterns.
   */
  public static boolean hasPathTraversalRisk(String path) {
    if (path == null) {
      return false;
    }
    return path.contains("../") || path.contains("..\\") ||
           path.contains("%2e%2e") || path.contains("%252e");
  }

  /**
   * Check if a string contains XXE (XML External Entity) vulnerability patterns.
   */
  public static boolean hasXXERisk(String code) {
    if (code == null) {
      return false;
    }
    String lower = code.toLowerCase();
    // Look for XML parsing without disabling external entities
    return (lower.contains("documentbuilder") || lower.contains("saxparser") ||
            lower.contains("xmlreader")) &&
           !lower.contains("disallow_doctype_decl") &&
           !lower.contains("external-general-entities");
  }

  /**
   * Check if a string contains deserialization vulnerability patterns.
   */
  public static boolean hasDeserializationRisk(String code) {
    if (code == null) {
      return false;
    }
    return code.contains("ObjectInputStream") && code.contains("readObject");
  }

  /**
   * Check if code uses insecure random number generation.
   */
  public static boolean usesInsecureRandom(String code) {
    if (code == null) {
      return false;
    }
    return (code.contains("Math.random()") ||
            code.contains("new Random(")) &&
           !code.contains("SecureRandom");
  }

  /**
   * Check if a CORS configuration is insecure (allows all origins).
   */
  public static boolean hasInsecureCORS(String origin) {
    return "*".equals(origin);
  }

  /**
   * Check if a redirect URL is potentially unsafe (not validated).
   */
  public static boolean hasUnvalidatedRedirect(String code) {
    if (code == null) {
      return false;
    }
    String lower = code.toLowerCase();
    return (lower.contains("redirect") || lower.contains("forward")) &&
           (code.contains("request.getParameter") ||
            code.contains("payload") ||
            code.contains("vars."));
  }

  /**
   * Check if code exposes stack traces in responses.
   */
  public static boolean exposesStackTrace(String code) {
    if (code == null) {
      return false;
    }
    return code.contains("printStackTrace") ||
           (code.contains("exception") && code.contains("getMessage()")) ||
           code.contains("e.toString()");
  }

  /**
   * Check if error handler swallows exceptions silently.
   */
  public static boolean swallowsException(String code) {
    if (code == null) {
      return false;
    }
    String lower = code.toLowerCase();
    // Empty catch blocks or catch without logging
    return (lower.contains("catch") && lower.contains("{}")) ||
           (lower.contains("catch") && !lower.contains("log") &&
            !lower.contains("throw") && !lower.contains("raise"));
  }

  /**
   * Get a description of why a class is dangerous.
   */
  public static String getDangerousClassReason(String className) {
    if (className == null) {
      return "Unknown class";
    }

    if (isFileIOClass(className)) {
      return "File I/O operations can read/write sensitive files";
    } else if (isNetworkClass(className)) {
      return "Network operations can create unauthorized connections";
    } else if (isScriptEngineClass(className)) {
      return "Script engines can execute arbitrary code";
    } else if (isReflectionClass(className)) {
      return "Reflection can bypass security controls";
    } else if (className.contains("Runtime") || className.contains("ProcessBuilder")) {
      return "Can execute arbitrary system commands";
    }

    return "Potentially dangerous class usage";
  }
}
