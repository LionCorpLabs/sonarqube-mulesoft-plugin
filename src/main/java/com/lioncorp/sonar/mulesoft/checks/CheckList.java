package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.MuleSoftCheck;

import java.util.Arrays;
import java.util.List;

/**
 * Registry of all available checks for MuleSoft files.
 */
public class CheckList {

  private CheckList() {
    // Utility class
  }

  /**
   * Get all available checks.
   *
   * @return list of all check classes
   */
  public static List<Class<? extends MuleSoftCheck>> getChecks() {
    List<Class<? extends MuleSoftCheck>> checks = new java.util.ArrayList<>();
    checks.addAll(getSecurityChecks());
    checks.addAll(getStructureChecks());
    checks.addAll(getNamingChecks());
    checks.addAll(getPerformanceChecks());
    checks.addAll(getErrorHandlingChecks());
    checks.addAll(getJavaChecks());
    return checks;
  }

  /**
   * Get security checks (MS001-MS030).
   *
   * @return list of security check classes
   */
  private static List<Class<? extends MuleSoftCheck>> getSecurityChecks() {
    return Arrays.asList(
        com.lioncorp.sonar.mulesoft.checks.security.HardcodedCredentialsCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.InsecureHTTPEndpointCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.SQLInjectionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.XMLExternalEntityInjectionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.CommandInjectionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.PathTraversalRiskCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.InsecureDeserializationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.WeakCryptographyCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingAuthenticationHeaderCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.InsecureRandomnessCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.HardcodedIPAddressCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.ClearTextProtocolCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingCSRFProtectionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.ExposedAdminEndpointCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.UnvalidatedRedirectCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.JavaClassSecurityCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingInputValidationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.SensitiveDataLoggingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingOAuthValidationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.InsecureCORSConfigurationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.DatabaseConnectionWithoutEncryptionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingRateLimitingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.ExcessiveDataExposureCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingContentTypeValidationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.UnsafeReflectionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.MissingSecurityHeadersCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.FileUploadWithoutValidationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.SessionManagementIssueCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.LDAPInjectionRiskCheck.class,
        com.lioncorp.sonar.mulesoft.checks.security.XMLBombRiskCheck.class
    );
  }

  /**
   * Get structure checks (MS031-MS058).
   *
   * @return list of structure check classes
   */
  private static List<Class<? extends MuleSoftCheck>> getStructureChecks() {
    return Arrays.asList(
        com.lioncorp.sonar.mulesoft.checks.structure.EmptyFlowCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.LargeFlowCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.DuplicatedFlowLogicCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.DeepFlowNestingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.UnusedSubFlowCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.CircularFlowReferenceCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.MissingErrorHandlerCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.TooManyFlowRefsCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.InconsistentFlowStructureCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.MixedResponsibilityFlowCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.CognitiveComplexityCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.ExcessiveChoiceBranchesCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.DeepDataWeaveExpressionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.LongParameterListCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.MagicNumberCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.ComplexBooleanExpressionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.GodFlowCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.TooManyVariablesCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.LongMethodNameCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.InappropriateIntimacyCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.InconsistentNamingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.MissingConfigurationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.DuplicatedConfigurationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.HardcodedEnvironmentValueCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.InconsistentTimeoutCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.DefaultConfigurationUsedCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.MissingDescriptionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.structure.ObsoleteConfigurationCheck.class
    );
  }

  /**
   * Get naming checks (MS059-MS071).
   *
   * @return list of naming check classes
   */
  private static List<Class<? extends MuleSoftCheck>> getNamingChecks() {
    return Arrays.asList(
        com.lioncorp.sonar.mulesoft.checks.naming.MissingFlowNameCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.VagueFlowNameCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.InconsistentCasingInNamesCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.AbbreviatedNameCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.MissingLoggerMessageCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.UninformativeVariableNameCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.MissingDocDescriptionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.TodoCommentCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.OutdatedCommentCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.MissingAPIDocumentationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.InconsistentFlowNamingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.TooLongVariableNameCheck.class,
        com.lioncorp.sonar.mulesoft.checks.naming.MissingTransformationCommentCheck.class
    );
  }

  /**
   * Get performance checks (MS072-MS084).
   *
   * @return list of performance check classes
   */
  private static List<Class<? extends MuleSoftCheck>> getPerformanceChecks() {
    return Arrays.asList(
        com.lioncorp.sonar.mulesoft.checks.performance.SynchronousProcessingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.MissingCachingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.IneffientDataWeaveTransformationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.ExcessiveLoggingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.MissingBatchProcessingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.UnboundedScatterGatherCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.DatabaseQueryInLoopCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.LargePayloadInMemoryCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.ExcessivePayloadTransformationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.MissingConnectionPoolingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.SynchronousAPICallCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.IneffientXMLParsingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.performance.UnoptimizedDataWeaveScriptCheck.class
    );
  }

  /**
   * Get error handling checks (MS085-MS096).
   *
   * @return list of error handling check classes
   */
  private static List<Class<? extends MuleSoftCheck>> getErrorHandlingChecks() {
    return Arrays.asList(
        com.lioncorp.sonar.mulesoft.checks.errorhandling.MissingGlobalErrorHandlerCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.EmptyErrorHandlerCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.GenericErrorCatchCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.ErrorSwallowingCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.MissingRetryStrategyCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.ExcessiveRetryCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.MissingCircuitBreakerCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.InconsistentErrorResponseCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.ExposedStackTraceCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.MissingTimeoutConfigurationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.RethrowingGenericExceptionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.errorhandling.UnhandledCustomErrorCheck.class
    );
  }

  /**
   * Get Java integration checks (MS097-MS110).
   *
   * @return list of Java integration check classes
   */
  private static List<Class<? extends MuleSoftCheck>> getJavaChecks() {
    return Arrays.asList(
        com.lioncorp.sonar.mulesoft.checks.java.UntypedJavaInvocationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.MissingNullCheckCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.UncheckedCastCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.DeprecatedJavaMethodCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.ExceptionNotHandledCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.MissingJavaClassValidationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.IneffientJavaCollectionCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.ResourceLeakInJavaCodeCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.ThreadSafetyIssueCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.BlockingCallInJavaComponentCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.ExcessiveJavaComplexityCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.MissingSerializableImplementationCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.HardcodedValuesInJavaCodeCheck.class,
        com.lioncorp.sonar.mulesoft.checks.java.UnoptimizedJavaRegexCheck.class
    );
  }
}
