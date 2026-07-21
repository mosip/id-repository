#!/usr/bin/env python3
"""Enrich JavaDoc for io.mosip.idrepository.vid.* and salt.* packages."""
import re
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ROOTS = [
    BASE / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "vid",
    BASE / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "salt",
    BASE / "id-repository-service" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "vid",
]

COLUMN_DOCS = {
    "id": "Sequence / primary key for the salt row.",
    "salt": "Base64-encoded salt bytes used for UIN/VID hashing or encryption.",
    "cr_by": "Audit — creator (typically {@code System} for batch job).",
    "cr_dtimes": "Audit — row creation timestamp (UTC).",
    "upd_by": "Audit — last updater.",
    "upd_dtimes": "Audit — last update timestamp (UTC).",
    "vid": "Virtual ID token (encrypted at rest in {@code idmap.vid}).",
    "uin_hash": "SHA-256 hash of linked UIN.",
    "uin": "Encrypted UIN token linked to this VID.",
    "vidtyp_code": "VID type (PERPETUAL, TEMPORARY, etc.).",
    "generated_dtimes": "When the VID was generated.",
    "expiry_dtimes": "VID expiry; {@code null} for non-expiring types.",
    "status_code": "VID lifecycle status (ACTIVE, EXPIRED, etc.).",
    "is_deleted": "Soft-delete flag.",
    "del_dtimes": "Soft-delete timestamp (UTC).",
}

