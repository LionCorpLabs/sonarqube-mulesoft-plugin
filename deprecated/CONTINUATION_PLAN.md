# Continuation Plan: Deploy & Test on SonarQube

## Current State

### ✅ Completed in This Session
- **Foundation Classes**: Created 5 new utility/base classes
- **Core Refactoring**: Simplified 4 core plugin files
- **Check Migration**: Migrated all 115 check classes to use BaseCheck
- **Compilation**: ✅ BUILD SUCCESS - All 131 files compile
- **Unit Tests**: ✅ All tests pass
- **Code Reduction**: ~2,878 lines eliminated (15% reduction)

### 📦 Build Output
- Plugin JAR files should be in: `target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar`
- This is the shaded JAR with all dependencies included

---

## Next Session: Deployment & Testing

### Phase 1: Build the Plugin

**Objective**: Create the final deployable JAR file

```bash
# Full clean build with tests
./scripts/build.sh

# OR: Maven directly
mvn clean package

# Verify JAR files created
ls -lh target/*.jar
```

**Expected Output**:
- `sonar-mulesoft-plugin-1.0.0-SNAPSHOT-RAW.jar` (without dependencies)
- `sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar` (with dependencies - **USE THIS**)

**Success Criteria**:
- ✅ Both JAR files exist
- ✅ FINAL JAR is larger (contains dependencies)
- ✅ No build errors
- ✅ All tests pass

---

### Phase 2: Deploy to SonarQube

**Objective**: Install the plugin in a test SonarQube instance

#### Prerequisites
1. **SonarQube Server** (version 9.9+) installed and running
2. Access to `$SONARQUBE_HOME/extensions/plugins/` directory
3. Admin access to SonarQube web interface

#### Deployment Steps

```bash
# 1. Stop SonarQube (if running)
$SONARQUBE_HOME/bin/[your-os]/sonar.sh stop

# 2. Remove old plugin version (if exists)
rm $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin*.jar

# 3. Copy new plugin
cp target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar \
   $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar

# 4. Verify file permissions
ls -l $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar

# 5. Start SonarQube
$SONARQUBE_HOME/bin/[your-os]/sonar.sh start

# 6. Monitor startup logs
tail -f $SONARQUBE_HOME/logs/sonar.log
```

**Success Criteria**:
- ✅ SonarQube starts without errors
- ✅ No ClassNotFoundException in logs
- ✅ No plugin loading errors

---

### Phase 3: Verify Plugin Installation

**Objective**: Confirm the plugin loaded correctly in SonarQube

#### Web Interface Verification

1. **Navigate to**: `http://localhost:9000` (or your SonarQube URL)

2. **Administration → Marketplace → Installed**
   - ✅ "MuleSoft" plugin should be listed
   - ✅ Version: 1.0.0-SNAPSHOT
   - ✅ Status: Should show as active/loaded

3. **Quality Profiles → MuleSoft Way**
   - ✅ Profile should exist
   - ✅ Should be marked as "Default"
   - ✅ Should show **110 active rules**

4. **Rules → MuleSoft**
   - ✅ Repository "mulesoft" should exist
   - ✅ Should list all 110 rules (MS001-MS110)
   - ✅ Click a few rules to verify descriptions load

**Success Criteria**:
- ✅ Plugin appears in Marketplace
- ✅ All 110 rules are registered
- ✅ Default quality profile exists with all rules activated
- ✅ Rule descriptions display correctly

---

### Phase 4: Test with Sample Files

**Objective**: Verify the plugin analyzes MuleSoft files correctly

#### Prepare Test Project

```bash
# Create test project directory
mkdir -p ~/sonarqube-test/mulesoft-sample
cd ~/sonarqube-test/mulesoft-sample

# Copy example files
cp /Users/joshuaquek/Desktop/lioncorp-mulesoft-plugin/examples/*.xml .

# Create sonar-project.properties
cat > sonar-project.properties << 'EOF'
sonar.projectKey=mulesoft-test
sonar.projectName=MuleSoft Test Project
sonar.projectVersion=1.0
sonar.sources=.
sonar.language=mulesoft
sonar.sourceEncoding=UTF-8
EOF
```

#### Run Analysis

```bash
# Run SonarQube scanner
sonar-scanner

# OR: If using Maven project
mvn sonar:sonar
```

**Monitor for**:
- Scanner detects MuleSoft files
- "MuleSoft Sensor" appears in logs
- Rules are executed
- Issues are reported

#### Verify Results in SonarQube UI

1. **Navigate to**: Projects → mulesoft-test

