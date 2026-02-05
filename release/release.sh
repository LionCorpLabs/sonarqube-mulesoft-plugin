#!/bin/bash

# =============================================================================
# MuleSoft Plugin Release Script
# =============================================================================
# This script automates the release process for the SonarQube MuleSoft plugin.
#
# Usage:
#   RELEASE_VERSION=1.0.0 ./release.sh
#   RELEASE_VERSION=1.0.0 NEXT_VERSION=1.1.0 SKIP_TESTS=false ./release.sh
#
# Environment Variables:
#   RELEASE_VERSION  - The version to release (required, e.g., "1.0.0")
#   NEXT_VERSION     - Next development version (optional, auto-increments minor)
#   SKIP_TESTS       - Skip tests during build (default: false)
#   DRY_RUN          - Show what would happen without executing (default: false)
#   AUTO_PUSH        - Automatically push to remote without confirmation (default: false)
#   REMOTE_NAME      - Git remote name (default: "origin")
#   BRANCH_NAME      - Git branch name (default: "main")
# =============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
SKIP_TESTS=${SKIP_TESTS:-false}
DRY_RUN=${DRY_RUN:-false}
AUTO_PUSH=${AUTO_PUSH:-false}
REMOTE_NAME=${REMOTE_NAME:-origin}
BRANCH_NAME=${BRANCH_NAME:-main}

# =============================================================================
# Helper Functions
# =============================================================================

print_header() {
    echo -e "\n${BLUE}===================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}===================================================${NC}\n"
}

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

confirm() {
    local prompt="$1"
    local response
    read -p "$(echo -e ${YELLOW}${prompt}${NC}) [y/N]: " response
    case "$response" in
        [yY][eE][sS]|[yY])
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

execute_command() {
    local cmd="$1"
    local description="$2"

    if [ "$DRY_RUN" = "true" ]; then
        print_info "[DRY RUN] Would execute: $cmd"
        return 0
    fi

    print_info "$description"
    echo "  → $cmd"
    eval "$cmd"
}

increment_version() {
    local version=$1
    local major minor patch

    # Remove any -SNAPSHOT suffix
    version=${version%-SNAPSHOT}

    # Parse version components
    IFS='.' read -r major minor patch <<< "$version"

    # Increment minor version
    minor=$((minor + 1))

    echo "${major}.${minor}.${patch:-0}"
}

update_pom_version() {
    local new_version="$1"
    local pom_file="pom.xml"

    if [ "$DRY_RUN" = "true" ]; then
        print_info "[DRY RUN] Would update pom.xml version to: $new_version"
        return 0
    fi

    # Use sed to update version (works on both Linux and macOS)
    # Only replace the project version (after <artifactId>), not XML declaration or dependencies
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS - match version tag that follows artifactId
        sed -i '' '/<artifactId>/,/<version>/ s|<version>.*</version>|<version>'"${new_version}"'</version>|' "$pom_file"
    else
        # Linux - match version tag that follows artifactId
        sed -i '/<artifactId>/,/<version>/ s|<version>.*</version>|<version>'"${new_version}"'</version>|' "$pom_file"
    fi

    print_info "Updated pom.xml to version: $new_version"
}

get_current_version() {
    grep -m 1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | xargs
}

# =============================================================================
# Validation
# =============================================================================

print_header "MuleSoft Plugin Release Script"

# Check if RELEASE_VERSION is provided
if [ -z "$RELEASE_VERSION" ]; then
    print_error "RELEASE_VERSION environment variable is required!"
    echo ""
    echo "Usage examples:"
    echo "  RELEASE_VERSION=1.0.0 ./release.sh"
    echo "  RELEASE_VERSION=1.0.0 NEXT_VERSION=1.1.0 ./release.sh"
    echo "  DRY_RUN=true RELEASE_VERSION=1.0.0 ./release.sh"
    exit 1
fi

