#!/usr/bin/env python3
"""
Generate skeleton check classes for MuleSoft SonarQube plugin.
"""

import os

# Rule definitions: (key, className, category, description)
RULES = [
    # Security - Vulnerabilities (MS001-MS015)
    ("MS001", "HardcodedCredentialsCheck", "security", "Hardcoded credentials detected"),
    ("MS002", "InsecureHTTPEndpointCheck", "security", "HTTP endpoint without TLS/SSL"),
    ("MS003", "SQLInjectionCheck", "security", "Potential SQL injection vulnerability"),
    ("MS004", "XMLExternalEntityInjectionCheck", "security", "XXE injection vulnerability"),
    ("MS005", "CommandInjectionCheck", "security", "OS command injection vulnerability"),
    ("MS006", "PathTraversalRiskCheck", "security", "Path traversal vulnerability"),
    ("MS007", "InsecureDeserializationCheck", "security", "Insecure deserialization detected"),
    ("MS008", "WeakCryptographyCheck", "security", "Weak encryption algorithm used"),
    ("MS009", "MissingAuthenticationHeaderCheck", "security", "HTTP request without authentication"),
    ("MS010", "InsecureRandomnessCheck", "security", "Insecure random number generation"),
    ("MS011", "HardcodedIPAddressCheck", "security", "Hardcoded IP address in configuration"),
    ("MS012", "ClearTextProtocolCheck", "security", "Cleartext protocol used"),
    ("MS013", "MissingCSRFProtectionCheck", "security", "Missing CSRF protection"),
    ("MS014", "ExposedAdminEndpointCheck", "security", "Admin endpoint without protection"),
    ("MS015", "UnvalidatedRedirectCheck", "security", "Unvalidated redirect detected"),

    # Security - Hotspots (MS016-MS030)
    ("MS016", "JavaClassSecurityCheck", "security", "Dangerous Java class usage"),
    ("MS017", "MissingInputValidationCheck", "security", "Missing input validation"),
    ("MS018", "SensitiveDataLoggingCheck", "security", "Sensitive data in logs"),
    ("MS019", "MissingOAuthValidationCheck", "security", "Missing OAuth/JWT validation"),
    ("MS020", "InsecureCORSConfigurationCheck", "security", "Insecure CORS configuration"),
    ("MS021", "DatabaseConnectionWithoutEncryptionCheck", "security", "Database connection without SSL"),
    ("MS022", "MissingRateLimitingCheck", "security", "Missing rate limiting"),
    ("MS023", "ExcessiveDataExposureCheck", "security", "Excessive data exposure in API"),
    ("MS024", "MissingContentTypeValidationCheck", "security", "Missing Content-Type validation"),
    ("MS025", "UnsafeReflectionCheck", "security", "Unsafe reflection usage"),
    ("MS026", "MissingSecurityHeadersCheck", "security", "Missing security headers"),
    ("MS027", "FileUploadWithoutValidationCheck", "security", "File upload without validation"),
    ("MS028", "SessionManagementIssueCheck", "security", "Session management issue"),
    ("MS029", "LDAPInjectionRiskCheck", "security", "LDAP injection risk"),
    ("MS030", "XMLBombRiskCheck", "security", "XML bomb/billion laughs risk"),

    # Structure (MS031-MS058)
    ("MS031", "EmptyFlowCheck", "structure", "Empty flow detected"),
    ("MS032", "LargeFlowCheck", "structure", "Flow too large"),
    ("MS033", "DuplicatedFlowLogicCheck", "structure", "Duplicated flow logic"),
    ("MS034", "DeepFlowNestingCheck", "structure", "Excessive flow nesting"),
    ("MS035", "UnusedSubFlowCheck", "structure", "Unused sub-flow"),
    ("MS036", "CircularFlowReferenceCheck", "structure", "Circular flow reference"),
    ("MS037", "MissingErrorHandlerCheck", "structure", "Missing error handler"),
    ("MS038", "TooManyFlowRefsCheck", "structure", "Too many flow-refs"),
    ("MS039", "InconsistentFlowStructureCheck", "structure", "Inconsistent flow structure"),
    ("MS040", "MixedResponsibilityFlowCheck", "structure", "Mixed responsibility in flow"),
    ("MS041", "CognitiveComplexityCheck", "structure", "High cognitive complexity"),
    ("MS042", "ExcessiveChoiceBranchesCheck", "structure", "Too many choice branches"),
    ("MS043", "DeepDataWeaveExpressionCheck", "structure", "Deep DataWeave nesting"),
    ("MS044", "LongParameterListCheck", "structure", "Long parameter list"),
    ("MS045", "MagicNumberCheck", "structure", "Magic number detected"),
    ("MS046", "ComplexBooleanExpressionCheck", "structure", "Complex boolean expression"),
    ("MS047", "GodFlowCheck", "structure", "God flow anti-pattern"),
    ("MS048", "TooManyVariablesCheck", "structure", "Too many variables"),
    ("MS049", "LongMethodNameCheck", "structure", "Method name too long"),
    ("MS050", "InappropriateIntimacyCheck", "structure", "Inappropriate intimacy"),
    ("MS051", "InconsistentNamingCheck", "structure", "Inconsistent naming convention"),
    ("MS052", "MissingConfigurationCheck", "structure", "Missing configuration"),
    ("MS053", "DuplicatedConfigurationCheck", "structure", "Duplicated configuration"),
    ("MS054", "HardcodedEnvironmentValueCheck", "structure", "Hardcoded environment value"),
    ("MS055", "InconsistentTimeoutCheck", "structure", "Inconsistent timeout values"),
    ("MS056", "DefaultConfigurationUsedCheck", "structure", "Default configuration used"),
    ("MS057", "MissingDescriptionCheck", "structure", "Missing description"),
    ("MS058", "ObsoleteConfigurationCheck", "structure", "Obsolete configuration"),

    # Naming (MS059-MS071)
    ("MS059", "MissingFlowNameCheck", "naming", "Missing flow name"),
    ("MS060", "VagueFlowNameCheck", "naming", "Vague flow name"),
    ("MS061", "InconsistentCasingInNamesCheck", "naming", "Inconsistent casing"),
    ("MS062", "AbbreviatedNameCheck", "naming", "Excessive abbreviations"),
    ("MS063", "MissingLoggerMessageCheck", "naming", "Missing logger message"),
    ("MS064", "UninformativeVariableNameCheck", "naming", "Uninformative variable name"),
    ("MS065", "MissingDocDescriptionCheck", "naming", "Missing doc description"),
    ("MS066", "TodoCommentCheck", "naming", "TODO comment found"),
    ("MS067", "OutdatedCommentCheck", "naming", "Outdated comment"),
    ("MS068", "MissingAPIDocumentationCheck", "naming", "Missing API documentation"),
    ("MS069", "InconsistentFlowNamingCheck", "naming", "Inconsistent flow naming"),
    ("MS070", "TooLongVariableNameCheck", "naming", "Variable name too long"),
    ("MS071", "MissingTransformationCommentCheck", "naming", "Missing transformation comment"),

    # Performance (MS072-MS084)
    ("MS072", "SynchronousProcessingCheck", "performance", "Synchronous processing detected"),
    ("MS073", "MissingCachingCheck", "performance", "Missing caching"),
    ("MS074", "IneffientDataWeaveTransformationCheck", "performance", "Inefficient DataWeave"),
    ("MS075", "ExcessiveLoggingCheck", "performance", "Excessive logging"),
    ("MS076", "MissingBatchProcessingCheck", "performance", "Missing batch processing"),
    ("MS077", "UnboundedScatterGatherCheck", "performance", "Unbounded scatter-gather"),
    ("MS078", "DatabaseQueryInLoopCheck", "performance", "Database query in loop"),
    ("MS079", "LargePayloadInMemoryCheck", "performance", "Large payload in memory"),
    ("MS080", "ExcessivePayloadTransformationCheck", "performance", "Excessive transformations"),
    ("MS081", "MissingConnectionPoolingCheck", "performance", "Missing connection pooling"),
    ("MS082", "SynchronousAPICallCheck", "performance", "Synchronous API call"),
    ("MS083", "IneffientXMLParsingCheck", "performance", "Inefficient XML parsing"),
    ("MS084", "UnoptimizedDataWeaveScriptCheck", "performance", "Unoptimized DataWeave"),

    # Error Handling (MS085-MS096)
    ("MS085", "MissingGlobalErrorHandlerCheck", "errorhandling", "Missing global error handler"),
    ("MS086", "EmptyErrorHandlerCheck", "errorhandling", "Empty error handler"),
    ("MS087", "GenericErrorCatchCheck", "errorhandling", "Generic error catch"),
    ("MS088", "ErrorSwallowingCheck", "errorhandling", "Error swallowing detected"),
    ("MS089", "MissingRetryStrategyCheck", "errorhandling", "Missing retry strategy"),
    ("MS090", "ExcessiveRetryCheck", "errorhandling", "Excessive retry attempts"),
    ("MS091", "MissingCircuitBreakerCheck", "errorhandling", "Missing circuit breaker"),
    ("MS092", "InconsistentErrorResponseCheck", "errorhandling", "Inconsistent error response"),
    ("MS093", "ExposedStackTraceCheck", "errorhandling", "Stack trace exposed"),
    ("MS094", "MissingTimeoutConfigurationCheck", "errorhandling", "Missing timeout"),
    ("MS095", "RethrowingGenericExceptionCheck", "errorhandling", "Rethrowing generic exception"),
    ("MS096", "UnhandledCustomErrorCheck", "errorhandling", "Unhandled custom error"),

    # Java Integration (MS097-MS110)
    ("MS097", "UntypedJavaInvocationCheck", "java", "Untyped Java invocation"),
    ("MS098", "MissingNullCheckCheck", "java", "Missing null check"),
    ("MS099", "UncheckedCastCheck", "java", "Unchecked cast"),
    ("MS100", "DeprecatedJavaMethodCheck", "java", "Deprecated Java method"),
    ("MS101", "ExceptionNotHandledCheck", "java", "Exception not handled"),
    ("MS102", "MissingJavaClassValidationCheck", "java", "Missing class validation"),
    ("MS103", "IneffientJavaCollectionCheck", "java", "Inefficient collection usage"),
    ("MS104", "ResourceLeakInJavaCodeCheck", "java", "Resource leak in Java code"),
    ("MS105", "ThreadSafetyIssueCheck", "java", "Thread safety issue"),
    ("MS106", "BlockingCallInJavaComponentCheck", "java", "Blocking call in component"),
    ("MS107", "ExcessiveJavaComplexityCheck", "java", "Excessive Java complexity"),
    ("MS108", "MissingSerializableImplementationCheck", "java", "Missing Serializable"),
    ("MS109", "HardcodedValuesInJavaCodeCheck", "java", "Hardcoded values in Java"),
    ("MS110", "UnoptimizedJavaRegexCheck", "java", "Unoptimized regex"),
]