2. **Check Overview**:
   - ✅ Project analyzed successfully
   - ✅ Files were scanned (count > 0)
   - ✅ Issues found (should find violations in examples/violations-example.xml)

3. **Issues Tab**:
   - ✅ Browse issues found
   - ✅ Verify issue messages are correct
   - ✅ Check issue locations (file + line numbers)
   - ✅ Confirm severity levels (Blocker, Critical, Major, etc.)

4. **Code Tab**:
   - ✅ Select a MuleSoft XML file
   - ✅ Issues should be highlighted in source view
   - ✅ Click on issue to see details

**Success Criteria**:
- ✅ Files are scanned and analyzed
- ✅ Issues are detected in violations-example.xml
- ✅ No issues in compliant-example.xml
- ✅ Issue messages are meaningful and accurate
- ✅ Line numbers are correct

---

### Phase 5: Validation Checklist

**Test Each Rule Category**:

Create test files to verify each category of rules:

#### Security Rules (MS001-MS030)
```bash
# Test file should trigger security issues
# Examples: hardcoded credentials, insecure HTTP, SQL injection patterns
```
- ✅ MS001: Hardcoded credentials detected
- ✅ MS002: HTTP endpoint flagged
- ✅ MS003: SQL injection pattern detected
- ✅ MS018: Sensitive data logging detected

#### Structure Rules (MS031-MS058)
```bash
# Test file with structural issues
# Examples: large flows, deep nesting, missing error handlers
```
- ✅ MS032: Large flow detected (>15 components)
- ✅ MS034: Deep nesting detected (>3 levels)
- ✅ MS037: Missing error handler flagged

#### Performance Rules (MS072-MS084)
```bash
# Test file with performance issues
# Examples: DB queries in loops, missing async processing
```
- ✅ MS078: Database query in loop detected

#### All Categories
- ✅ Naming rules (MS059-MS071)
- ✅ Error handling rules (MS085-MS096)
- ✅ Java integration rules (MS097-MS110)

---

### Phase 6: Regression Testing

**Objective**: Ensure refactored code behaves identically to original

#### Compare with Previous Version (If Available)

1. **Analyze same project with old plugin** (if you have it)
2. **Analyze same project with new plugin**
3. **Compare results**:
   - Same number of issues?
   - Same issue types?
   - Same locations?

#### Baseline Test
```bash
# Run against known codebase
# Document: number of issues, types, locations

# After fixes/changes
# Re-run and compare - should be identical
```

---

### Phase 7: Performance Testing

**Objective**: Ensure refactoring didn't impact performance

```bash
# Time the analysis
time sonar-scanner

# Check SonarQube logs for:
# - "MuleSoft Sensor starting. Loaded X checks."
# - "MuleSoft Sensor finished. Scanned X MuleSoft files."
# - Total execution time
```

**Expected**:
- Performance should be similar or better (less code = potentially faster)
- No significant slowdown
- Memory usage should be comparable

---

## Troubleshooting Guide

### Issue: Plugin Not Appearing in Marketplace

**Symptoms**: Plugin doesn't show up in SonarQube UI

**Solutions**:
1. Check SonarQube logs: `tail -f $SONARQUBE_HOME/logs/sonar.log`
2. Look for plugin loading errors
3. Verify JAR file permissions (must be readable)
4. Confirm using FINAL jar (not RAW jar)
5. Check SonarQube version compatibility (need 9.9+)

### Issue: ClassNotFoundException

**Symptoms**: Errors in logs about missing classes

**Cause**: Using RAW jar instead of FINAL jar

**Solution**:
```bash
# Remove wrong JAR
rm $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar

# Install FINAL JAR (with dependencies)
cp target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar \
   $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar

# Restart SonarQube
```

### Issue: Rules Not Registered

**Symptoms**: Plugin loads but no rules appear

**Check**:
1. Verify CheckList.getChecks() returns all 115+ classes
2. Check MuleSoftRulesDefinition has all 110 rules
3. Look for exceptions during rule registration in logs

### Issue: Files Not Analyzed

**Symptoms**: Scanner runs but no files scanned

**Solutions**:
1. Verify `sonar.language=mulesoft` in sonar-project.properties
2. Check file extensions are `.xml`
3. Confirm files contain MuleSoft namespaces
4. Check `sonar.sources` points to correct directory

### Issue: Wrong Issues Detected

**Symptoms**: Issues in wrong places or wrong types

**Debug**:
1. Check specific rule implementation
2. Verify reportIssue() calls use correct line numbers
3. Test with simpler XML file to isolate issue
4. Compare with pre-refactoring behavior (if possible)

---

## Rollback Plan

If critical issues are found:

