#!/usr/bin/env python3
"""Build WireMock idschema/latest response from schema + optional dev UI export."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
FILES = HERE / "__files"
DEFAULT_SCHEMA = FILES / "id-schema-01.json"
DEFAULT_UI_SPEC = FILES / "id-schema-ui-spec.json"
DEFAULT_OUT = FILES / "masterdata-idschema-latest-response.json"


def load_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def compact_schema_json(schema_path: Path) -> str:
    schema = load_json(schema_path)
    return json.dumps(schema, separators=(",", ":"))


def build_response(schema_path: Path, ui_spec_path: Path | None) -> dict:
    schema_json = compact_schema_json(schema_path)
    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"

    response: dict = {
        "updatedBy": None,
        "idVersion": 0.1,
        "description": "Mosip Sample identity",
        "updatedOn": None,
        "title": "Mosip Identity",
        "createdOn": "2026-07-23T19:28:21.245Z",
        "createdBy": "admin",
        "schemaJson": schema_json,
        "id": "1001",
        "effectiveFrom": "2026-07-23T19:28:21.257Z",
        "status": "PUBLISHED",
    }

    if ui_spec_path and ui_spec_path.is_file():
        ui = load_json(ui_spec_path)
        for key in (
            "settings",
            "updateProcess",
            "newProcess",
            "lostProcess",
            "bioCorrectionProcess",
        ):
            if key in ui:
                response[key] = ui[key]
    else:
        response["settings"] = []

    return {
        "id": None,
        "version": None,
        "responsetime": now,
        "metadata": None,
        "response": response,
        "errors": None,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    parser.add_argument("--ui-spec", type=Path, default=DEFAULT_UI_SPEC)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument(
        "--from-dev-export",
        type=Path,
        help="Full dev GET /idschema/latest JSON; extracts UI spec keys into --ui-spec",
    )
    args = parser.parse_args()

    if args.from_dev_export:
        export = load_json(args.from_dev_export)
        ui_keys = (
            "settings",
            "updateProcess",
            "newProcess",
            "lostProcess",
            "bioCorrectionProcess",
        )
        ui_spec = {k: export["response"][k] for k in ui_keys if k in export.get("response", {})}
        args.ui_spec.parent.mkdir(parents=True, exist_ok=True)
        with args.ui_spec.open("w", encoding="utf-8") as handle:
            json.dump(ui_spec, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
        print(f"Wrote UI spec -> {args.ui_spec}")

    payload = build_response(args.schema, args.ui_spec)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(f"Wrote WireMock body -> {args.out}")


if __name__ == "__main__":
    main()