CLASS_DOCS = {
    "Vid": """/**
 * Virtual ID row mapped to {@code idmap.vid}.
 * <p>
 * Links a generated VID to a UIN hash; {@link #uin} is encrypted by
 * {@link io.mosip.idrepository.vid.interceptor.IdRepoVidEntityInterceptor} on persist.
 * </p>
 *
 * @author Prem Kumar
 */""",
    "VidRepo": """/**
 * Spring Data repository for {@link io.mosip.idrepository.vid.entity.Vid} ({@code idmap.vid}).
 * <p>
 * Supports lookup by VID token, UIN hash, type, and expiry for policy enforcement.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 */""",
    "VidUinHashSaltRepo": """/**
 * Hash-salt lookup on the <strong>idmap</strong> persistence unit ({@code uin_hash_salt}).
 * <p>
 * Separate from idrepo salts to prevent cross-database mis-routing in the merged JVM.
 * </p>
 */""",
    "VidUinEncryptSaltRepo": """/**
 * Encrypt-salt lookup on the <strong>idmap</strong> persistence unit ({@code uin_encrypt_salt}).
 */""",
    "VidServiceImpl": """/**
 * Core VID service: generate, regenerate, activate/deactivate, and retrieve VIDs.
 * <p>
 * Implements {@link io.mosip.idrepository.core.spi.VidService}; uses {@code idmap} transaction manager,
 * {@link io.mosip.idrepository.vid.provider.VidPolicyProvider} for limits, and publishes WebSub events.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 */""",
    "VidPolicyProvider": """/**
 * Loads and caches VID issuance policy from config-server JSON (validated against schema).
 * <p>
 * Supplies per-VID-type limits (count, expiry) to {@link io.mosip.idrepository.vid.service.impl.VidServiceImpl}.
 * </p>
 *
 * @author Manoj SP
 */""",
    "IdRepoVidEntityInterceptor": """/**
 * Hibernate interceptor encrypting {@link io.mosip.idrepository.vid.entity.Vid#uin} before flush to {@code idmap}.
 *
 * @author Manoj SP
 */""",
    "VidRequestValidator": """/**
 * Validates VID REST requests (create, update, regenerate, activate/deactivate).
 * <p>
 * Extends {@link io.mosip.idrepository.core.validator.BaseIdRepoValidator}; checks UIN/VID format and policy types.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 */""",
    "VidController": """/**
 * REST controller for VID APIs ({@code /idrepository/v1/vid/*}).
 * <p>
 * Generate, regenerate, update status, and retrieve VIDs; delegates to {@link io.mosip.idrepository.core.spi.VidService}.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 */""",
    "VidAuthorizedRolesDto": """/**
 * Keycloak role lists for VID REST endpoints ({@code mosip.role.idrepo.vid.*}).
 * <p>
 * Referenced by {@code @PreAuthorize} SpEL on {@link io.mosip.idrepository.vid.controller.VidController}.
 * </p>
 */""",
    "IdentityHashSaltEntity": """/**
 * Hash salt row for identity DB ({@code idrepo.uin_hash_salt}).
 * <p>
 * Populated by the salt-generator K8s job; consumed by {@code mosip_idrepo} crypto.
 * </p>
 *
 * @author Manoj SP
 */""",
    "IdentityEncryptSaltEntity": """/**
 * Encrypt salt row for identity DB ({@code idrepo.uin_encrypt_salt}).
 *
 * @author Manoj SP
 */""",
    "VidHashSaltEntity": """/**
 * Hash salt row for idmap DB ({@code idmap.uin_hash_salt}).
 * <p>
 * Hash values mirror idrepo for the same sequence id; encrypt salts differ per database.
 * </p>
 *
 * @author Manoj SP
 */""",
    "VidEncryptSaltEntity": """/**
 * Encrypt salt row for idmap DB ({@code idmap.uin_encrypt_salt}).
 *
 * @author Manoj SP
 */""",
    "ISaltEntity": """/**
 * Common contract for salt-generator JPA entities (sequence id + salt + audit columns).
 */""",
    "IdRepoSaltEntitiesComposite": """/**
 * Batch item bundling one sequence id's salts for both idrepo and idmap databases.
 * <p>
 * Produced by {@link io.mosip.idrepository.salt.step.SaltReader}; consumed by {@link io.mosip.idrepository.salt.step.SaltWriter}.
 * </p>
 */""",
    "SaltReader": """/**
 * Spring Batch {@link org.springframework.batch.item.ItemReader} that materializes salt rows for a configured id range.
 * <p>
 * Config: {@link io.mosip.idrepository.salt.constant.SaltGeneratorConstant#START_SEQ} /
 * {@link io.mosip.idrepository.salt.constant.SaltGeneratorConstant#END_SEQ}. Salt bytes from {@code HMACUtils2.generateSalt()}.
 * </p>
 *
 * @author Manoj SP
 */""",
    "SaltWriter": """/**
 * Spring Batch {@link org.springframework.batch.item.ItemWriter} persisting composite salts to idrepo then idmap.
 * <p>
 * Skips insert when any id in the chunk already exists (idempotent job re-runs).
 * </p>
 *
 * @author Manoj SP
 */""",
    "SaltGenerator": """/**
 * Orchestrates one-shot salt population: read id range, write to both databases.
 * <p>
 * Entry point for the K8s Job via {@code io.mosip.idrepository.service.SaltGeneratorBootApplication}.
 * </p>
 */""",
    "SaltGeneratorConstant": """/**
 * Configuration property keys for the salt-generator batch job.
 *
 * @author Manoj SP
 */""",
    "Database": """/**
 * Logical datasource target for the salt-generator job ({@code mosip_idrepo} vs {@code mosip_idmap}).
 */""",
    "DatabaseThreadContext": """/**
 * {@link ThreadLocal} holder routing JDBC to PRIMARY (idrepo) or SECONDARY (idmap) during salt writes.
 */""",
    "RoutingDataSource": """/**
 * {@link org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource} keyed by {@link DatabaseThreadContext}.
 */""",
    "DatabaseRouter": """/**
 * Wires primary/secondary datasources and the routing datasource for the salt-generator job.
 */""",
    "IdentityHashSaltRepository": """/**
 * JPA repository for {@link io.mosip.idrepository.salt.entity.idrepo.IdentityHashSaltEntity}.
 *
 * @author Manoj SP
 */""",
    "IdentityEncryptSaltRepository": """/**
 * JPA repository for {@link io.mosip.idrepository.salt.entity.idrepo.IdentityEncryptSaltEntity}.
 *
 * @author Manoj SP
 */""",
    "VidHashSaltRepository": """/**
 * JPA repository for {@link io.mosip.idrepository.salt.entity.idmap.VidHashSaltEntity}.
 *
 * @author Manoj SP
 */""",
    "VidEncryptSaltRepository": """/**
 * JPA repository for {@link io.mosip.idrepository.salt.entity.idmap.VidEncryptSaltEntity}.
 *
 * @author Manoj SP
 */""",
}


def humanize(name: str) -> str:
    s = re.sub(r"([a-z])([A-Z])", r"\1 \2", name)
    return s.replace("_", " ").lower().strip()


