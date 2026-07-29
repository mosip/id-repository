#!/usr/bin/env python3
"""Polish credential JavaDoc: dedupe field docs, fix annotation order."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "credential"


def dedupe_field_javadoc(content: str) -> str:
    """Remove generic second /** */ immediately before private/protected/public field."""
    lines = content.splitlines()
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # pattern: annotations ... then /** short */ then field
        if re.match(r"^\t/\*\*.*\*/\s*$", line):
            j = i + 1
            ann_start = len(out)
            while j < len(lines) and (
                lines[j].strip().startswith("@")
                or lines[j].strip() == ""
                or re.match(r"^\t/\*\*.*\*/\s*$", lines[j])
            ):
                if re.match(r"^\t/\*\*.*\*/\s*$", lines[j]) and j > i:
                    # second javadoc before field — skip it
                    j += 1
                    continue
                out.append(lines[j])
                j += 1
            if j < len(lines) and re.match(r"^\t(private|protected|public)\s+", lines[j]):
                out.append(lines[j])
                i = j + 1
                continue
            # not a field pattern — emit original line
            out.append(line)
            i += 1
            continue
        out.append(line)
        i += 1
    return "\n".join(out) + ("\n" if content.endswith("\n") else "")


def fix_component_order(content: str) -> str:
    return re.sub(
        r"@Component\n(/\*\*.*?\*/)\n(public class)",
        r"\1\n@Component\n\2",
        content,
        flags=re.DOTALL,
    )


def fix_value_javadoc_order(content: str) -> str:
    return re.sub(
        r"(\s*/\*\*[^*]*\*/)\s*\n@Value\(([^\n]+)\)\n(\s*private)",
        r"\1\n\t@Value(\2)\n\3",
        content,
    )


def fix_annotation_before_javadoc(content: str) -> str:
    """Move @Retryable/@Cacheable after method javadoc."""
    return re.sub(
        r"(@(?:Retryable|Cacheable|Autowired)\([^\n]*\)(?:\s*\n\s*@[^\n]+)*)\n(\s*/\*\*.*?\*/)\n(\s*public\s+)",
        r"\2\n\1\n\3",
        content,
        flags=re.DOTALL,
    )


def main():
    n = 0
    for path in ROOT.rglob("*.java"):
        if path.name == "package-info.java":
            continue
        old = path.read_text(encoding="utf-8")
        new = fix_annotation_before_javadoc(
            fix_value_javadoc_order(fix_component_order(dedupe_field_javadoc(old)))
        )
        if new != old:
            path.write_text(new, encoding="utf-8", newline="\n")
            n += 1
    print(f"fixed: {n}")


if __name__ == "__main__":
    main()
