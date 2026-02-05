#!/usr/bin/env python3
"""
Script to fix Document mocking issues in test files.
Replaces mock(Document.class) with TestXmlHelper methods.
"""

import os
import re
from pathlib import Path

def fix_test_file(file_path):
    """Fix a single test file by replacing Document mocks with TestXmlHelper."""
    with open(file_path, 'r') as f:
        content = f.read()

    original_content = content
    modified = False

    # Replace mock(Document.class) with TestXmlHelper.createEmptyDocument()
    if 'mock(Document.class)' in content:
        content = content.replace('mock(Document.class)', 'TestXmlHelper.createEmptyDocument()')
        modified = True

    # Add import for TestXmlHelper if we made changes and it's not already imported
    if modified and 'import com.lioncorp.sonar.mulesoft.TestXmlHelper;' not in content:
        # Find the last import statement
        import_pattern = r'(import [^;]+;)\n'
        imports = list(re.finditer(import_pattern, content))
        if imports:
            last_import = imports[-1]
            insert_pos = last_import.end()
            content = (content[:insert_pos] +
                      'import com.lioncorp.sonar.mulesoft.TestXmlHelper;\n' +
                      content[insert_pos:])

    # Remove unused mock import if Document.class was the only thing being mocked
    # Only remove if there are no other mock() calls in the file
    if modified:
        # Check if there are any other mock() calls
        remaining_mocks = re.findall(r'\bmock\([^)]+\)', content)
        if not remaining_mocks:
            # Remove the mock import
            content = re.sub(r'import static org\.mockito\.Mockito\.mock;\n?', '', content)

    if content != original_content:
        with open(file_path, 'w') as f:
            f.write(content)
        return True

    return False

def main():
    # Get the project root directory
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    test_dir = project_root / 'src' / 'test' / 'java'

    # Find all test files
    test_files = list(test_dir.rglob('*Test.java'))

    fixed_count = 0
    for test_file in test_files:
        if fix_test_file(test_file):
            fixed_count += 1
            print(f"Fixed: {test_file.relative_to(project_root)}")

    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
