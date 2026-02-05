# Release Guide

## Quick Start

### Basic Release
```bash
RELEASE_VERSION=1.0.0 ./release.sh
```

### Full Automated Release (no prompts)
```bash
RELEASE_VERSION=1.0.0 AUTO_PUSH=true ./release.sh
```

### Dry Run (see what would happen)
```bash
DRY_RUN=true RELEASE_VERSION=1.0.0 ./release.sh
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `RELEASE_VERSION` | ✅ Yes | - | Version to release (e.g., "1.0.0") |
| `NEXT_VERSION` | No | Auto-increment minor | Next development version (e.g., "1.1.0") |
| `SKIP_TESTS` | No | `false` | Skip tests during build |
| `DRY_RUN` | No | `false` | Show what would happen without executing |
| `AUTO_PUSH` | No | `false` | Push to remote without confirmation |
| `REMOTE_NAME` | No | `origin` | Git remote name |
| `BRANCH_NAME` | No | `main` | Git branch name |

## Common Scenarios

### 1. First Release (1.0.0)
```bash
RELEASE_VERSION=1.0.0 ./release.sh
```

### 2. Minor Version Update (1.0.0 → 1.1.0)
```bash
RELEASE_VERSION=1.1.0 ./release.sh
```

### 3. Patch Release (1.0.0 → 1.0.1)
```bash
RELEASE_VERSION=1.0.1 NEXT_VERSION=1.1.0 ./release.sh
```

### 4. Major Version Update (1.x.x → 2.0.0)
```bash
RELEASE_VERSION=2.0.0 ./release.sh
```

### 5. Fast Release (skip tests, auto-push)
```bash
RELEASE_VERSION=1.0.0 SKIP_TESTS=true AUTO_PUSH=true ./release.sh
```

### 6. Test the Release Process (dry run)
```bash
DRY_RUN=true RELEASE_VERSION=1.0.0 ./release.sh
```

### 7. Custom Next Development Version
```bash
RELEASE_VERSION=1.0.0 NEXT_VERSION=2.0.0 ./release.sh
```

### 8. Release to Different Remote/Branch
```bash
RELEASE_VERSION=1.0.0 REMOTE_NAME=upstream BRANCH_NAME=develop ./release.sh
```

## What the Script Does

The script automates these steps:

1. **Validation**
   - Checks if `RELEASE_VERSION` is provided and valid
   - Verifies no uncommitted changes (with confirmation)
   - Ensures tag doesn't already exist

2. **Update to Release Version**
   - Updates `pom.xml` with release version (removes `-SNAPSHOT`)

3. **Build and Test**
   - Runs `mvn clean test` (unless `SKIP_TESTS=true`)
   - Creates release artifact: `target/sonar-mulesoft-plugin-X.Y.Z-FINAL.jar`

4. **Commit Release Version**
   - Commits: `"Release version X.Y.Z"`

5. **Create Git Tag**
   - Creates annotated tag: `vX.Y.Z`

6. **Update to Next Development Version**
   - Updates `pom.xml` with next version (adds `-SNAPSHOT`)
   - Commits: `"Prepare for next development iteration X.Y.Z-SNAPSHOT"`

7. **Push to Remote**
   - Pushes commits and tag to remote (with confirmation unless `AUTO_PUSH=true`)

## Manual Release (Without Script)

If you prefer to do it manually:

```bash
# 1. Update version
# Edit pom.xml line 9: <version>1.0.0</version>

# 2. Build and test
mvn clean package

# 3. Commit and tag
git add pom.xml
git commit -m "Release version 1.0.0"
git tag -a v1.0.0 -m "Release version 1.0.0"

# 4. Update to next dev version
# Edit pom.xml line 9: <version>1.1.0-SNAPSHOT</version>
git add pom.xml
git commit -m "Prepare for next development iteration"

# 5. Push
git push origin main
git push origin v1.0.0
```

## Versioning Strategy

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** version (X.0.0): Breaking changes
- **MINOR** version (1.X.0): New features, backward compatible
- **PATCH** version (1.0.X): Bug fixes, backward compatible

Examples:
- `1.0.0` → `1.0.1`: Bug fix
- `1.0.0` → `1.1.0`: New rule added
- `1.0.0` → `2.0.0`: Breaking API change

## Troubleshooting

### Tag Already Exists
```bash
# Delete local tag
git tag -d v1.0.0

# Delete remote tag (careful!)
git push origin :refs/tags/v1.0.0
```

### Rollback Release (before push)
```bash
# Reset to before release commits
git reset --hard HEAD~2

# Delete tag
git tag -d v1.0.0
```

### Rollback Release (after push)
```bash
# This is difficult - create a new patch release instead
RELEASE_VERSION=1.0.1 ./release.sh
```

## Creating GitHub Release

After running the script, create a GitHub release:

1. Go to: `https://github.com/YOUR_ORG/YOUR_REPO/releases/new?tag=vX.Y.Z`
2. Add release notes
3. Attach `target/sonar-mulesoft-plugin-X.Y.Z-FINAL.jar`
4. Publish release

## CI/CD Integration

For automated releases in CI/CD:

```bash
# .github/workflows/release.yml
- name: Release
  env:
    RELEASE_VERSION: ${{ github.event.inputs.version }}
    AUTO_PUSH: true
    SKIP_TESTS: false
  run: ./release.sh
```