TEMPLATE = '''package com.lioncorp.sonar.mulesoft.checks.{category};

import com.lioncorp.sonar.mulesoft.MuleSoftCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

/**
 * {description}.
 */
@Rule(key = "{key}")
public class {class_name} implements MuleSoftCheck {{

  private static final String REPOSITORY_KEY = "mulesoft";
  private static final String RULE_KEY = "{key}";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {{
    // TODO: Implement {key} check logic
    // Analyze parsedFile and report issues using reportIssue()
  }}

  private void reportIssue(SensorContext context, InputFile inputFile, String message) {{
    NewIssue issue = context.newIssue();
    NewIssueLocation location = issue.newLocation()
        .on(inputFile)
        .message(message);

    issue
        .at(location)
        .forRule(RuleKey.of(REPOSITORY_KEY, RULE_KEY))
        .save();
  }}
}}
'''

def generate_check(key, class_name, category, description):
    """Generate a single check class file."""
    content = TEMPLATE.format(
        key=key,
        class_name=class_name,
        category=category,
        description=description
    )

    # Determine output path - go to parent directory (project root)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.join(script_dir, "..")
    base_dir = os.path.join(project_root, "src/main/java/com/lioncorp/sonar/mulesoft/checks")
    output_dir = os.path.join(base_dir, category)
    os.makedirs(output_dir, exist_ok=True)

    output_file = os.path.join(output_dir, f"{class_name}.java")

    # Skip if already exists (don't overwrite)
    if os.path.exists(output_file):
        print(f"Skipping {output_file} (already exists)")
        return False

    with open(output_file, 'w') as f:
        f.write(content)

    print(f"Generated {output_file}")
    return True

def main():
    """Generate all check classes."""
    print("Generating MuleSoft check classes...")
    generated = 0
    skipped = 0

    for key, class_name, category, description in RULES:
        if generate_check(key, class_name, category, description):
            generated += 1
        else:
            skipped += 1

    print(f"\nDone! Generated {generated} files, skipped {skipped} existing files.")

if __name__ == "__main__":
    main()
