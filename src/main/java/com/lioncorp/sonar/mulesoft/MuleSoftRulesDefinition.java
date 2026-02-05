package com.lioncorp.sonar.mulesoft;

import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.server.rule.RulesDefinition;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the rules repository for MuleSoft analysis.
 */
public class MuleSoftRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "mulesoft";
  public static final String REPOSITORY_NAME = "MuleSoft Analyzer";

  // Common tag constants
  private static final String TAG_SECURITY = "security";
  private static final String TAG_OWASP_A1 = "owasp-a1";
  private static final String TAG_OWASP_A2 = "owasp-a2";
  private static final String TAG_INJECTION = "injection";
  private static final String TAG_VULNERABILITY = "vulnerability";
  private static final String TAG_AUTHENTICATION = "authentication";
  private static final String TAG_ENCRYPTION = "encryption";
  private static final String TAG_VALIDATION = "validation";
  private static final String TAG_LOGGING = "logging";
  private static final String TAG_DATABASE = "database";
  private static final String TAG_CONFIGURATION = "configuration";
  private static final String TAG_TIMEOUT = "timeout";
  private static final String TAG_MAINTAINABILITY = "maintainability";
  private static final String TAG_COMPLEXITY = "complexity";
  private static final String TAG_ARCHITECTURE = "architecture";
  private static final String TAG_ERROR_HANDLING = "error-handling";
  private static final String TAG_CONSISTENCY = "consistency";
  private static final String TAG_CONVENTION = "convention";
  private static final String TAG_DATAWEAVE = "dataweave";
  private static final String TAG_READABILITY = "readability";
  private static final String TAG_NAMING = "naming";
  private static final String TAG_DOCUMENTATION = "documentation";
  private static final String TAG_PERFORMANCE = "performance";
  private static final String TAG_OPTIMIZATION = "optimization";
  private static final String TAG_RESILIENCE = "resilience";
  private static final String TAG_JAVA = "java";

  /**
   * Represents metadata for a single rule.
   */
  private record RuleMetadata(
      String key,
      String name,
      String description,
      SoftwareQuality quality,
      Severity severity,
      String... tags) {

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RuleMetadata that = (RuleMetadata) o;
      return key.equals(that.key) &&
          name.equals(that.name) &&
          description.equals(that.description) &&
          quality == that.quality &&
          severity == that.severity &&
          Arrays.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
      int result = key.hashCode();
      result = 31 * result + name.hashCode();
      result = 31 * result + description.hashCode();
      result = 31 * result + quality.hashCode();
      result = 31 * result + severity.hashCode();
      result = 31 * result + Arrays.hashCode(tags);
      return result;
    }

    @Override
    public String toString() {
      return "RuleMetadata{" +
          "key='" + key + '\'' +
          ", name='" + name + '\'' +
          ", description='" + description + '\'' +
          ", quality=" + quality +
          ", severity=" + severity +
          ", tags=" + Arrays.toString(tags) +
          '}';
    }
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
        .createRepository(REPOSITORY_KEY, MuleSoftLanguage.KEY)
        .setName(REPOSITORY_NAME);

    RULES.forEach(rule -> defineRule(repository, rule));

    repository.done();
  }

  private void defineRule(NewRepository repository, RuleMetadata rule) {
    repository.createRule(rule.key)
        .setName(rule.name)
        .setHtmlDescription(rule.description)
        .addDefaultImpact(rule.quality, rule.severity)
        .setTags(rule.tags);
  }

  private static final List<RuleMetadata> RULES = List.of(
      // Security Rules (MS001-MS030) - Critical vulnerabilities and security hotspots
      new RuleMetadata("MS001", "Credentials should not be hardcoded", "Hardcoded credentials in configuration files pose serious security risks. Use property placeholders (${property}) or secure vaults instead.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe", TAG_OWASP_A2),
      new RuleMetadata("MS002", "HTTP endpoints should use HTTPS", "HTTP endpoints transmit data in cleartext. Use HTTPS with TLS/SSL to encrypt data in transit.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, TAG_OWASP_A2, "transport"),
      new RuleMetadata("MS003", "SQL queries should not use string concatenation", "SQL queries built with string concatenation are vulnerable to SQL injection. Use parameterized queries.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-89", TAG_OWASP_A1, TAG_INJECTION),
      new RuleMetadata("MS004", "XML parsers should be protected against XXE", "XML External Entity (XXE) attacks can read sensitive files or perform SSRF. Disable external entity processing in XML parsers.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-611", "owasp-a4", "xxe", TAG_VULNERABILITY),
      new RuleMetadata("MS005", "OS commands should not be constructed from user input", "Command injection vulnerabilities allow attackers to execute arbitrary OS commands. Avoid executing OS commands or validate input strictly.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-78", TAG_OWASP_A1, TAG_INJECTION, TAG_VULNERABILITY),
      new RuleMetadata("MS006", "File paths should not be constructed from user input", "Path traversal vulnerabilities allow reading arbitrary files using sequences like '../'. Validate and sanitize file paths.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-22", TAG_OWASP_A1, "path-traversal", TAG_VULNERABILITY),
      new RuleMetadata("MS007", "Deserialization should be restricted to safe classes", "Insecure deserialization can lead to remote code execution. Use safe deserialization libraries or whitelist allowed classes.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-502", "owasp-a8", "deserialization", TAG_VULNERABILITY),
      new RuleMetadata("MS008", "Cryptographic algorithms should be strong", "Weak algorithms like DES, MD5, SHA1 are cryptographically broken. Use AES-256, SHA-256 or stronger algorithms.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-327", TAG_OWASP_A2, "cryptography"),
      new RuleMetadata("MS009", "API endpoints should validate authentication headers", "Missing authentication checks allow unauthorized access. Verify Authorization headers or OAuth tokens on all protected endpoints.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, TAG_AUTHENTICATION, TAG_OWASP_A2),
      new RuleMetadata("MS010", "Random values should use secure generators", "Java's Random class is predictable and unsuitable for security. Use SecureRandom for tokens, keys, and security-critical randomness.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "cwe-330", "randomness"),
      new RuleMetadata("MS011", "Redirects should validate target URLs", "Unvalidated redirects enable phishing attacks. Validate redirect URLs against a whitelist of allowed destinations.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "cwe-open-redirect", TAG_OWASP_A1),
      new RuleMetadata("MS012", "Network protocols should use encryption", "Cleartext protocols like HTTP, FTP, SMTP expose data to eavesdropping. Use HTTPS, FTPS, SMTPS instead.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cleartext", TAG_ENCRYPTION, TAG_OWASP_A2),
      new RuleMetadata("MS013", "LDAP queries should not be constructed from user input", "LDAP injection allows attackers to manipulate directory queries. Use parameterized LDAP queries and escape special characters.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-90", "ldap-injection", TAG_INJECTION),
      new RuleMetadata("MS014", "API responses should not expose excessive data", "Returning entire database objects or internal data structures leaks sensitive information. Filter responses to include only necessary fields.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "owasp-api3", "data-exposure"),
      new RuleMetadata("MS015", "CORS configuration should be restrictive", "Wildcard CORS origins (*) allow any website to access your API. Specify explicit allowed origins.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "cors", "owasp-a5"),
      new RuleMetadata("MS016", "Dangerous Java classes should be reviewed", "Classes like Runtime and ProcessBuilder introduce security risks. Review usage and ensure proper input validation.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "cwe", "owasp"),
      new RuleMetadata("MS017", "User input should be validated", "Missing input validation leads to injection attacks and data corruption. Validate type, format, length, and range of all user inputs.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, TAG_VALIDATION, TAG_OWASP_A1),
      new RuleMetadata("MS018", "Sensitive data should not be logged", "Logging passwords, tokens, credit cards, or PII violates privacy. Sanitize logs and avoid logging sensitive fields.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-532", TAG_LOGGING, "privacy"),
      new RuleMetadata("MS019", "OAuth tokens and JWTs should be validated", "Accepting unvalidated JWTs bypasses authentication. Verify signature, expiration, issuer, and audience claims.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "oauth", "jwt", TAG_AUTHENTICATION),
      new RuleMetadata("MS020", "Database connections should use encryption", "Unencrypted database connections expose credentials and data. Enable SSL/TLS for database connections.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, TAG_DATABASE, TAG_ENCRYPTION),
      new RuleMetadata("MS021", "TLS version should be 1.2 or higher", "TLS 1.0 and 1.1 have known vulnerabilities. Configure HTTPS connectors to use TLS 1.2 or 1.3.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "tls", TAG_ENCRYPTION, "protocol"),
      new RuleMetadata("MS022", "Admin endpoints should not be publicly accessible", "Exposed admin panels enable unauthorized access. Restrict admin endpoints to internal networks or require strong authentication.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "access-control", "owasp-a5"),
      new RuleMetadata("MS023", "File uploads should validate type and size", "Unvalidated file uploads allow malware uploads or DoS via large files. Validate MIME types, extensions, and enforce size limits.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "file-upload", TAG_VALIDATION, "owasp-a4"),
      new RuleMetadata("MS024", "IP addresses should not be hardcoded", "Hardcoded IP addresses create deployment dependencies and security risks. Use DNS names or configuration properties.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "hardcoded", TAG_CONFIGURATION),
      new RuleMetadata("MS025", "Content-Type headers should be validated", "Missing Content-Type validation allows attackers to send unexpected data formats. Validate request Content-Type headers.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, TAG_VALIDATION, "headers"),
      new RuleMetadata("MS026", "State-changing operations should have CSRF protection", "Missing CSRF tokens allow attackers to perform unauthorized actions. Implement CSRF tokens for POST/PUT/DELETE endpoints.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-352", "csrf", "owasp-a8"),
      new RuleMetadata("MS027", "Session tokens should be regenerated after authentication", "Session fixation allows attackers to hijack sessions. Regenerate session IDs after successful login.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "session-fixation", TAG_AUTHENTICATION),
      new RuleMetadata("MS028", "Sessions should have appropriate timeouts", "Long or infinite session timeouts increase exposure window. Configure reasonable session expiration times.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_SECURITY, "session-management", TAG_TIMEOUT),
      new RuleMetadata("MS029", "Session cookies should be secure and HttpOnly", "Insecure session cookies are vulnerable to XSS and MITM attacks. Set Secure, HttpOnly, and SameSite flags.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cookies", "session-management"),
      new RuleMetadata("MS030", "XML processing should limit entity expansion", "XML bomb attacks (billion laughs) cause memory exhaustion. Configure entity expansion limits in XML parsers.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_SECURITY, "cwe-776", "xml-bomb", "dos"),

      // Structure Rules (MS031-MS058) - Code quality and organization
      new RuleMetadata("MS031", "Empty flows should be removed", "Flows without components should be removed or properly implemented.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "bad-practice", "unused"),
      new RuleMetadata("MS032", "Large flows should be broken down", "Flows with too many components (>15) are hard to maintain. Split into smaller, focused flows.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "brain-overload", TAG_MAINTAINABILITY),
      new RuleMetadata("MS033", "Duplicated flow logic should be extracted", "Similar logic across multiple flows indicates code duplication. Extract common logic into reusable sub-flows.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "duplication", "dry-principle"),
      new RuleMetadata("MS034", "Flow nesting should not exceed 3 levels", "Deep nesting of choice/foreach/scatter-gather reduces readability. Refactor to flatten structure.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_COMPLEXITY, "nesting"),
      new RuleMetadata("MS035", "Unused sub-flows should be removed", "Sub-flows that are never referenced create maintenance burden. Remove unused sub-flows.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, "unused", "dead-code"),
      new RuleMetadata("MS036", "Circular flow references should be avoided", "Circular dependencies between flows create maintenance issues and potential infinite loops. Refactor to eliminate cycles.", SoftwareQuality.RELIABILITY, Severity.HIGH, "circular-dependency", TAG_ARCHITECTURE),
      new RuleMetadata("MS037", "Flows should have error handlers", "Missing error handlers make flows fragile. Add error-handler elements to handle failures gracefully.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_ERROR_HANDLING, "robustness"),
      new RuleMetadata("MS038", "Flows should not have excessive flow-ref calls", "Too many flow-ref calls (>5) indicate poor cohesion. Consider consolidating or restructuring flows.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_COMPLEXITY, "cohesion"),
      new RuleMetadata("MS039", "Flow structure should be consistent", "Inconsistent flow structures make codebases harder to navigate. Establish and follow structural patterns.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_CONSISTENCY, TAG_CONVENTION),
      new RuleMetadata("MS040", "Flows should have single responsibility", "Flows mixing multiple concerns (e.g., DB operations and HTTP calls) violate single responsibility. Split into focused flows.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "solid", "single-responsibility"),
      new RuleMetadata("MS041", "Cognitive complexity should be low", "High cognitive complexity makes code hard to understand. Simplify logic by extracting complex conditions and nested structures.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_COMPLEXITY, "cognitive-load"),
      new RuleMetadata("MS042", "Choice components should not have excessive branches", "Choice routers with more than 7 branches become hard to maintain. Consider lookup tables or strategy patterns.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_COMPLEXITY, "branching"),
      new RuleMetadata("MS043", "DataWeave expressions should not be deeply nested", "Deeply nested DataWeave expressions reduce readability. Break complex transformations into multiple steps.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_DATAWEAVE, TAG_COMPLEXITY),
      new RuleMetadata("MS044", "Flows should not have long parameter lists", "Flows accepting many parameters (>5) are hard to use. Group related parameters into objects or reduce dependencies.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, "parameters", "api-design"),
      new RuleMetadata("MS045", "Magic numbers should be replaced with constants", "Hardcoded numbers like timeouts, limits, or thresholds lack context. Define named properties or variables.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, "magic-numbers", TAG_READABILITY),
      new RuleMetadata("MS046", "Boolean expressions should not be overly complex", "Complex boolean expressions with multiple AND/OR operators are error-prone. Extract to named variables or simplify.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "boolean-logic", TAG_COMPLEXITY),
      new RuleMetadata("MS047", "God flows should be refactored", "Flows doing everything (>20 components, multiple responsibilities) are anti-patterns. Decompose into smaller, focused flows.", SoftwareQuality.MAINTAINABILITY, Severity.HIGH, "god-object", TAG_ARCHITECTURE),
      new RuleMetadata("MS048", "Flows should not set excessive variables", "Setting too many variables (>7) increases cognitive load. Reduce variable count or use structured data.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "variables", TAG_COMPLEXITY),
      new RuleMetadata("MS049", "Method names should not be excessively long", "Flow or sub-flow names longer than 50 characters are hard to read. Use concise, descriptive names.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, TAG_READABILITY),
      new RuleMetadata("MS050", "Flows should not have tight coupling", "Flows with excessive dependencies on other flows create inappropriate intimacy. Reduce coupling through interfaces or events.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "coupling", TAG_ARCHITECTURE),
      new RuleMetadata("MS051", "Naming conventions should be consistent", "Inconsistent naming patterns across flows reduce code consistency. Adopt and enforce naming standards.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, TAG_CONSISTENCY),
      new RuleMetadata("MS052", "Required configurations should not be missing", "Flows missing essential configurations (like timeouts, retries) may fail unpredictably. Define required settings.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_CONFIGURATION, "reliability"),
      new RuleMetadata("MS053", "Configuration should not be duplicated", "Duplicated configuration blocks increase maintenance burden. Use global configurations or property files.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, "duplication", TAG_CONFIGURATION),
      new RuleMetadata("MS054", "Environment-specific values should be externalized", "Hardcoded environment values (URLs, hosts, ports) prevent portability. Use property placeholders for environment-specific values.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_CONFIGURATION, "portability"),
      new RuleMetadata("MS055", "Timeouts should be consistent across flows", "Inconsistent timeout values across similar operations create unpredictable behavior. Standardize timeout configurations.", SoftwareQuality.RELIABILITY, Severity.LOW, TAG_TIMEOUT, TAG_CONSISTENCY),
      new RuleMetadata("MS056", "Default configurations should be overridden", "Using default configurations without review may not meet requirements. Explicitly configure connectors and components.", SoftwareQuality.RELIABILITY, Severity.LOW, TAG_CONFIGURATION, "defaults"),
      new RuleMetadata("MS057", "Components should have descriptions", "Missing doc:description elements make components hard to understand. Document the purpose of key components.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_DOCUMENTATION, TAG_MAINTAINABILITY),
      new RuleMetadata("MS058", "Deprecated configurations should be updated", "Obsolete or deprecated configuration properties may break in future versions. Update to current recommended configurations.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "deprecation", "obsolete"),

      // Naming Rules (MS059-MS071) - Naming conventions and documentation
      new RuleMetadata("MS059", "All flows must have descriptive names", "Flows without names are impossible to debug and maintain. Every flow and sub-flow must have a meaningful name attribute.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_CONVENTION, TAG_READABILITY),
      new RuleMetadata("MS060", "Flow names should not be vague", "Generic names like 'process', 'handle', 'flow1' or 'test-flow' lack meaning. Use descriptive names that explain the business purpose.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, "clarity"),
      new RuleMetadata("MS061", "Flow naming should use consistent casing", "Inconsistent use of kebab-case, snake_case, or camelCase reduces readability. Choose one style and apply it consistently across the project.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, TAG_CONVENTION),
      new RuleMetadata("MS062", "Names should not contain excessive abbreviations", "Excessive abbreviations like 'tmp', 'msg', 'btn' reduce code readability. Use full words for clarity (e.g., 'temporary', 'message', 'button').", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, TAG_READABILITY),
      new RuleMetadata("MS063", "Logger components must have meaningful messages", "Loggers without messages or with vague messages like 'log' or 'here' provide no debugging value. Include descriptive messages.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_LOGGING, "debugging"),
      new RuleMetadata("MS064", "Variable names should be informative", "Generic variable names like 'temp', 'data', 'var', 'x' don't convey purpose. Use descriptive names that indicate what the variable contains.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, "clarity"),
      new RuleMetadata("MS065", "Flows should have doc:description elements", "Missing or empty doc:description elements make it hard to understand flow purpose. Add documentation explaining what each flow does.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_DOCUMENTATION, TAG_MAINTAINABILITY),
      new RuleMetadata("MS066", "TODO/FIXME comments should be resolved", "TODO, FIXME, HACK, XXX markers indicate incomplete work. Address them or convert to proper issue tickets.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, "todo", "technical-debt"),
      new RuleMetadata("MS067", "Outdated comments should be updated or removed", "Comments containing 'deprecated', 'old version', 'legacy', or old dates may be stale. Review and update documentation.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_DOCUMENTATION, "obsolete"),
      new RuleMetadata("MS068", "API endpoints should have documentation", "HTTP listeners (API endpoints) without doc:description lack important information about purpose, parameters, and responses.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, "api", TAG_DOCUMENTATION),
      new RuleMetadata("MS069", "Flow names should follow project conventions", "Inconsistent naming patterns (verb-noun vs noun-verb) reduce code consistency. Follow the project's established naming pattern.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, TAG_CONVENTION),
      new RuleMetadata("MS070", "Names should not be excessively long", "Names longer than 50 characters become hard to read and work with. Keep names concise while maintaining clarity.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_NAMING, TAG_READABILITY),
      new RuleMetadata("MS071", "DataWeave transformations should be commented", "Complex DataWeave transformations without comments are hard to understand. Add explanatory comments for maintainability.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_DATAWEAVE, TAG_DOCUMENTATION),

      // Performance Rules (MS072-MS084) - Performance and efficiency
      new RuleMetadata("MS072", "Use async processing for independent operations", "Synchronous processing blocks execution. Use async scopes or scatter-gather for independent operations to improve throughput.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, "async", "throughput"),
      new RuleMetadata("MS073", "Frequently accessed data should be cached", "Repeated calls to slow resources waste time. Implement caching for reference data and frequently accessed resources.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, "caching", TAG_OPTIMIZATION),
      new RuleMetadata("MS074", "DataWeave transformations should be optimized", "Inefficient DataWeave scripts slow processing. Avoid unnecessary iterations, use streaming, and optimize selectors.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, TAG_DATAWEAVE, TAG_OPTIMIZATION),
      new RuleMetadata("MS075", "Logging should not be excessive", "Too many log statements (especially at DEBUG level) degrade performance. Log only meaningful events at appropriate levels.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_PERFORMANCE, TAG_LOGGING),
      new RuleMetadata("MS076", "Large datasets should use batch processing", "Processing large datasets with foreach is inefficient. Use batch:job for bulk operations to improve performance and memory usage.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, "batch", "scalability"),
      new RuleMetadata("MS077", "Scatter-gather should have timeout configuration", "Unbounded scatter-gather operations can hang indefinitely. Configure appropriate timeout values.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_PERFORMANCE, TAG_TIMEOUT, "scatter-gather"),
      new RuleMetadata("MS078", "Database operations should not be in loops", "Database calls in foreach cause N+1 query problems. Batch queries or use bulk operations to reduce round-trips.", SoftwareQuality.MAINTAINABILITY, Severity.HIGH, TAG_PERFORMANCE, TAG_DATABASE, "n-plus-one"),
      new RuleMetadata("MS079", "Large payloads should be streamed", "Loading large payloads entirely into memory causes OutOfMemoryErrors. Use streaming APIs for files and large datasets.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_PERFORMANCE, "memory", "streaming"),
      new RuleMetadata("MS080", "Avoid multiple transformations of same payload", "Repeatedly transforming the same payload wastes CPU. Combine transformations into single DataWeave script.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, "transformation", TAG_OPTIMIZATION),
      new RuleMetadata("MS081", "Database connectors should use connection pooling", "Creating connections per request is slow. Configure connection pools to reuse database connections efficiently.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, TAG_DATABASE, "pooling"),
      new RuleMetadata("MS082", "API calls should be asynchronous when possible", "Synchronous API calls block threads. Use async HTTP requests or publish-subscribe patterns for non-critical operations.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, "async", "api"),
      new RuleMetadata("MS083", "XML parsing should be efficient", "DOM parsing loads entire XML into memory. Use SAX or StAX for large XML documents to reduce memory footprint.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, "xml", "parsing"),
      new RuleMetadata("MS084", "DataWeave scripts should be reviewed for optimization", "Unoptimized DataWeave with nested loops or inefficient operations degrades performance. Profile and optimize complex scripts.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_PERFORMANCE, TAG_DATAWEAVE, "profiling"),

      // Error Handling Rules (MS085-MS096) - Exception handling and resilience
      new RuleMetadata("MS085", "Applications should have global error handlers", "Missing global error handlers leave uncaught exceptions unhandled. Define a global error-handler element for consistent error handling.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_ERROR_HANDLING, "global", "robustness"),
      new RuleMetadata("MS086", "Error handlers should not be empty", "Empty on-error-continue or on-error-propagate blocks provide no error handling. Log errors and take appropriate action.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_ERROR_HANDLING, "empty"),
      new RuleMetadata("MS087", "Avoid catching generic error types", "Catching 'ANY' or generic exceptions masks specific failures. Handle specific error types for precise error management.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_ERROR_HANDLING, "specificity"),
      new RuleMetadata("MS088", "Errors should not be silently swallowed", "Error handlers that don't log or propagate hide problems. Always log errors and either handle or propagate them.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_ERROR_HANDLING, "silent-failure"),
      new RuleMetadata("MS089", "External calls should have retry strategies", "Transient failures in external systems cause unnecessary errors. Configure retry strategies with exponential backoff.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_ERROR_HANDLING, "retry", TAG_RESILIENCE),
      new RuleMetadata("MS090", "Retry counts should be reasonable", "Excessive retry attempts (>5) can cause cascading failures. Configure appropriate retry limits to avoid overwhelming systems.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, "retry", TAG_RESILIENCE),
      new RuleMetadata("MS091", "Circuit breakers should protect external dependencies", "Repeatedly calling failing services degrades performance. Implement circuit breaker pattern for external dependencies.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, "circuit-breaker", TAG_RESILIENCE, "fault-tolerance"),
      new RuleMetadata("MS092", "Error responses should be consistent", "Inconsistent error response formats confuse API consumers. Standardize error response structure across all endpoints.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_ERROR_HANDLING, "api", TAG_CONSISTENCY),
      new RuleMetadata("MS093", "Stack traces should not be exposed to clients", "Exposed stack traces reveal internal implementation details and aid attackers. Return generic error messages to clients.", SoftwareQuality.SECURITY, Severity.HIGH, TAG_ERROR_HANDLING, "information-disclosure", TAG_SECURITY),
      new RuleMetadata("MS094", "External operations should have timeouts", "Operations without timeouts can hang indefinitely. Configure appropriate timeout values for all external calls.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_TIMEOUT, TAG_RESILIENCE),
      new RuleMetadata("MS095", "Avoid rethrowing generic exceptions", "Rethrowing generic exceptions loses context. Wrap in domain-specific exceptions with meaningful messages.", SoftwareQuality.RELIABILITY, Severity.LOW, TAG_ERROR_HANDLING, "exceptions"),
      new RuleMetadata("MS096", "Custom errors should be handled", "Custom error types raised but not caught cause unexpected failures. Ensure all raise-error calls have corresponding handlers.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_ERROR_HANDLING, "custom-errors"),

      // Java Integration Rules (MS097-MS110) - Java code quality
      new RuleMetadata("MS097", "Java invocations should be type-safe", "Untyped Java invocations (using Object or no type) are error-prone. Specify explicit types for Java method calls.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_JAVA, "type-safety"),
      new RuleMetadata("MS098", "Java code should check for null values", "Missing null checks lead to NullPointerExceptions. Validate inputs and return values from Java components.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_JAVA, "null-safety", "bug"),
      new RuleMetadata("MS099", "Type casts should be checked", "Unchecked casts cause ClassCastException at runtime. Use instanceof or verify types before casting.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_JAVA, "type-safety", "casting"),
      new RuleMetadata("MS100", "Deprecated Java methods should be replaced", "Deprecated methods may be removed in future versions. Update to current recommended Java APIs.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_JAVA, "deprecation", "technical-debt"),
      new RuleMetadata("MS101", "Java exceptions should be handled", "Unhandled checked exceptions in Java components cause flow failures. Use try-catch or declare throws.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_JAVA, "exception-handling"),
      new RuleMetadata("MS102", "Java components should validate inputs", "Missing input validation in Java classes creates vulnerabilities. Validate parameters before processing.", SoftwareQuality.SECURITY, Severity.MEDIUM, TAG_JAVA, TAG_VALIDATION, TAG_SECURITY),
      new RuleMetadata("MS103", "Java collections should be efficient", "Inefficient collection usage (e.g., ArrayList for frequent inserts) degrades performance. Choose appropriate collection types.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_JAVA, "collections", TAG_PERFORMANCE),
      new RuleMetadata("MS104", "Resources should be properly closed", "Unclosed streams/connections cause resource leaks. Use try-with-resources or explicit finally blocks.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_JAVA, "resource-leak", "bug"),
      new RuleMetadata("MS105", "Java code should be thread-safe", "Shared mutable state without synchronization causes race conditions. Use thread-safe collections or synchronization.", SoftwareQuality.RELIABILITY, Severity.HIGH, TAG_JAVA, "thread-safety", "concurrency"),
      new RuleMetadata("MS106", "Blocking calls should not be in Java components", "Blocking I/O or Thread.sleep in Java components reduces throughput. Use async APIs or move to separate threads.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_JAVA, "blocking", TAG_PERFORMANCE),
      new RuleMetadata("MS107", "Java code complexity should be manageable", "Complex Java methods with high cyclomatic complexity are hard to test. Refactor into smaller methods.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_JAVA, TAG_COMPLEXITY, TAG_MAINTAINABILITY),
      new RuleMetadata("MS108", "Serializable classes should implement properly", "Missing serialVersionUID or improper Serializable implementation causes deserialization issues. Define explicit serialVersionUID.", SoftwareQuality.RELIABILITY, Severity.MEDIUM, TAG_JAVA, "serialization"),
      new RuleMetadata("MS109", "Hardcoded values should be extracted to configuration", "Hardcoded values in Java code reduce flexibility. Move constants to property files or configuration.", SoftwareQuality.MAINTAINABILITY, Severity.LOW, TAG_JAVA, "hardcoded", TAG_CONFIGURATION),
      new RuleMetadata("MS110", "Regular expressions should be optimized", "Inefficient regex patterns with excessive backtracking cause performance issues. Optimize regex or compile patterns once.", SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM, TAG_JAVA, "regex", TAG_PERFORMANCE)
  );
}
