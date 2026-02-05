⚠️ Disclaimer: This project is in **no way affiliated with or endorsed by SonarSource or MuleSoft.** It is an independent open-source initiative developed to enhance code quality and security for MuleSoft applications. Not fully perfect, and if you find any room for improvement, please leave a comment in the Issues section of this GitHub repository. Thank you!

#  🫏 🐋 SonarQube MuleSoft Plugin

A comprehensive static code analysis plugin for SonarQube that analyzes MuleSoft XML configuration files and embedded Java code to detect security vulnerabilities, code quality issues, performance problems, and maintainability concerns.

## Table of Contents

- [What's New](#whats-new)
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Usage](#usage)
- [Rule Catalog](#rule-catalog)
- [Code Examples](#code-examples)
- [Building from Source](#building-from-source)
  - [Build Instructions](#build-instructions)
  - [Build Scripts](#build-scripts)
- [Testing](#testing)
- [Technical Details](#technical-details)
- [Contributing](#contributing)
  - [Development Workflow](#development-workflow)
  - [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Reporting Issues](#reporting-issues)
- [License](#license)
- [Support](#support)

## What's New

### Latest Updates

**Build Automation & Developer Tools:**
- ✨ **New `scripts/` directory** with automated build and code generation tools
- 🚀 **`build.sh`** - Comprehensive build script with multiple options (skip tests, code generation, incremental builds)
- 🔧 **`generate_checks.py`** - Automated skeleton generation for all 110 rule check classes
- 📝 **`generate_registrations.py`** - Automated generation of rule registration code
- 📚 **Enhanced documentation** with detailed build workflows and developer guides
- ⚡ **Improved developer experience** - From hours to minutes when adding new rules

**Benefits:**
- Faster onboarding for new contributors
- Consistent code structure across all rules
- Reduced errors in rule registration
- Streamlined build process
- Better documentation for development workflows

## Overview

The **SonarQube MuleSoft Plugin** provides 110 carefully crafted rules organized into 6 categories to help development teams build secure, maintainable, and high-performance MuleSoft applications. The plugin analyzes MuleSoft XML files with embedded Java code and DataWeave scripts to identify potential issues before they reach production.

### Key Features

- **110 Comprehensive Rules** covering security, structure, naming, performance, error handling, and Java integration
- **Security-First Approach** with 30 dedicated security rules detecting vulnerabilities like SQL injection, XXE, hardcoded credentials, and more
- **Performance Optimization** rules identifying N+1 query problems, inefficient DataWeave scripts, and synchronous bottlenecks
- **Code Quality Enforcement** detecting god flows, duplicated logic, deep nesting, and high cognitive complexity
- **Built-in Quality Profile** pre-configured with all rules enabled for immediate use
- **SonarLint Support** for real-time feedback in IDEs
- **Embedded Java Analysis** using JavaParser for analyzing custom Java components

## Quick Start

### For Plugin Users

1. **Build the plugin**:
   ```bash
   scripts/build.sh
   ```

2. **Deploy to SonarQube**:
   ```bash
   cp target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar
   ```

3. **Restart SonarQube** and start analyzing your MuleSoft projects!

### For Plugin Developers

See the [Building from Source](#building-from-source) section for detailed development workflow.

## Installation

### Prerequisites

- SonarQube Server 9.9 or higher
- Java 17 or higher
- Maven 3.6+ (for building from source)

### Installation Steps

1. **Build the plugin** using the build script (recommended):
   ```bash
   scripts/build.sh
   ```

   Or build directly with Maven:
   ```bash
   mvn clean package
   ```

2. **Locate the plugin JAR**:

   The build produces two JAR files:
   - `target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-RAW.jar` - Unshaded jar without dependencies
   - `target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar` - Shaded jar with all dependencies (use this one)

3. **Copy to SonarQube plugins directory**:
   ```bash
   cp target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar
   ```

   **Note**: Use the **FINAL** jar as it contains all bundled dependencies (JavaParser, Commons Lang).

4. **Restart SonarQube**:
   ```bash
   $SONARQUBE_HOME/bin/[your-os]/sonar.sh restart
   ```

5. **Verify installation**:
   - Navigate to **Administration > Marketplace > Installed**
   - Confirm "MuleSoft" plugin is listed and active

## Usage

### Running Analysis

1. **Configure your project** with a `sonar-project.properties` file:
   ```properties
   sonar.projectKey=my-mulesoft-project
   sonar.projectName=My MuleSoft Project
   sonar.projectVersion=1.0
   sonar.sources=src/main/mule
   sonar.language=mulesoft
   sonar.sourceEncoding=UTF-8
   ```

2. **Run the SonarQube scanner**:
   ```bash
   sonar-scanner
   ```

   Or using Maven:
   ```bash
   mvn sonar:sonar
   ```

### Applying the Quality Profile

The plugin includes a built-in quality profile called **"MuleSoft Quality Profile"** with all 110 rules enabled.

1. Navigate to **Quality Profiles** in SonarQube
2. Find "MuleSoft Quality Profile"
3. Set it as the default profile for your MuleSoft projects

### Configuration Options

Rules can be customized in the Quality Profile:

- **Activate/Deactivate** individual rules
- **Adjust severity levels** (Blocker, Critical, Major, Minor, Info)
- **Configure rule parameters** where applicable
- **Create custom profiles** for different project types

## Rule Catalog

The plugin provides **110 rules** organized into 6 categories:

### Security Vulnerabilities (MS001-MS015) - 15 Rules

Critical security vulnerabilities that must be addressed immediately:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS001 | Credentials should not be hardcoded | HIGH | Detects hardcoded credentials in configuration |
| MS002 | HTTP endpoints should use HTTPS | HIGH | Identifies insecure HTTP endpoints |
| MS003 | SQL queries should not use string concatenation | HIGH | Detects SQL injection vulnerabilities |
| MS004 | XML parsers should be protected against XXE | HIGH | Identifies XML External Entity vulnerabilities |
| MS005 | OS commands should not be constructed from user input | HIGH | Detects command injection risks |
| MS006 | File paths should not be constructed from user input | HIGH | Identifies path traversal vulnerabilities |
| MS007 | Deserialization should be restricted to safe classes | HIGH | Detects insecure deserialization |
| MS008 | Cryptographic algorithms should be strong | HIGH | Identifies weak cryptographic algorithms |
| MS009 | API endpoints should validate authentication headers | HIGH | Detects missing authentication checks |
| MS010 | Random values should use secure generators | MEDIUM | Identifies use of insecure random |
| MS011 | Redirects should validate target URLs | MEDIUM | Detects unvalidated redirect vulnerabilities |
| MS012 | Network protocols should use encryption | HIGH | Identifies cleartext protocol usage |
| MS013 | LDAP queries should not be constructed from user input | HIGH | Detects LDAP injection vulnerabilities |
| MS014 | API responses should not expose excessive data | MEDIUM | Identifies excessive data exposure |
| MS015 | CORS configuration should be restrictive | MEDIUM | Detects insecure CORS configuration |

### Security Hotspots (MS016-MS030) - 15 Rules

Security-sensitive areas requiring review:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS016 | Dangerous Java classes should be reviewed | MEDIUM | Detects usage of Runtime, ProcessBuilder |
| MS017 | User input should be validated | HIGH | Identifies missing input validation |
| MS018 | Sensitive data should not be logged | HIGH | Detects logging of passwords, tokens, PII |
| MS019 | OAuth tokens and JWTs should be validated | HIGH | Identifies missing OAuth/JWT validation |
| MS020 | Database connections should use encryption | HIGH | Detects unencrypted database connections |
| MS021 | TLS version should be 1.2 or higher | HIGH | Identifies weak TLS versions |
| MS022 | Admin endpoints should not be publicly accessible | HIGH | Detects exposed admin endpoints |
| MS023 | File uploads should validate type and size | HIGH | Identifies unvalidated file uploads |
| MS024 | IP addresses should not be hardcoded | MEDIUM | Detects hardcoded IP addresses |
| MS025 | Content-Type headers should be validated | MEDIUM | Identifies missing Content-Type validation |
| MS026 | State-changing operations should have CSRF protection | HIGH | Detects missing CSRF protection |
| MS027 | Session tokens should be regenerated after authentication | HIGH | Identifies session fixation risks |
| MS028 | Sessions should have appropriate timeouts | MEDIUM | Detects improper session timeout configuration |
| MS029 | Session cookies should be secure and HttpOnly | HIGH | Identifies insecure cookie configuration |
| MS030 | XML processing should limit entity expansion | HIGH | Detects XML bomb (billion laughs) vulnerabilities |

### Structure Rules (MS031-MS058) - 28 Rules

Code organization, architecture, and quality:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS031 | Empty flows should be removed | MEDIUM | Detects flows without components |
| MS032 | Large flows should be broken down | MEDIUM | Identifies flows with >15 components |
| MS033 | Duplicated flow logic should be extracted | MEDIUM | Detects code duplication |
| MS034 | Flow nesting should not exceed 3 levels | MEDIUM | Identifies excessive nesting |
| MS035 | Unused sub-flows should be removed | LOW | Detects unreferenced sub-flows |
| MS036 | Circular flow references should be avoided | HIGH | Identifies circular dependencies |
| MS037 | Flows should have error handlers | MEDIUM | Detects missing error handlers |
| MS038 | Flows should not have excessive flow-ref calls | MEDIUM | Identifies >5 flow-ref calls |
| MS039 | Flow structure should be consistent | LOW | Detects structural inconsistencies |
| MS040 | Flows should have single responsibility | MEDIUM | Identifies mixed responsibility flows |
| MS041 | Cognitive complexity should be low | MEDIUM | Detects high cognitive complexity |
| MS042 | Choice components should not have excessive branches | MEDIUM | Identifies >7 choice branches |
| MS043 | DataWeave expressions should not be deeply nested | MEDIUM | Detects complex nested DataWeave |
| MS044 | Flows should not have long parameter lists | LOW | Identifies >5 parameters |
| MS045 | Magic numbers should be replaced with constants | LOW | Detects hardcoded numbers |
| MS046 | Boolean expressions should not be overly complex | MEDIUM | Identifies complex boolean logic |
| MS047 | God flows should be refactored | HIGH | Detects flows with >20 components |
| MS048 | Flows should not set excessive variables | MEDIUM | Identifies >7 variables |
| MS049 | Method names should not be excessively long | LOW | Detects names >50 characters |
| MS050 | Flows should not have tight coupling | MEDIUM | Identifies inappropriate intimacy |
| MS051 | Naming conventions should be consistent | LOW | Detects inconsistent naming |
| MS052 | Required configurations should not be missing | MEDIUM | Identifies missing configurations |
| MS053 | Configuration should not be duplicated | LOW | Detects duplicated configuration |
| MS054 | Environment-specific values should be externalized | MEDIUM | Identifies hardcoded environment values |
| MS055 | Timeouts should be consistent across flows | LOW | Detects inconsistent timeouts |
| MS056 | Default configurations should be overridden | LOW | Identifies usage of defaults |
| MS057 | Components should have descriptions | LOW | Detects missing doc:description |
| MS058 | Deprecated configurations should be updated | MEDIUM | Identifies obsolete configurations |

### Naming Rules (MS059-MS071) - 13 Rules

Naming conventions and documentation standards:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS059 | All flows must have descriptive names | MEDIUM | Detects missing flow names |
| MS060 | Flow names should not be vague | LOW | Identifies generic names like "process" |
| MS061 | Flow naming should use consistent casing | LOW | Detects inconsistent casing styles |
| MS062 | Names should not contain excessive abbreviations | LOW | Identifies unclear abbreviations |
| MS063 | Logger components must have meaningful messages | MEDIUM | Detects vague log messages |
| MS064 | Variable names should be informative | LOW | Identifies generic variable names |
| MS065 | Flows should have doc:description elements | MEDIUM | Detects missing documentation |
| MS066 | TODO/FIXME comments should be resolved | LOW | Identifies unresolved TODO markers |
| MS067 | Outdated comments should be updated or removed | LOW | Detects stale documentation |
| MS068 | API endpoints should have documentation | MEDIUM | Identifies undocumented endpoints |
| MS069 | Flow names should follow project conventions | LOW | Detects naming pattern inconsistencies |
| MS070 | Names should not be excessively long | LOW | Identifies names >50 characters |
| MS071 | DataWeave transformations should be commented | LOW | Detects uncommented complex DataWeave |

### Performance Rules (MS072-MS084) - 13 Rules

Performance optimization and efficiency:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS072 | Use async processing for independent operations | MEDIUM | Identifies synchronous bottlenecks |
| MS073 | Frequently accessed data should be cached | MEDIUM | Detects missing caching opportunities |
| MS074 | DataWeave transformations should be optimized | MEDIUM | Identifies inefficient DataWeave scripts |
| MS075 | Logging should not be excessive | LOW | Detects performance-impacting logging |
| MS076 | Large datasets should use batch processing | MEDIUM | Identifies missing batch processing |
| MS077 | Scatter-gather should have timeout configuration | MEDIUM | Detects unbounded scatter-gather |
| MS078 | Database operations should not be in loops | HIGH | Identifies N+1 query problems |
| MS079 | Large payloads should be streamed | HIGH | Detects in-memory loading of large files |
| MS080 | Avoid multiple transformations of same payload | MEDIUM | Identifies redundant transformations |
| MS081 | Database connectors should use connection pooling | MEDIUM | Detects missing connection pooling |
| MS082 | API calls should be asynchronous when possible | MEDIUM | Identifies blocking API calls |
| MS083 | XML parsing should be efficient | MEDIUM | Detects inefficient XML parsing |
| MS084 | DataWeave scripts should be reviewed for optimization | MEDIUM | Identifies unoptimized DataWeave |

### Error Handling Rules (MS085-MS096) - 12 Rules

Exception handling and resilience patterns:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS085 | Applications should have global error handlers | HIGH | Detects missing global error handlers |
| MS086 | Error handlers should not be empty | MEDIUM | Identifies empty error handlers |
| MS087 | Avoid catching generic error types | MEDIUM | Detects catch-all error handlers |
| MS088 | Errors should not be silently swallowed | HIGH | Identifies silent error suppression |
| MS089 | External calls should have retry strategies | MEDIUM | Detects missing retry logic |
| MS090 | Retry counts should be reasonable | MEDIUM | Identifies excessive retry attempts |
| MS091 | Circuit breakers should protect external dependencies | MEDIUM | Detects missing circuit breakers |
| MS092 | Error responses should be consistent | MEDIUM | Identifies inconsistent error formats |
| MS093 | Stack traces should not be exposed to clients | HIGH | Detects exposed stack traces |
| MS094 | External operations should have timeouts | HIGH | Identifies missing timeout configuration |
| MS095 | Avoid rethrowing generic exceptions | LOW | Detects generic exception rethrowing |
| MS096 | Custom errors should be handled | MEDIUM | Identifies unhandled custom errors |

### Java Integration Rules (MS097-MS110) - 14 Rules

Java code quality for custom components:

| Rule ID | Name | Severity | Description |
|---------|------|----------|-------------|
| MS097 | Java invocations should be type-safe | MEDIUM | Detects untyped Java invocations |
| MS098 | Java code should check for null values | HIGH | Identifies missing null checks |
| MS099 | Type casts should be checked | MEDIUM | Detects unchecked casts |
| MS100 | Deprecated Java methods should be replaced | LOW | Identifies deprecated API usage |
| MS101 | Java exceptions should be handled | HIGH | Detects unhandled exceptions |
| MS102 | Java components should validate inputs | MEDIUM | Identifies missing input validation |
| MS103 | Java collections should be efficient | MEDIUM | Detects inefficient collection usage |
| MS104 | Resources should be properly closed | HIGH | Identifies resource leaks |
| MS105 | Java code should be thread-safe | HIGH | Detects thread-safety issues |
| MS106 | Blocking calls should not be in Java components | MEDIUM | Identifies blocking operations |
| MS107 | Java code complexity should be manageable | MEDIUM | Detects high cyclomatic complexity |
| MS108 | Serializable classes should implement properly | MEDIUM | Identifies serialization issues |
| MS109 | Hardcoded values should be extracted to configuration | LOW | Detects hardcoded values in Java |
| MS110 | Regular expressions should be optimized | MEDIUM | Identifies inefficient regex patterns |

## Code Examples

### Compliant Code Example

```xml
<!-- Secure configuration with property placeholders -->
<http:request-config name="Secure_API_Config">
    <http:request-connection host="${api.host}" port="${api.port}" protocol="HTTPS">
        <http:authentication>
            <http:basic-authentication
                username="${api.username}"
                password="${secure::api.password}"/>
        </http:authentication>
    </http:request-connection>
</http:request-config>

<!-- Well-structured flow with proper error handling -->
<flow name="get-user-by-id-flow" doc:name="Retrieve User By ID">
    <doc:description>
        Retrieves user information by user ID.
        Validates input, queries database using parameterized query.
    </doc:description>

    <http:listener config-ref="HTTPS_Listener_Config"
                   path="/api/v1/users/{userId}"/>

    <flow-ref name="validate-user-id-subflow"/>
    <flow-ref name="get-user-from-database-subflow"/>

    <!-- Parameterized query prevents SQL injection -->
    <db:select config-ref="Secure_Database_Config" queryTimeout="5">
        <db:sql><![CDATA[
            SELECT id, username, email, first_name, last_name
            FROM users
            WHERE id = :userId AND active = true
        ]]></db:sql>
        <db:input-parameters><![CDATA[#[{
            userId: vars.userId
        }]]]></db:input-parameters>
    </db:select>

    <error-handler>
        <on-error-propagate type="DB:QUERY_EXECUTION">
            <logger level="ERROR" message="Database query failed"/>
            <!-- Consistent error response format -->
            <ee:transform>
                <ee:set-payload><![CDATA[{
                    "error": "DATABASE_ERROR",
                    "message": "Unable to retrieve user"
                }]]></ee:set-payload>
            </ee:transform>
        </on-error-propagate>
    </error-handler>
</flow>
```

### Violation Examples

See the complete example files:
- **Compliant examples**: [`examples/compliant-example.xml`](examples/compliant-example.xml)
- **Violation examples**: [`examples/violations-example.xml`](examples/violations-example.xml)

Common violations detected:

```xml
<!-- MS001: Hardcoded credentials -->
<http:basic-authentication username="admin" password="password123"/>

<!-- MS003: SQL injection risk -->
<db:sql>SELECT * FROM users WHERE id = #[payload.userId]</db:sql>

<!-- MS018: Sensitive data logging -->
<logger message="Password: #[payload.password]"/>

<!-- MS078: N+1 query problem -->
<foreach>
    <db:select>
        <db:sql>SELECT * FROM details WHERE user_id = #[payload.id]</db:sql>
    </db:select>
</foreach>

<!-- MS037: Missing error handler -->
<flow name="risky-operation">
    <http:request path="/external"/>
    <!-- No error-handler element -->
</flow>
```

## Building from Source

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+** or higher
- Git

### Build Instructions

#### Using the Build Script (Recommended)

The project includes a comprehensive build script that handles all build steps:

```bash
# Clone the repository
git clone https://github.com/your-org/lioncorp-mulesoft-plugin.git
cd lioncorp-mulesoft-plugin

# Full build with tests
scripts/build.sh

# Quick build without tests (faster)
scripts/build.sh --skip-tests

# Generate skeleton code for new rules, then build
scripts/build.sh --generate

# Skip the clean step for incremental builds
scripts/build.sh --no-clean

# Show all options
scripts/build.sh --help
```

**What the build script does:**
1. Checks prerequisites (Java 17+, Maven 3.6+)
2. Optionally runs code generation scripts
3. Cleans previous build artifacts
4. Compiles source code
5. Runs unit tests (unless skipped)
6. Packages plugin into JAR files
7. Shows build summary with deployment instructions

#### Using Maven Directly

```bash
# Standard build
mvn clean package

# Run tests only
mvn test

# Skip tests for faster build
mvn clean package -DskipTests

# Clean only
mvn clean
```

### Build Output

The build process creates two JAR files:

1. **RAW jar** (unshaded, without dependencies):
   ```
   target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-RAW.jar
   ```

2. **FINAL jar** (shaded, with all dependencies - **use this for deployment**):
   ```
   target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar
   ```

The **FINAL** jar includes all required dependencies (JavaParser, Commons Lang) and is the artifact you should deploy to SonarQube.

### Build Scripts

The `scripts/` directory contains automated build and code generation utilities:

#### `build.sh` - Main Build Script

Comprehensive build automation with multiple options:

```bash
scripts/build.sh [OPTIONS]

Options:
  --skip-tests    Skip unit tests (faster builds)
  --generate      Run code generation before building
  --no-clean      Skip Maven clean step
  --help          Show usage information
```

**Features:**
- Validates prerequisites (Java 17+, Maven 3.6+, Python 3)
- Optionally generates skeleton check classes
- Runs full Maven build lifecycle
- Creates both RAW and FINAL JAR files
- Provides deployment instructions
- Color-coded output for easy reading

#### `generate_checks.py` - Check Class Generator

Generates skeleton Java check classes for all 110 rules:

```bash
python3 scripts/generate_checks.py
```

**What it does:**
- Creates Java files in `src/main/java/com/lioncorp/sonar/mulesoft/checks/`
- Organizes by category: `security`, `structure`, `naming`, `performance`, `errorhandling`, `java`
- Each class implements `MuleSoftCheck` with TODO placeholders
- Never overwrites existing implementations (safe to re-run)

**When to use:**
- Bootstrapping new rule implementations
- Adding new rules to the plugin
- Regenerating skeleton code after defining new rule IDs

#### `generate_registrations.py` - Registration Code Generator

Generates code snippets for registering rules:

```bash
python3 scripts/generate_registrations.py
```

**What it generates:**
- Registration code for `CheckList.java` (`getChecks()` method)
- Rule definitions for `MuleSoftRulesDefinition.java` (`define()` method)
- Includes full metadata: severity, type, descriptions, tags

**When to use:**
- After generating new check classes
- When adding rules to the plugin
- To get properly formatted registration code

#### Scripts README

See [scripts/README.md](scripts/README.md) for detailed documentation on each script, usage examples, and development workflows.

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=HardcodedCredentialsCheckTest

# Run tests with coverage
mvn clean verify
```

### Example Files

The `examples/` directory contains sample MuleSoft XML files for testing:

- **`compliant-example.xml`** - Examples of code that follows all rules
- **`violations-example.xml`** - Examples of common violations for each rule category

You can use these files to:
1. Verify the plugin is working correctly
2. Understand what violations look like
3. Learn best practices for MuleSoft development

## Technical Details

### Compatibility

- **SonarQube Version**: 9.9+ (tested with 10.x)
- **SonarQube Plugin API**: 10.7.0.2191
- **Java Version**: 17 (minimum)
- **Maven Version**: 3.6+ (for building)

### Supported File Types

- **Primary**: `.xml` files in MuleSoft format
- **Embedded Analysis**: Java code within `<java>` components
- **DataWeave**: Scripts within `<ee:transform>` elements

### Languages Detected

- **MuleSoft XML**: Primary language for flow configurations
- **Embedded Java**: Custom Java components and transformers
- **DataWeave**: Transformation scripts (static analysis)

### Dependencies

The plugin uses:
- **JavaParser 3.25.8** - For parsing embedded Java code
- **SonarQube Plugin API 10.7** - Core SonarQube integration
- **Apache Commons Lang 2.6** - Utility functions

### Architecture

```
MuleSoftPlugin
├── MuleSoftLanguage - Language definition
├── MuleSoftRulesDefinition - Rule repository
├── MuleSoftQualityProfile - Default quality profile
├── MuleSoftSensor - File analysis orchestration
└── checks/
    ├── security/ - Security vulnerability checks
    ├── structure/ - Code organization checks
    ├── naming/ - Naming convention checks
    ├── performance/ - Performance optimization checks
    ├── errorhandling/ - Error handling checks
    └── java/ - Java integration checks
```

## Contributing

We welcome contributions to improve the MuleSoft SonarQube Plugin!

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/new-rule`
3. **Make your changes**: Add new rules or improve existing ones
4. **Write tests**: Ensure all changes are covered by unit tests
5. **Run tests**: `scripts/build.sh` (or `mvn clean verify`)
6. **Submit a pull request**: Describe your changes clearly

### Development Workflow

#### Adding New Rules

The project includes automated code generation to streamline rule development:

1. **Define the rule** in both Python scripts:
   - Add rule metadata to `scripts/generate_checks.py` (RULES list)
   - Add full metadata to `scripts/generate_registrations.py` (RULES list)

2. **Generate skeleton code**:
   ```bash
   scripts/build.sh --generate
   ```
   This creates the Java check class with boilerplate code.

3. **Implement the check logic**:
   - Navigate to `src/main/java/com/lioncorp/sonar/mulesoft/checks/{category}/`
   - Implement the `scanFile()` method in your generated class
   - Use `reportIssue()` to report violations

4. **Register the rule**:
   - Add to `CheckList.java` (use output from `generate_registrations.py`)
   - Add to `MuleSoftRulesDefinition.java` (use output from `generate_registrations.py`)

5. **Write tests**:
   - Create test class in `src/test/java/`
   - Add test XML files with violations and compliant code
   - Verify rule detects violations correctly

6. **Build and test**:
   ```bash
   scripts/build.sh
   ```

7. **Update documentation**:
   - Add rule to README.md rule catalog
   - Add examples to `examples/` directory

#### Example: Adding Rule MS111

```bash
# 1. Update RULES in scripts/generate_checks.py and scripts/generate_registrations.py
# 2. Generate skeleton
python3 scripts/generate_checks.py

# 3. Implement the check logic
# Edit: src/main/java/com/lioncorp/sonar/mulesoft/checks/security/MyNewCheck.java

# 4. Get registration code
python3 scripts/generate_registrations.py

# 5. Add to CheckList.java and MuleSoftRulesDefinition.java

# 6. Write tests
# Create: src/test/java/.../MyNewCheckTest.java

# 7. Build and verify
scripts/build.sh
```

### Code Style

- Follow existing code patterns
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and concise

### Project Structure

```
lioncorp-mulesoft-plugin/
├── scripts/                              # Build and code generation scripts
│   ├── build.sh                         # Main build automation script
│   ├── generate_checks.py               # Generates skeleton check classes
│   ├── generate_registrations.py        # Generates registration code
│   └── README.md                        # Scripts documentation
├── src/
│   ├── main/
│   │   └── java/com/lioncorp/sonar/mulesoft/
│   │       ├── MuleSoftPlugin.java      # Plugin entry point
│   │       ├── MuleSoftLanguage.java    # Language definition
│   │       ├── MuleSoftSensor.java      # File analysis orchestrator
│   │       ├── MuleSoftRulesDefinition.java  # Rule metadata registry
│   │       ├── MuleSoftQualityProfile.java   # Default quality profile
│   │       ├── CheckList.java           # Check class registry
│   │       ├── checks/                  # Rule implementations
│   │       │   ├── security/           # Security rules (MS001-MS030)
│   │       │   ├── structure/          # Structure rules (MS031-MS058)
│   │       │   ├── naming/             # Naming rules (MS059-MS071)
│   │       │   ├── performance/        # Performance rules (MS072-MS084)
│   │       │   ├── errorhandling/      # Error handling rules (MS085-MS096)
│   │       │   └── java/               # Java integration rules (MS097-MS110)
│   │       └── parser/
│   │           └── MuleSoftFileParser.java   # XML/Java parser
│   └── test/                            # Unit tests
├── examples/                            # Example MuleSoft files
│   ├── compliant-example.xml           # Compliant code examples
│   └── violations-example.xml          # Violation examples
├── pom.xml                             # Maven build configuration
└── README.md                           # This file
```

## Troubleshooting

### Build Issues

#### Java Version Error
```
Error: Java 17 or higher is required
```
**Solution:** Install Java 17+ or set `JAVA_HOME` to Java 17+ installation:
```bash
export JAVA_HOME=/path/to/java17
java -version  # Verify version
```

#### Maven Not Found
```
Error: Maven is not installed or not in PATH
```
**Solution:** Install Maven 3.6+ and add to PATH:
```bash
# macOS with Homebrew
brew install maven

# Verify installation
mvn -version
```

#### Python Script Errors (with --generate flag)
```
Error: Python 3 is not installed
```
**Solution:** Only needed when using `--generate` flag. Install Python 3:
```bash
# macOS with Homebrew
brew install python3

# Verify installation
python3 --version
```

#### Build Fails with "Classes not found"
**Issue:** Missing generated check classes or registration.

**Solution:**
1. Run code generation:
   ```bash
   scripts/build.sh --generate
   ```
2. Verify generated classes in `src/main/java/com/lioncorp/sonar/mulesoft/checks/`
3. Check `CheckList.java` and `MuleSoftRulesDefinition.java` have all rules registered

### Plugin Installation Issues

#### Plugin Not Showing in SonarQube
**Checklist:**
1. Verify you copied the **FINAL** jar (not the RAW jar)
2. Check jar is in `$SONARQUBE_HOME/extensions/plugins/`
3. Verify jar has correct permissions (readable by SonarQube process)
4. Check SonarQube logs: `$SONARQUBE_HOME/logs/sonar.log`
5. Restart SonarQube completely

#### ClassNotFoundException in SonarQube Logs
**Issue:** Using RAW jar instead of FINAL jar.

**Solution:** Use the shaded FINAL jar:
```bash
cp target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar \
   $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar
```

### Analysis Issues

#### Files Not Being Analyzed
**Checklist:**
1. Verify `sonar.language=mulesoft` in `sonar-project.properties`
2. Check file extensions are `.xml`
3. Verify `sonar.sources` points to correct directory
4. Check SonarQube scanner logs for file detection

#### Rules Not Being Applied
**Solution:**
1. Go to **Quality Profiles** in SonarQube
2. Find "MuleSoft Quality Profile"
3. Set as default for your project
4. Or activate rules in your custom profile

### Development Issues

#### Generated Code Not Compiling
**Solution:**
1. Ensure you updated both Python scripts with rule metadata
2. Run clean build: `scripts/build.sh`
3. Check for typos in rule keys or class names

#### Tests Failing
**Solution:**
1. Run tests with verbose output: `mvn test`
2. Check test XML files in test resources
3. Verify check logic matches test expectations
4. Run individual test: `mvn test -Dtest=YourCheckTest`

### Getting Help

If you encounter issues not covered here:
1. Check [scripts/README.md](scripts/README.md) for detailed script documentation
2. Search existing GitHub issues
3. Enable verbose logging in Maven: `mvn -X clean package`
4. Include full error messages and logs when reporting issues

## Reporting Issues

Found a bug or have a feature request?

1. **Check existing issues** to avoid duplicates
2. **Create a new issue** with:
   - Clear description of the problem
   - Steps to reproduce (for bugs)
   - Expected vs actual behavior
   - MuleSoft XML sample (if applicable)
   - Plugin version and SonarQube version

## License

Copyright (c) 2026 LionCorpWay

This project is licensed under the terms specified in the LICENSE file.

---

## Support

For questions, issues, or feature requests:
- **GitHub Issues**: [Report an issue](https://github.com/your-org/lioncorp-mulesoft-plugin/issues)
- **Documentation**: [Wiki](https://github.com/your-org/lioncorp-mulesoft-plugin/wiki)

---

**Built with quality in mind by the LionCorp team**
