#!/usr/bin/env python3
"""Remove erroneous in-method JavaDoc blocks inserted by add-credential-member-javadoc.py."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "credential"


def cleanup(content: str) -> str:
    lines = content.splitlines()
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Remove javadoc with 2+ leading tabs (inside methods)
        if re.match(r"^\t\t+/\*\*.*\*/\s*$", line):
            i += 1
            continue
        # Remove duplicate consecutive field javadocs (keep last before field/annotation)
        if re.match(r"^\t/\*\*.*\*/\s*$", line) and i + 1 < len(lines):
            nxt = lines[i + 1].strip()
            if nxt.startswith("/**") or (nxt.startswith("@") and i + 2 < len(lines) and lines[i + 2].strip().startswith("/**")):
                i += 1
                continue
        # Fix @Autowired followed by javadoc then field -> javadoc then @Autowired then field
        if line.strip().startswith("@Autowired") and i + 1 < len(lines):
            nxt = lines[i + 1]
            if re.match(r"^\s+/\*\*.*\*/\s*$", nxt) and i + 2 < len(lines):
                fld = lines[i + 2]
                if re.search(r"private|protected|public", fld):
                    indent = re.match(r"^(\s*)", line).group(1)
                    out.append(indent + nxt.strip())
                    out.append(line)
                    out.append(fld)
                    i += 3
                    continue
        out.append(line)
        i += 1
    return "\n".join(out) + ("\n" if content.endswith("\n") else "")


def main():
    n = 0
    for path in ROOT.rglob("*.java"):
        if path.name == "package-info.java":
            continue
        old = path.read_text(encoding="utf-8")
        new = cleanup(old)
        if new != old:
            path.write_text(new, encoding="utf-8", newline="\n")
            n += 1
    print(f"cleaned: {n}")


if __name__ == "__main__":
    main()
