#!/bin/bash

# SonarQube Scanner Script for MuleSoft Plugin
# This script runs the SonarQube scanner on the plugin repository itself

export SONAR_SCANNER_OPTS="-Xmx8G -Xms8G -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication"

export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"


set -e  # Exit on error

echo "========================================"
echo "MuleSoft Plugin - SonarQube Scanner"
echo "========================================"
echo ""

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration
SONAR_HOST="http://localhost:9000"
SONAR_TOKEN="squ_b1cc529e7fa62c4f0c6e5d5cc7d176f932c07d3e"
PROJECT_KEY="sonar-mulesoft-plugin"
PROJECT_NAME="SonarQube MuleSoft Plugin"
PROJECT_VERSION="1.1.0-SNAPSHOT"
SOURCE_PATH="$SCRIPT_DIR/src"

echo "Project: $PROJECT_NAME"
echo "Source Path: $SOURCE_PATH"
echo "SonarQube Server: $SONAR_HOST"
echo ""

# Check if source directory exists
if [ ! -d "$SOURCE_PATH" ]; then
    echo "ERROR: Source directory not found: $SOURCE_PATH"
    exit 1
fi

echo "Starting SonarQube analysis..."
echo ""

# Run the scanner
npx sonar-scanner \
  -Dsonar.host.url="$SONAR_HOST" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.projectKey="$PROJECT_KEY" \
  -Dsonar.projectName="$PROJECT_NAME" \
  -Dsonar.projectVersion="$PROJECT_VERSION" \
  -Dsonar.sources="$SOURCE_PATH" \
  -Dsonar.sourceEncoding=UTF-8 \
  -Dsonar.java.source=17 \
  -Dsonar.java.binaries="$SCRIPT_DIR/target/classes" \
  -Dsonar.scm.disabled=true \
  -Dsonar.verbose=true

echo ""
echo "========================================"
echo "Analysis Complete!"
echo "View results at: $SONAR_HOST/dashboard?id=$PROJECT_KEY"
echo "========================================"
