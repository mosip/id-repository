#!/usr/bin/env python3
"""Remove duplicate/generic field JavaDoc in identity package."""
import re
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ROOTS = [
    BASE / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "identity",
    BASE / "id-repository-service" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "identity",
]

GENERIC = re.compile(r"^\s*/\*\* [A-Za-z][a-zA-Z ]+\. \*/\s*$")


def dedupe(content: str) -> str:
    lines = content.splitlines()
    out = []
    last_javadoc = None
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        if stripped.startswith("/**") and stripped.endswith("*/"):
            if stripped == last_javadoc:
                i += 1
                continue
            # generic javadoc right before private/protected field — drop if recent detailed doc exists
            if GENERIC.match(line) and i + 1 < len(lines):
                nxt = lines[i + 1]
                if re.match(r"^\s*(private|protected)\s+", nxt):
                    # look back for a longer javadoc in prior 8 lines
                    for k in range(len(out) - 1, max(len(out) - 9, -1), -1):
                        prev = out[k].strip()
                        if prev.startswith("/**") and len(prev) > 40:
                            i += 1
                            line = None
                            break
            if line is None:
                continue
            last_javadoc = stripped
            out.append(line)
            i += 1
            continue
        if not stripped.startswith("@"):
            last_javadoc = None
        out.append(line)
        i += 1
    return "\n".join(out) + ("\n" if content.endswith("\n") else "")


def main():
    n = 0
    for root in ROOTS:
        for path in root.rglob("*.java"):
            old = path.read_text(encoding="utf-8")
            new = dedupe(old)
            if new != old:
                path.write_text(new, encoding="utf-8", newline="\n")
                n += 1
                print(path.relative_to(BASE))
    print(f"deduped: {n}")


if __name__ == "__main__":
    main()
