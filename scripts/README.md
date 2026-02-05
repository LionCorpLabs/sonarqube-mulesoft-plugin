# Build Scripts

This directory contains development and build utilities for the MuleSoft SonarQube Plugin.

## Scripts

### `build.sh`

Comprehensive build script for the plugin with multiple options.

**Usage:**
```bash
# From project root
scripts/build.sh [OPTIONS]
```

**Options:**
- `--skip-tests` - Skip running unit tests (faster build)
- `--generate` - Run code generation scripts before building
- `--no-clean` - Skip the `mvn clean` step
- `--help` - Show help message

**Examples:**
```bash
scripts/build.sh                    # Full build with tests
scripts/build.sh --skip-tests       # Build without tests
scripts/build.sh --generate         # Generate code skeletons then build
```

**What it does:**
1. Checks prerequisites (Java 17+, Maven 3.6+, Python 3)
2. Optionally runs code generation scripts
3. Cleans previous build artifacts
4. Compiles source code
5. Runs unit tests (unless skipped)
6. Packages the plugin into RAW and FINAL JARs
7. Shows build summary with deployment instructions

---

### `generate_checks.py`

Generates skeleton Java check classes for all 110 rules.

**Usage:**
```bash
python3 scripts/generate_checks.py
```

**What it does:**
- Creates Java class files in `src/main/java/com/lioncorp/sonar/mulesoft/checks/`
- Organizes checks by category: `security`, `structure`, `naming`, `performance`, `errorhandling`, `java`
- Each class implements `MuleSoftCheck` interface with a TODO placeholder
- Skips files that already exist (won't overwrite implemented checks)

**When to use:**
- Bootstrapping the project structure
- Adding new rules to the plugin
- Regenerating skeleton code after defining new rule IDs

**Output example:**
```
Generated src/main/java/com/lioncorp/sonar/mulesoft/checks/security/HardcodedCredentialsCheck.java
Skipping src/main/java/com/lioncorp/sonar/mulesoft/checks/security/SQLInjectionCheck.java (already exists)
```

---

### `generate_registrations.py`

Generates code snippets for registering rules in the plugin.

**Usage:**
```bash
python3 scripts/generate_registrations.py
```

**What it does:**
- Outputs Java code snippets to stdout
- Generates registration code for `CheckList.java` (the `getChecks()` method)
- Generates rule definitions for `MuleSoftRulesDefinition.java` (the `define()` method)
- Includes full metadata: severity, type, descriptions, and tags

**When to use:**
- After generating check classes
- When adding new rules to the plugin
- To get properly formatted registration code

**Output example:**
```java
# CheckList.java - Add to getChecks() method:
return Arrays.asList(
    com.lioncorp.sonar.mulesoft.checks.security.HardcodedCredentialsCheck.class,
    com.lioncorp.sonar.mulesoft.checks.security.InsecureHTTPEndpointCheck.class,
    // ...
);

# MuleSoftRulesDefinition.java - Add to define() method:
repository.createRule("MS001")
    .setName("Credentials should not be hardcoded")
    .setHtmlDescription("Hardcoded credentials in configuration files pose serious security risks...")
    .setSeverity("BLOCKER")
    .setType(RuleType.VULNERABILITY)
    .setTags("security", "cwe", "owasp-a2");
```

---

## Development Workflow

### Initial Setup
```bash
# Generate all skeleton check classes
python3 scripts/generate_checks.py

# Review registration code
python3 scripts/generate_registrations.py

# Copy registration code to CheckList.java and MuleSoftRulesDefinition.java
```

### Regular Development
```bash
# Build with tests
scripts/build.sh

# Quick build without tests
scripts/build.sh --skip-tests
```

### Adding New Rules
```bash
# 1. Update RULES list in both Python scripts
# 2. Generate new check classes
scripts/build.sh --generate

# 3. Implement the check logic in generated classes
# 4. Add tests
# 5. Build and verify
scripts/build.sh
```

---

## Prerequisites

- **Java 17+** - Required for compilation
- **Maven 3.6+** - Required for building
- **Python 3** - Required for code generation scripts (optional if not using `--generate`)

## Notes

- The build script automatically changes to the project root directory
- Python scripts correctly reference the parent directory for file generation
- Generated files are never overwritten to preserve implemented check logic
- The FINAL JAR (shaded with dependencies) should be used for deployment to SonarQube
