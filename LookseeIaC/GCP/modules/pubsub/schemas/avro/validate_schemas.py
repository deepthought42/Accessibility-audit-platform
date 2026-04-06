#!/usr/bin/env python3
"""
Validates all Avro schema files (.avsc) in the schemas/avro/ directory.

Checks:
1. Valid JSON syntax
2. Required Avro fields (type, name, namespace, fields)
3. Version metadata present
4. All fields have name, type, and doc
5. Common base fields (messageId, publishTime, accountId) present
"""
import json
import glob
import sys
import os

REQUIRED_BASE_FIELDS = {"messageId", "publishTime", "accountId"}
SCHEMA_DIR = os.path.dirname(os.path.abspath(__file__))


def validate_schema(filepath):
    errors = []
    filename = os.path.basename(filepath)

    try:
        with open(filepath) as f:
            schema = json.load(f)
    except json.JSONDecodeError as e:
        return [f"{filename}: Invalid JSON - {e}"]

    # Check required top-level fields
    for required in ["type", "name", "namespace", "fields", "version", "doc"]:
        if required not in schema:
            errors.append(f"{filename}: Missing required field '{required}'")

    if schema.get("type") != "record":
        errors.append(f"{filename}: type must be 'record', got '{schema.get('type')}'")

    if schema.get("namespace") != "com.looksee.models.message":
        errors.append(f"{filename}: namespace should be 'com.looksee.models.message'")

    # Validate version format
    version = schema.get("version", "")
    if not version:
        errors.append(f"{filename}: version is empty")
    elif not all(part.isdigit() for part in version.split(".")):
        errors.append(f"{filename}: version '{version}' is not semver format")

    # Validate fields
    fields = schema.get("fields", [])
    field_names = set()
    for field in fields:
        if "name" not in field:
            errors.append(f"{filename}: field missing 'name'")
            continue
        field_names.add(field["name"])
        if "type" not in field:
            errors.append(f"{filename}: field '{field['name']}' missing 'type'")
        if "doc" not in field:
            errors.append(f"{filename}: field '{field['name']}' missing 'doc'")

    # Check base fields present
    missing_base = REQUIRED_BASE_FIELDS - field_names
    if missing_base:
        errors.append(f"{filename}: missing base fields: {missing_base}")

    return errors


def main():
    schema_files = sorted(glob.glob(os.path.join(SCHEMA_DIR, "*.avsc")))
    if not schema_files:
        print("ERROR: No .avsc files found")
        sys.exit(1)

    total_errors = []
    for filepath in schema_files:
        errors = validate_schema(filepath)
        if errors:
            total_errors.extend(errors)
        else:
            print(f"  OK: {os.path.basename(filepath)}")

    print(f"\nValidated {len(schema_files)} schemas")
    if total_errors:
        print(f"\n{len(total_errors)} errors found:")
        for error in total_errors:
            print(f"  FAIL: {error}")
        sys.exit(1)
    else:
        print("All schemas valid!")
        sys.exit(0)


if __name__ == "__main__":
    main()