# Validate version format
if ! [[ "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid version format: $RELEASE_VERSION"
    print_error "Expected format: X.Y.Z (e.g., 1.0.0)"
    exit 1
fi

# Get current version
CURRENT_VERSION=$(get_current_version)
print_info "Current version: $CURRENT_VERSION"
print_info "Release version: $RELEASE_VERSION"

# Calculate next development version if not provided
if [ -z "$NEXT_VERSION" ]; then
    NEXT_VERSION=$(increment_version "$RELEASE_VERSION")
    print_info "Next version (auto-calculated): $NEXT_VERSION-SNAPSHOT"
else
    print_info "Next version: $NEXT_VERSION-SNAPSHOT"
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD -- 2>/dev/null; then
    print_warning "You have uncommitted changes!"
    git status --short
    if ! confirm "Continue anyway?"; then
        exit 1
    fi
fi

# Check if tag already exists
if git rev-parse "v$RELEASE_VERSION" >/dev/null 2>&1; then
    print_error "Tag v$RELEASE_VERSION already exists!"
    exit 1
fi

# Display summary
print_header "Release Summary"
echo "  Release Version:     $RELEASE_VERSION"
echo "  Next Dev Version:    $NEXT_VERSION-SNAPSHOT"
echo "  Skip Tests:          $SKIP_TESTS"
echo "  Dry Run:             $DRY_RUN"
echo "  Auto Push:           $AUTO_PUSH"
echo "  Remote:              $REMOTE_NAME"
echo "  Branch:              $BRANCH_NAME"
echo ""

if [ "$DRY_RUN" != "true" ]; then
    if ! confirm "Proceed with release?"; then
        print_warning "Release cancelled"
        exit 0
    fi
fi

# =============================================================================
# Step 1: Update to Release Version
# =============================================================================

print_header "Step 1: Update to Release Version"
update_pom_version "$RELEASE_VERSION"

# =============================================================================
# Step 2: Build and Test
# =============================================================================

print_header "Step 2: Build and Test"

if [ "$SKIP_TESTS" = "true" ]; then
    execute_command "mvn clean package -DskipTests" "Building without tests"
else
    execute_command "mvn clean test" "Running tests"
    execute_command "mvn clean package" "Building release artifact"
fi

if [ "$DRY_RUN" != "true" ]; then
    ARTIFACT_PATH="target/sonar-mulesoft-plugin-${RELEASE_VERSION}-FINAL.jar"
    if [ -f "$ARTIFACT_PATH" ]; then
        print_info "Release artifact created: $ARTIFACT_PATH"
        ls -lh "$ARTIFACT_PATH"
    else
        print_error "Failed to create release artifact!"
        exit 1
    fi
fi

# =============================================================================
# Step 3: Commit Release Version
# =============================================================================

print_header "Step 3: Commit Release Version"
execute_command "git add pom.xml" "Staging pom.xml"
execute_command "git commit -m 'Release version $RELEASE_VERSION'" "Committing release version"

# =============================================================================
# Step 4: Create Git Tag
# =============================================================================

print_header "Step 4: Create Git Tag"
execute_command "git tag -a v$RELEASE_VERSION -m 'Release version $RELEASE_VERSION'" "Creating tag v$RELEASE_VERSION"

# =============================================================================
# Step 5: Update to Next Development Version
# =============================================================================

print_header "Step 5: Update to Next Development Version"
update_pom_version "$NEXT_VERSION-SNAPSHOT"
execute_command "git add pom.xml" "Staging pom.xml"
execute_command "git commit -m 'Prepare for next development iteration $NEXT_VERSION-SNAPSHOT'" "Committing next dev version"

# =============================================================================
# Step 6: Push to Remote
# =============================================================================

print_header "Step 6: Push to Remote"

if [ "$DRY_RUN" = "true" ]; then
    print_info "[DRY RUN] Would push:"
    print_info "  git push $REMOTE_NAME $BRANCH_NAME"
    print_info "  git push $REMOTE_NAME v$RELEASE_VERSION"
else
    if [ "$AUTO_PUSH" = "true" ] || confirm "Push commits and tag to $REMOTE_NAME?"; then
        execute_command "git push $REMOTE_NAME $BRANCH_NAME" "Pushing commits"
        execute_command "git push $REMOTE_NAME v$RELEASE_VERSION" "Pushing tag"
    else
        print_warning "Skipping push. You can push manually later with:"
        echo "  git push $REMOTE_NAME $BRANCH_NAME"
        echo "  git push $REMOTE_NAME v$RELEASE_VERSION"
    fi
fi

# =============================================================================
# Complete
# =============================================================================

print_header "Release Complete! 🎉"
echo "  Version Released:    v$RELEASE_VERSION"
echo "  Next Dev Version:    $NEXT_VERSION-SNAPSHOT"
if [ "$DRY_RUN" != "true" ]; then
    echo "  Release Artifact:    target/sonar-mulesoft-plugin-${RELEASE_VERSION}-FINAL.jar"
fi
echo ""
print_info "To create a GitHub release, visit:"
echo "  https://github.com/YOUR_ORG/YOUR_REPO/releases/new?tag=v$RELEASE_VERSION"
echo ""
