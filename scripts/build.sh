#!/bin/bash

#######################################
# MuleSoft SonarQube Plugin Build Script
#######################################

set -e  # Exit on any error

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SKIP_TESTS=false
GENERATE_CODE=false
CLEAN_BUILD=true

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --generate)
      GENERATE_CODE=true
      shift
      ;;
    --no-clean)
      CLEAN_BUILD=false
      shift
      ;;
    --help)
      echo "Usage: scripts/build.sh [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --skip-tests    Skip running unit tests"
      echo "  --generate      Run code generation scripts (generate_checks.py, generate_registrations.py)"
      echo "  --no-clean      Skip 'mvn clean' step"
      echo "  --help          Show this help message"
      echo ""
      echo "Examples:"
      echo "  scripts/build.sh                    # Full build with tests"
      echo "  scripts/build.sh --skip-tests       # Build without tests"
      echo "  scripts/build.sh --generate         # Generate code skeletons then build"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      echo "Run './build.sh --help' for usage information"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  MuleSoft SonarQube Plugin Build${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}[1/6] Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17 or higher is required (found Java $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    exit 1
fi

MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
echo -e "${GREEN}✓ Maven $MVN_VERSION detected${NC}"

# Check Python (optional, only if --generate flag is used)
if [ "$GENERATE_CODE" = true ]; then
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}Error: Python 3 is not installed or not in PATH (required for --generate)${NC}"
        exit 1
    fi
    PYTHON_VERSION=$(python3 --version | awk '{print $2}')
    echo -e "${GREEN}✓ Python $PYTHON_VERSION detected${NC}"
fi

echo ""

# Change to project root directory
cd "$PROJECT_ROOT"

# Code generation (optional)
if [ "$GENERATE_CODE" = true ]; then
    echo -e "${YELLOW}[2/6] Running code generation scripts...${NC}"

    if [ -f "$SCRIPT_DIR/generate_checks.py" ]; then
        echo "Running generate_checks.py..."
        python3 "$SCRIPT_DIR/generate_checks.py"
        echo -e "${GREEN}✓ Check classes generated${NC}"
    else
        echo -e "${YELLOW}Warning: generate_checks.py not found, skipping${NC}"
    fi

    if [ -f "$SCRIPT_DIR/generate_registrations.py" ]; then
        echo "Running generate_registrations.py..."
        echo -e "${BLUE}Registration code snippets:${NC}"
        python3 "$SCRIPT_DIR/generate_registrations.py"
        echo -e "${GREEN}✓ Registration snippets generated (review output above)${NC}"
    else
        echo -e "${YELLOW}Warning: generate_registrations.py not found, skipping${NC}"
    fi

    echo ""
else
    echo -e "${YELLOW}[2/6] Skipping code generation (use --generate flag to enable)${NC}"
    echo ""
fi

# Maven clean
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "${YELLOW}[3/6] Cleaning previous build artifacts...${NC}"
    mvn clean
    echo -e "${GREEN}✓ Clean completed${NC}"
    echo ""
else
    echo -e "${YELLOW}[3/6] Skipping clean (--no-clean specified)${NC}"
    echo ""
fi

# Maven compile
echo -e "${YELLOW}[4/6] Compiling source code...${NC}"
mvn compile
echo -e "${GREEN}✓ Compilation successful${NC}"
echo ""

# Maven test
if [ "$SKIP_TESTS" = false ]; then
    echo -e "${YELLOW}[5/6] Running unit tests...${NC}"
    mvn test
    echo -e "${GREEN}✓ All tests passed${NC}"
    echo ""
else
    echo -e "${YELLOW}[5/6] Skipping tests (--skip-tests specified)${NC}"
    echo ""
fi

# Maven package
echo -e "${YELLOW}[6/6] Building plugin JAR...${NC}"
if [ "$SKIP_TESTS" = true ]; then
    mvn package -DskipTests
else
    mvn package
fi
echo -e "${GREEN}✓ Package created${NC}"
echo ""

# Show build results
echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Build Summary${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
RAW_JAR="target/sonar-mulesoft-plugin-${PROJECT_VERSION}-RAW.jar"
FINAL_JAR="target/sonar-mulesoft-plugin-${PROJECT_VERSION}-FINAL.jar"

if [ -f "$RAW_JAR" ]; then
    RAW_SIZE=$(du -h "$RAW_JAR" | cut -f1)
    echo -e "${GREEN}✓ RAW JAR (unshaded):${NC}"
    echo -e "  ${RAW_JAR} (${RAW_SIZE})"
else
    echo -e "${RED}✗ RAW JAR not found${NC}"
fi

echo ""

if [ -f "$FINAL_JAR" ]; then
    FINAL_SIZE=$(du -h "$FINAL_JAR" | cut -f1)
    echo -e "${GREEN}✓ FINAL JAR (shaded with dependencies):${NC}"
    echo -e "  ${FINAL_JAR} (${FINAL_SIZE})"
    echo ""
    echo -e "${YELLOW}📦 Deploy this JAR to SonarQube:${NC}"
    echo -e "   cp ${FINAL_JAR} \$SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar"
else
    echo -e "${RED}✗ FINAL JAR not found${NC}"
fi

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Build completed successfully!${NC}"
echo -e "${GREEN}======================================${NC}"
