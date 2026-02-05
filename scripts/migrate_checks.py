#!/usr/bin/env python3
"""
Script to help migrate MuleSoft check classes to use BaseCheck.
This script performs the basic structural changes needed for migration.
"""

import os
import re
import sys

def migrate_check_file(filepath):
    """Migrate a single check file to use BaseCheck."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        original_content = content

        # Extract the rule key from @Rule annotation
        rule_match = re.search(r'@Rule\(key\s*=\s*"([^"]+)"\)', content)
        if not rule_match:
            print(f"  ⚠️  Could not find @Rule annotation in {filepath}")
            return False

        rule_key = rule_match.group(1)

        # Check if already migrated
        if 'extends BaseCheck' in content:
            print(f"  ✓ Already migrated: {os.path.basename(filepath)}")
            return True

        # Step 1: Change implements MuleSoftCheck to extends BaseCheck
        content = re.sub(
            r'implements MuleSoftCheck',
            'extends BaseCheck',
            content
        )

        # Step 2: Remove REPOSITORY_KEY and RULE_KEY constants
        content = re.sub(
            r'\n\s+private static final String REPOSITORY_KEY\s*=\s*"[^"]+";?\n',
            '\n',
            content
        )
        content = re.sub(
            r'\n\s+private static final String RULE_KEY\s*=\s*"[^"]+";?\n',
            '\n',
            content
        )

        # Step 3: Remove reportIssue methods
        # Remove the complete reportIssue method(s)
        content = re.sub(
            r'\n\s+private void reportIssue\([^)]+\)\s*\{[^}]*\n\s+NewIssue issue[^}]+\}\n',
            '\n',
            content,
            flags=re.DOTALL
        )

        # Step 4: Add getRuleKey() method after class declaration
        # Find the position after class declaration
        class_match = re.search(r'(public class \w+ extends BaseCheck \{)', content)
        if class_match:
            insert_pos = class_match.end()
            get_rule_key_method = f'\n\n  @Override\n  protected String getRuleKey() {{\n    return "{rule_key}";\n  }}'
            content = content[:insert_pos] + get_rule_key_method + content[insert_pos:]

        # Step 5: Update imports
        # Remove old imports
        content = re.sub(
            r'import com\.lioncorp\.sonar\.mulesoft\.MuleSoftCheck;\n',
            '',
            content
        )
        content = re.sub(
            r'import org\.sonar\.api\.batch\.sensor\.issue\.NewIssue;\n',
            '',
            content
        )
        content = re.sub(
            r'import org\.sonar\.api\.batch\.sensor\.issue\.NewIssueLocation;\n',
            '',
            content
        )
        content = re.sub(
            r'import org\.sonar\.api\.rule\.RuleKey;\n',
            '',
            content
        )

        # Add BaseCheck import if not present
        if 'import com.lioncorp.sonar.mulesoft.checks.BaseCheck;' not in content:
            # Find the package line and insert after imports section
            import_match = re.search(r'(package [^;]+;\n)', content)
            if import_match:
                insert_pos = import_match.end()
                # Find where to insert (after existing imports or after package)
                next_import = re.search(r'\nimport ', content[insert_pos:])
                if next_import:
                    # Insert with other imports
                    content = content[:insert_pos] + '\nimport com.lioncorp.sonar.mulesoft.checks.BaseCheck;' + content[insert_pos:]
                else:
                    # No imports yet, add after package
                    content = content[:insert_pos] + '\nimport com.lioncorp.sonar.mulesoft.checks.BaseCheck;\nimport com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;\nimport org.sonar.api.batch.fs.InputFile;\nimport org.sonar.api.batch.sensor.SensorContext;\nimport org.sonar.check.Rule;\n' + content[insert_pos:]

        # Only write if content changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"  ✓ Migrated: {os.path.basename(filepath)}")
            return True
        else:
            print(f"  - No changes: {os.path.basename(filepath)}")
            return False

    except Exception as e:
        print(f"  ✗ Error migrating {filepath}: {e}")
        return False

def migrate_directory(directory):
    """Migrate all check files in a directory."""
    if not os.path.exists(directory):
        print(f"Directory not found: {directory}")
        return

    print(f"\nMigrating checks in: {directory}")
    print("=" * 60)

    migrated = 0
    failed = 0

    for filename in sorted(os.listdir(directory)):
        if filename.endswith('Check.java'):
            filepath = os.path.join(directory, filename)
            if migrate_check_file(filepath):
                migrated += 1
            else:
                failed += 1

    print(f"\nSummary: {migrated} migrated, {failed} failed/skipped")

def main():
    base_path = '/Users/joshuaquek/Desktop/lioncorp-mulesoft-plugin/src/main/java/com/lioncorp/sonar/mulesoft/checks'

    directories = [
        'security',
        'structure',
        'naming',
        'performance',
        'errorhandling',
        'java'
    ]

    if len(sys.argv) > 1:
        # Migrate specific directory
        dir_name = sys.argv[1]
        migrate_directory(os.path.join(base_path, dir_name))
    else:
        # Migrate all directories
        for dir_name in directories:
            migrate_directory(os.path.join(base_path, dir_name))

        # Also migrate root-level checks
        print(f"\nMigrating root-level checks in: {base_path}")
        print("=" * 60)
        for filename in ['EmptyFlowCheck.java', 'MissingFlowNameCheck.java', 'HardcodedCredentialsCheck.java', 'LargeFlowCheck.java', 'JavaClassSecurityCheck.java']:
            filepath = os.path.join(base_path, filename)
            if os.path.exists(filepath):
                migrate_check_file(filepath)

if __name__ == '__main__':
    main()