### Option 1: Revert to Previous Version
```bash
# Stop SonarQube
$SONARQUBE_HOME/bin/[your-os]/sonar.sh stop

# Restore old plugin (if backed up)
cp sonar-mulesoft-plugin-OLD.jar \
   $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar

# Start SonarQube
$SONARQUBE_HOME/bin/[your-os]/sonar.sh start
```

### Option 2: Git Revert
```bash
cd /Users/joshuaquek/Desktop/lioncorp-mulesoft-plugin

# Find commit before refactoring
git log --oneline

# Revert to specific commit
git revert <commit-hash>

# Rebuild
mvn clean package

# Redeploy
```

---

## Success Metrics

### Must Have ✅
- [ ] Plugin loads without errors
- [ ] All 110 rules are registered
- [ ] Files are scanned successfully
- [ ] Issues are detected and reported
- [ ] Issue locations (line numbers) are accurate
- [ ] No false positives compared to previous version

### Should Have 📊
- [ ] Performance is comparable or better
- [ ] No memory issues
- [ ] All rule categories work (6 categories tested)
- [ ] UI displays issues correctly
- [ ] Issue descriptions are clear and helpful

### Nice to Have 🎯
- [ ] Faster analysis than before
- [ ] Cleaner error messages
- [ ] Better logging output

---

## Post-Validation Steps

Once testing is successful:

### 1. Documentation
- [ ] Update README.md with any deployment notes
- [ ] Document any issues found and fixed
- [ ] Add troubleshooting tips to docs

### 2. Version Control
```bash
# Commit successful deployment
git add .
git commit -m "test: Verify refactored plugin works in SonarQube

- Deployed to SonarQube 9.9+
- All 110 rules functioning correctly
- No regressions detected
- Performance equivalent to pre-refactoring"

git push
```

### 3. Tag Release
```bash
# Tag this version
git tag -a v1.0.0-refactored -m "Simplified codebase - 2,878 LOC reduced"
git push --tags
```

### 4. Share Results
- Create summary report of testing
- Document any edge cases discovered
- Share performance metrics

---

## Additional Testing Ideas

### Extended Testing
1. **Large Projects**: Test with real MuleSoft projects (100+ files)
2. **Complex Flows**: Test with deeply nested flows, large configurations
3. **Edge Cases**: Empty files, malformed XML, mixed content
4. **Concurrent Analysis**: Multiple projects analyzing simultaneously

### Compatibility Testing
- Different SonarQube versions (9.9, 10.0, 10.x)
- Different operating systems (Linux, macOS, Windows)
- Different Java versions (17, 21)

---

## Notes for Next Session

### Commands Quick Reference
```bash
# Build
mvn clean package

# Deploy
cp target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar \
   $SONARQUBE_HOME/extensions/plugins/sonar-mulesoft-plugin.jar

# Start/Stop SonarQube
$SONARQUBE_HOME/bin/[your-os]/sonar.sh start
$SONARQUBE_HOME/bin/[your-os]/sonar.sh stop

# View logs
tail -f $SONARQUBE_HOME/logs/sonar.log

# Run analysis
sonar-scanner
```

### Key Files to Check
- Plugin JAR: `target/sonar-mulesoft-plugin-1.0.0-SNAPSHOT-FINAL.jar`
- Test files: `examples/violations-example.xml`, `examples/compliant-example.xml`
- SonarQube logs: `$SONARQUBE_HOME/logs/sonar.log`
- This summary: `SIMPLIFICATION_SUMMARY.md`

### Environment Variables Needed
```bash
export SONARQUBE_HOME=/path/to/sonarqube
export SONAR_SCANNER_HOME=/path/to/sonar-scanner
export PATH=$PATH:$SONAR_SCANNER_HOME/bin
```

---

## Expected Timeline

- **Phase 1** (Build): 2-3 minutes
- **Phase 2** (Deploy): 5-10 minutes
- **Phase 3** (Verify): 5-10 minutes
- **Phase 4** (Test): 10-15 minutes
- **Phase 5** (Validation): 15-30 minutes
- **Phase 6** (Regression): 15-20 minutes
- **Phase 7** (Performance): 5-10 minutes

**Total**: ~1-2 hours for complete validation

---

## Conclusion

This continuation plan provides a comprehensive roadmap for deploying and testing the refactored MuleSoft plugin. Follow each phase systematically to ensure the simplification didn't break any functionality.

**Remember**: The goal is to verify that all 110 rules work identically to before, just with cleaner code!

---

**Created**: 2026-02-05
**Project**: SonarQube MuleSoft Plugin v1.0.0-SNAPSHOT
**Status**: Ready for deployment testing