def replace_class_javadoc(content: str, class_name: str) -> str:
    if class_name not in CLASS_DOCS:
        if "The Class " in content or "The Interface " in content or "The Entity " in content or "The Repository " in content:
            pass
        else:
            return content
    new_doc = CLASS_DOCS.get(class_name)
    if not new_doc and ("The Class " in content or "The Interface " in content or "The Entity " in content):
        new_doc = f"/**\n * {class_name} — identity/VID/salt module component.\n */"
    if not new_doc:
        return content
    m = re.search(
        r"/\*\*.*?\*/\s*\n((?:@\w+(?:\([^)]*\))?\s*\n)*)(public\s+(?:class|interface|enum)\s+" + re.escape(class_name) + r"\b)",
        content,
        re.DOTALL,
    )
    if m:
        return content[: m.start()] + new_doc + "\n" + m.group(1) + m.group(2) + content[m.end() :]
    # no existing javadoc — insert before annotations
    m2 = re.search(
        r"((?:@\w+(?:\([^)]*\))?\s*\n)*)(public\s+(?:class|interface|enum)\s+" + re.escape(class_name) + r"\b)",
        content,
    )
    if m2 and class_name in CLASS_DOCS:
        return content[: m2.start()] + new_doc + "\n" + m2.group(1) + m2.group(2) + content[m2.end() :]
    return content


def upgrade_fields(content: str) -> str:
    lines = content.splitlines()
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if re.match(r"^\s*/\*\* The [^*]+ \*/\s*$", line) or re.match(r"^\s*/\*\* The value to hold .+ \*/\s*$", line):
            j = i + 1
            block = []
            while j < len(lines) and not re.match(r"^\s*private\s+", lines[j]):
                block.append(lines[j])
                j += 1
            col = None
            blob = "\n".join(block)
            m = re.search(r'name\s*=\s*"([a-z_]+)"', blob)
            if m:
                col = m.group(1)
            if j < len(lines) and col and col in COLUMN_DOCS:
                indent = re.match(r"^(\s*)", line).group(1)
                out.append(f"{indent}/** {COLUMN_DOCS[col]} */")
                i += 1
                continue
        if re.match(r"^\s*@Column", line):
            j = i
            block = []
            while j < len(lines) and not re.match(r"^\s*private\s+", lines[j]):
                block.append(lines[j])
                j += 1
            if j < len(lines) and (not out or not out[-1].strip().startswith("/**")):
                blob = "\n".join(block)
                m = re.search(r'name\s*=\s*"([a-z_]+)"', blob)
                if m and m.group(1) in COLUMN_DOCS:
                    fname_m = re.search(r"private\s+[\w.<>,\[\]?]+\s+(\w+)\s*;", lines[j])
                    if fname_m:
                        indent = re.match(r"^(\s*)", line).group(1)
                        out.append(f"{indent}/** {COLUMN_DOCS[m.group(1)]} */")
        out.append(line)
        i += 1
    return "\n".join(out) + ("\n" if content.endswith("\n") else "")


def remove_stray_class_javadoc(content: str) -> str:
    return re.sub(
        r"\n/\*\*\n \* Instantiates a new salt entity\.\n \*/\n@Data",
        "\n@Data",
        content,
    )


def process_file(path: Path) -> str:
    content = path.read_text(encoding="utf-8")
    cm = re.search(r"public\s+(?:class|interface|enum)\s+(\w+)", content)
    if not cm:
        return content
    class_name = cm.group(1)
    new = remove_stray_class_javadoc(content)
    new = replace_class_javadoc(new, class_name)
    new = upgrade_fields(new)
    new = re.sub(r"/\*\* The mosip logger\. \*/", "/** Logger for this class. */", new)
    new = re.sub(r"/\*\* The ([a-z][^.]*)\. \*/", lambda m: f"/** {humanize(m.group(1)).capitalize()}. */", new)
    return new


def main():
    n = 0
    for root in ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.java")):
            old = path.read_text(encoding="utf-8")
            new = process_file(path)
            if new != old:
                path.write_text(new, encoding="utf-8", newline="\n")
                n += 1
                print(path.relative_to(BASE))
    print(f"updated: {n}")


if __name__ == "__main__":
    main()
