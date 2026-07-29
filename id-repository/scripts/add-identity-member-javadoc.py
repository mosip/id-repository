#!/usr/bin/env python3
"""Add field and public-method JavaDoc at class-body level for identity package."""
import re
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ROOTS = [
    BASE / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "identity",
    BASE / "id-repository-service" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "identity",
]
SKIP_METHODS = {"equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait"}


def humanize(name: str) -> str:
    s = re.sub(r"([a-z])([A-Z])", r"\1 \2", name)
    return s.replace("_", " ").lower().strip()


def field_doc(name: str, ftype: str) -> str:
    hint = humanize(name)
    if ftype == "boolean":
        return f"/** Whether {hint}. */"
    if "List<" in ftype or "Map<" in ftype:
        return f"/** {hint.capitalize()} ({ftype}). */"
    return f"/** {hint.capitalize()}. */"


def method_doc(name: str, params, ret: str) -> list:
    lines = ["/**"]
    if name.startswith("get") and not params:
        lines.append(f" * @return {humanize(name[3:])}")
    elif name.startswith("set") and len(params) == 1:
        lines.append(f" * @param {params[0][1]} {humanize(params[0][1])}")
    elif name.startswith("is") and not params and ret == "boolean":
        lines.append(f" * @return whether {humanize(name[2:])}")
    else:
        lines.append(f" * {humanize(name).capitalize()}.")
        for _, pname in params:
            lines.append(f" * @param {pname} {humanize(pname)}")
        if ret != "void":
            lines.append(f" * @return {humanize(ret)}")
    lines.append(" */")
    return lines


def has_javadoc_before(lines, idx):
    j = idx - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    return j >= 0 and lines[j].strip().endswith("*/")


def parse_params(sig: str):
    inside = sig[sig.index("(") + 1 : sig.rindex(")")]
    if not inside.strip():
        return []
    params = []
    depth = 0
    cur = ""
    for ch in inside:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        elif ch == "," and depth == 0:
            if cur.strip():
                t, n = cur.strip().rsplit(" ", 1)
                params.append((t, n))
            cur = ""
            continue
        cur += ch
    if cur.strip():
        t, n = cur.strip().rsplit(" ", 1)
        params.append((t, n))
    return params


def process(content: str) -> str:
    lines = content.splitlines()
    out = []
    depth = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        opens = line.count("{")
        closes = line.count("}")
        at_class_body = depth == 1

        if at_class_body and not has_javadoc_before(out, len(out)):
            fm = re.match(
                r"^(\s*)((?:@\w+(?:\([^)]*\))?\s+)*)((?:public|private|protected)\s+)?(?:static\s+)?(?:final\s+)?([\w.<>,\[\]?]+)\s+(\w+)\s*(=.*)?;\s*$",
                line,
            )
            if fm and fm.group(5) not in ("serialVersionUID",):
                indent, _, _, ftype, fname, _ = fm.groups()
                if fname == "LOGGER" or (not fname.isupper()):
                    out.extend([indent + field_doc(fname, ftype)])
                    out.append(line)
                    i += 1
                    depth += opens - closes
                    continue

            if re.match(r"^\s*(public|protected)\s+", stripped) and "(" in stripped:
                mm = re.match(
                    r"^\s*(public|protected)\s+(?:static\s+)?(?:<[^>]+>\s+)?([\w.<>,\[\]?]+)\s+(\w+)\s*\(",
                    stripped,
                )
                if mm:
                    ret, mname = mm.group(2), mm.group(3)
                    if mname not in SKIP_METHODS:
                        sig = stripped
                        j = i + 1
                        while j < len(lines) and "{" not in sig and not sig.rstrip().endswith(";"):
                            sig += " " + lines[j].strip()
                            j += 1
                        if "{" in sig or sig.rstrip().endswith(";"):
                            params = parse_params(sig)
                            indent = re.match(r"^(\s*)", line).group(1)
                            out.extend([indent + dl for dl in method_doc(mname, params, ret)])
                            out.append(line)
                            i += 1
                            depth += opens - closes
                            continue

        out.append(line)
        i += 1
        depth += opens - closes
        if depth < 0:
            depth = 0
    return "\n".join(out) + ("\n" if content.endswith("\n") else "")


def main():
    n = 0
    for root in ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.java")):
            old = path.read_text(encoding="utf-8")
            new = process(old)
            if new != old:
                path.write_text(new, encoding="utf-8", newline="\n")
                n += 1
                print(path.relative_to(BASE))
    print(f"updated: {n}")


if __name__ == "__main__":
    main()
