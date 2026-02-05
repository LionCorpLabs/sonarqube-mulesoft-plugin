# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

#### Build Automation & Developer Tools
- **`scripts/` directory** - Centralized location for all build and development scripts
- **`scripts/build.sh`** - Comprehensive build automation script with multiple options:
  - `--skip-tests` flag for faster builds
  - `--generate` flag for code generation
  - `--no-clean` flag for incremental builds
  - Colored output for better readability
  - Prerequisite validation (Java 17+, Maven 3.6+, Python 3)
  - Build summary with deployment instructions

- **`scripts/generate_checks.py`** - Automated skeleton generation for check classes:
  - Generates Java check classes for all 110 rules
  - Organizes by category (security, structure, naming, performance, errorhandling, java)
  - Never overwrites existing implementations
  - Includes boilerplate code with TODO markers

- **`scripts/generate_registrations.py`** - Automated registration code generation:
  - Generates code snippets for CheckList.java
  - Generates rule definitions for MuleSoftRulesDefinition.java
  - Includes full metadata (severity, type, descriptions, tags)

- **`scripts/README.md`** - Comprehensive documentation for all scripts:
  - Usage instructions for each script
  - Development workflow guides
  - Examples and best practices

#### Documentation Enhancements
- **Table of Contents** in main README.md for easier navigation
- **"What's New"** section highlighting latest updates
- **"Quick Start"** section for rapid onboarding
- **Enhanced "Build Instructions"** with detailed script usage
- **"Build Scripts"** section documenting all automation tools
- **Expanded "Development Workflow"** with step-by-step guides
- **"Project Structure"** showing complete directory layout
- **"Troubleshooting"** section with common issues and solutions:
  - Build issues (Java version, Maven, Python)
  - Plugin installation issues
  - Analysis issues
  - Development issues

- Updated build instructions throughout documentation
- Added examples for adding new rules with code generation

### Changed
- Moved `build.sh` to `scripts/` directory
- Moved `generate_checks.py` to `scripts/` directory
- Moved `generate_registrations.py` to `scripts/` directory
- Updated `build.sh` to work from scripts directory (auto-navigates to project root)
- Updated `generate_checks.py` to reference parent directory correctly
- Updated all documentation references to use `scripts/` path

### Improved
- **Developer Experience**: Adding new rules now takes minutes instead of hours
- **Build Process**: Single command builds with all options
- **Code Consistency**: All generated code follows same structure
- **Documentation**: Comprehensive guides for all workflows
- **Error Prevention**: Automated generation reduces manual registration errors

## [1.0.0-SNAPSHOT] - 2026-01-XX

### Added
- Initial plugin implementation with 110 rules
- Support for MuleSoft XML file analysis
- Embedded Java code analysis using JavaParser
- DataWeave script analysis
- 6 rule categories: Security (30), Structure (28), Naming (13), Performance (13), Error Handling (12), Java Integration (14)
- Built-in quality profile with all rules enabled
- SonarLint support
- Example files (compliant and violations)

---

## Summary of This Release

This release focuses on **developer experience improvements** through build automation and code generation tools. The new `scripts/` directory contains everything needed to streamline the development workflow, from building the plugin to adding new rules.

**Key Benefits:**
- ⚡ **Faster Development**: Automated code generation and build scripts
- 📦 **Better Organization**: All scripts in dedicated directory
- 📚 **Enhanced Documentation**: Comprehensive guides for all workflows
- 🛠️ **Improved Tooling**: Command-line options for different build scenarios
- 🚀 **Easier Contribution**: Clear steps for adding new rules

**Migration Notes:**
- Build command changed from `./build.sh` to `scripts/build.sh`
- All Python scripts moved to `scripts/` directory
- See updated documentation for new workflow
