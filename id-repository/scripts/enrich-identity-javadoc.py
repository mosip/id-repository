#!/usr/bin/env python3
"""Enrich JavaDoc under io.mosip.idrepository.identity.* with detailed descriptions."""
import re
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ROOTS = [
    BASE / "id-repository-core" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "identity",
    BASE / "id-repository-service" / "src" / "main" / "java" / "io" / "mosip" / "idrepository" / "identity",
]

SHALLOW_CLASS = re.compile(
    r"/\*\*\s*\n(?:\s*\*[^\n]*\n)*?\s*\* (?:The Class |The Interface |The Enum )",
    re.MULTILINE,
)
SHALLOW_FIELD = re.compile(r"/\*\* The ([a-z][^.]*)\. \*/", re.IGNORECASE)
SHALLOW_METHOD = re.compile(
    r"/\*\*\s*\n\s*\* Gets the ([^.]+)\.\s*\n\s*\*\s*\n\s*\* @return the \1\s*\n\s*\*/",
    re.IGNORECASE,
)

COLUMN_DOCS = {
    "uin_ref_id": "Salt-shard reference id (primary key; maps to {@code uin_ref_id}).",
    "uin": "Tokenized UIN stored for lookup (encrypted at rest).",
    "uin_hash": "SHA-256 hash of UIN used for indexed lookup without decryption.",
    "uin_data": "Encrypted demographic identity JSON blob ({@code uin_data}).",
    "uin_data_hash": "Integrity hash of decrypted {@code uin_data} payload.",
    "reg_id": "Registration ID (RID) from registration processor.",
    "bio_ref_id": "Biometric reference id linking CBEFF rows.",
    "status_code": "Identity lifecycle status (ACTIVATED, BLOCKED, etc.).",
    "lang_code": "Preferred language code for identity attributes.",
    "cr_by": "Audit — creator user or service id.",
    "cr_dtimes": "Audit — row creation timestamp (UTC).",
    "upd_by": "Audit — last updater user or service id.",
    "upd_dtimes": "Audit — last update timestamp (UTC).",
    "is_deleted": "Soft-delete flag.",
    "del_dtimes": "Soft-delete timestamp (UTC).",
    "eff_dtimes": "Effective datetime from which the record version is valid.",
    "id_hash": "Hash of individual id used for anonymous profile correlation.",
    "profile_data": "Serialized anonymous profile JSON.",
    "auth_type": "Authentication type code (bio, demo, otp, etc.).",
    "lock_type": "Lock category applied to the auth type.",
    "lock_status": "Whether the auth type is currently locked.",
    "lock_duration": "Lock duration in configured time unit.",
    "unlock_expiry_dtimes": "Timestamp when the lock automatically expires.",
    "doc_id": "Document category or type identifier.",
    "doc_hash": "Hash of document bytes for deduplication.",
    "doc_format": "Document encoding format (pdf, jpg, etc.).",
    "bio_file_id": "Object-store or DB reference to biometric file.",
    "bio_file_hash": "Hash of biometric file content.",
    "bio_type": "Biometric modality (FIR, FMR, face, etc.).",
    "bio_sub_type": "Biometric sub-type (left thumb, right iris, etc.).",
    "bio_format": "CBEFF or ISO encoding format.",
    "bio_data": "Encrypted biometric BDB bytes.",
    "channel": "Notification channel (email, phone).",
    "channel_info": "Masked channel value or handle.",
    "handle": "Resident handle value (email/phone).",
    "handle_hash": "Hash of handle for lookup.",
    "draft_id": "Draft identity primary key.",
    "rid": "Registration id in API payloads.",
}

CLASS_DOCS = {
    "Uin": """/**
 * Primary UIN aggregate mapped to {@code idrepo.uin}.
 * <p>
 * Root entity for resident identity: encrypted {@link #uinData}, linked {@link UinBiometric}
 * and {@link UinDocument} children, and lifecycle {@link #statusCode}. {@link #uinRefId} is the
 * salt-shard key; demographic updates flow through {@link io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl}.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.entity.UinInfo
 */""",
    "UinHistory": """/**
 * Historical snapshot of a UIN row ({@code idrepo.uin_h}).
 * <p>
 * Written on each identity update; composite key {@link HistoryPK} ties version to effective time.
 * </p>
 */""",
    "UinDraft": """/**
 * Staged identity draft before activation ({@code idrepo.uin_draft}).
 * <p>
 * Holds in-progress demographic/biometric/document changes until publish via draft APIs.
 * </p>
 *
 * @see io.mosip.idrepository.identity.service.impl.IdRepoDraftServiceImpl
 */""",
    "UinBiometric": """/**
 * Biometric BDB row linked to a {@link Uin} ({@code idrepo.uin_biometric}).
 * <p>
 * Composite key {@link BiometricPK}; payload encrypted by {@link io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor}.
 * </p>
 */""",
    "UinDocument": """/**
 * Proof-of-identity document linked to a {@link Uin} ({@code idrepo.uin_document}).
 */""",
    "UinBiometricHistory": """/**
 * Historical biometric version ({@code idrepo.uin_biometric_h}).
 */""",
    "UinDocumentHistory": """/**
 * Historical document version ({@code idrepo.uin_document_h}).
 */""",
    "UinBiometricDraft": """/**
 * Draft biometric row pending publish ({@code idrepo.uin_biometric_draft}).
 */""",
    "UinDocumentDraft": """/**
 * Draft document row pending publish ({@code idrepo.uin_document_draft}).
 */""",
    "BiometricPK": """/**
 * Composite primary key for {@link UinBiometric} ({@code bio_ref_id}, {@code bio_type}, {@code bio_sub_type}).
 */""",
    "BiometricDraftPK": """/**
 * Composite primary key for {@link UinBiometricDraft}.
 */""",
    "DocumentPK": """/**
 * Composite primary key for {@link UinDocument} ({@code uin_ref_id}, {@code doc_id}).
 */""",
    "DocumentDraftPK": """/**
 * Composite primary key for {@link UinDocumentDraft}.
 */""",
    "HistoryPK": """/**
 * Composite key for history tables ({@code uin_ref_id} + effective datetime).
 */""",
    "AuthtypeLock": """/**
 * Per-auth-type lock state for a UIN ({@code idrepo.uin_auth_lock}).
 * <p>
 * Updated by {@link io.mosip.idrepository.identity.service.impl.AuthTypeStatusImpl}.
 * </p>
 */""",
    "AnonymousProfileEntity": """/**
 * Anonymous profile correlation row ({@code idrepo.anonymous_profile}).
 */""",
    "ChannelInfo": """/**
 * Resident notification channel metadata ({@code idrepo.channel_info}).
 */""",
    "IdentityUpdateTracker": """/**
 * Tracks identity update counts against partner policy limits ({@code idrepo.identity_update_tracker}).
 */""",
    "UinRepo": """/**
 * Spring Data repository for {@link io.mosip.idrepository.identity.entity.Uin} ({@code idrepo.uin}).
 * <p>
 * Lookup by RID, UIN hash, and status; used by {@link io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl}.
 * </p>
 *
 * @author Manoj SP
 */""",
    "RidDto": """/**
 * API response carrying a resident registration id (RID).
 *
 * @author Ritik Jain
 */""",
    "HandleDto": """/**
 * Handle (email/phone) value exposed in identity API responses.
 */""",
    "AttributeListDto": """/**
 * Wrapper listing sharable identity attribute names for retrieve requests.
 */""",
    "UpdateCountDto": """/**
 * Remaining allowed update count for a constrained identity field per partner policy.
 */""",
    "IdentityAuthorizedRolesDto": """/**
 * Keycloak role names bound to identity REST endpoints for {@code @PreAuthorize} SpEL.
 */""",
    "IdRepoServiceImpl": """/**
 * Core identity service: add, update, retrieve, and lifecycle operations on UIN.
 * <p>
 * Orchestrates encryption, history, credential issuance triggers, VID/handle integration,
 * and WebSub events. Primary implementation of {@link io.mosip.idrepository.core.spi.IdRepoService}.
 * </p>
 */""",
    "IdRepoDraftServiceImpl": """/**
 * Draft identity workflow: create, update, publish, and discard staged UIN changes.
 */""",
    "IdRepoProxyServiceImpl": """/**
 * Proxy retrieve implementation routing reads through policy and partner filters.
 */""",
    "AuthTypeStatusImpl": """/**
 * Manages authentication-type lock/unlock status for a UIN.
 * <p>
 * Implements {@link io.mosip.idrepository.core.spi.AuthtypeStatusService}.
 * </p>
 */""",
    "BiometricExtractionServiceImpl": """/**
 * Extracts ISO biometric templates from CBEFF for credential and auth flows.
 * <p>
 * Implements {@link io.mosip.idrepository.core.spi.BiometricExtractionService}.
 * </p>
 */""",
    "DefaultShardResolver": """/**
 * Resolves salt-shard / DB routing key from UIN hash for multi-DB deployments.
 */""",
    "IdRequestValidator": """/**
 * Validates inbound identity REST requests (add, update, retrieve, auth lock).
 * <p>
 * Extends {@link io.mosip.idrepository.core.validator.BaseIdRepoValidator} with identity-specific rules.
 * </p>
 */""",
    "IndividualIdValidator": """/**
 * Validates individual id type and format (UIN, VID, RID) on retrieve and update paths.
 */""",
    "IdRepoEntityInterceptor": """/**
 * Hibernate interceptor encrypting/decrypting UIN and biometric columns at persistence boundary.
 */""",
    "IdRepoFilter": """/**
 * Servlet filter attaching request id and audit context to identity HTTP calls.
 */""",
    "IdRepoController": """/**
 * REST controller for identity APIs ({@code /idrepository/v1/identity/*}).
 * <p>
 * Thin HTTP layer over {@link io.mosip.idrepository.core.spi.IdRepoService}.
 * </p>
 */""",
    "IdRepoDraftController": """/**
 * REST controller for draft identity APIs ({@code /idrepository/v1/identity/draft/*}).
 */""",
    "VidEventCallbackController": """/**
 * WebSub callback endpoint for VID lifecycle events affecting identity linkage.
 */""",
    "IdentityUpdateTrackerPolicyProvider": """/**
 * Supplies partner policy limits for identity field update tracking.
 */""",
}


def humanize(name: str) -> str:
    s = re.sub(r"([a-z])([A-Z])", r"\1 \2", name)
    return s.replace("_", " ").lower().strip()


def column_for_field(block: str) -> str | None:
    m = re.search(r'@Column\([^)]*name\s*=\s*"?([a-z_]+)"?', block)
    return m.group(1) if m else None


def default_class_doc(class_name: str, pkg: str) -> str:
    if class_name in CLASS_DOCS:
        return CLASS_DOCS[class_name]
    if class_name.endswith("Repo"):
        entity = class_name.replace("Repo", "")
        return (
            "/**\n"
            f" * Spring Data JPA repository for `idrepo` {entity} persistence operations.\n"
            " */"
        )
    if class_name.endswith("Helper"):
        return f"""/**
 * Helper bean supporting identity service operations ({class_name}).
 */"""
    if class_name.endswith("Config"):
        return f"""/**
 * Spring configuration for the identity module ({class_name}).
 */"""
    if class_name.endswith("Impl"):
        return f"""/**
 * Service implementation for identity domain operations ({class_name}).
 */"""
    return f"""/**
 * Identity module component ({class_name}).
 */"""


def replace_class_javadoc(content: str, class_name: str) -> str:
    m = re.search(
        r"(/\*\*.*?\*/)\s*\n((?:@\w+(?:\([^)]*\))?\s*\n)*)(public\s+(?:class|interface|enum)\s+" + re.escape(class_name) + r"\b)",
        content,
        re.DOTALL,
    )
    if not m:
        return content
    old, anns, decl = m.group(1), m.group(2), m.group(3)
    if "The Class " in old or "The Interface " in old or "The Get " in old or len(old) < 120:
        new_doc = default_class_doc(class_name, "")
        return content.replace(old, new_doc, 1)
    return content


def field_doc_from_column(col: str, fname: str) -> str:
    if col in COLUMN_DOCS:
        return f"/** {COLUMN_DOCS[col]} */"
    return f"/** {humanize(fname).capitalize()} ({{@code {col}}} column). */"


def upgrade_fields(content: str) -> str:
    lines = content.splitlines()
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        sm = SHALLOW_FIELD.match(line.strip())
        if sm or (line.strip().startswith("/**") and i + 1 < len(lines) and not lines[i].strip().endswith("*/")):
            pass
        if sm:
            # look ahead for @Column
            j = i + 1
            block = []
            while j < len(lines) and not re.match(r"^\s*private\s+", lines[j]):
                block.append(lines[j])
                j += 1
            if j < len(lines):
                col = column_for_field("\n".join(block))
                fname_m = re.search(r"private\s+[\w.<>,\[\]?]+\s+(\w+)\s*;", lines[j])
                if col and fname_m:
                    out.append("\t" + field_doc_from_column(col, fname_m.group(1)))
                    i += 1
                    continue
        # field without javadoc before @Column
        if re.match(r"^\s*@Column", line) and (not out or not out[-1].strip().startswith("/**")):
            j = i
            block = []
            while j < len(lines) and not re.match(r"^\s*private\s+", lines[j]):
                block.append(lines[j])
                j += 1
            if j < len(lines) and (i == 0 or not lines[i - 1].strip().startswith("/**")):
                col = column_for_field("\n".join(block))
                fname_m = re.search(r"private\s+[\w.<>,\[\]?]+\s+(\w+)\s*;", lines[j])
                if col and fname_m and fname_m.group(1) not in ("serialVersionUID",):
                    out.append("\t" + field_doc_from_column(col, fname_m.group(1)))
        out.append(line)
        i += 1
    return "\n".join(out) + ("\n" if content.endswith("\n") else "")


def upgrade_shallow_getters(content: str) -> str:
    def repl(m):
        name = m.group(1).strip()
        return f"/**\n\t * @return {humanize(name)}\n\t */"

    return SHALLOW_METHOD.sub(repl, content)


def fix_annotation_order(content: str) -> str:
    content = re.sub(
        r"(@(?:Autowired|Value|Lazy|Qualifier|PostConstruct|Scheduled|Bean|Override|Query)\([^\n]*\)(?:\s*\n\s*@[^\n]+)*)\n(\s*/\*\*.*?\*/)",
        r"\2\n\1",
        content,
        flags=re.DOTALL,
    )
    content = re.sub(
        r"(@Value\([^\n]+\))\n(\s*/\*\*[^*]*\*/)\n(\s*private)",
        r"\2\n\1\n\3",
        content,
    )
    return content


def add_rid_field_doc(content: str) -> str:
    if "class RidDto" in content and "private String rid" in content and "/**" not in content.split("private String rid")[0][-80:]:
        content = content.replace(
            "\tprivate String rid;",
            "\t/** Registration id (RID) returned to the client. */\n\tprivate String rid;",
        )
    return content


def process_file(path: Path) -> str:
    content = path.read_text(encoding="utf-8")
    cm = re.search(r"public\s+(?:class|interface|enum)\s+(\w+)", content)
    if not cm:
        return content
    class_name = cm.group(1)
    new = content
    new = replace_class_javadoc(new, class_name)
    new = upgrade_fields(new)
    new = upgrade_shallow_getters(new)
    new = fix_annotation_order(new)
    new = add_rid_field_doc(new)
    # generic shallow field cleanup
    new = re.sub(r"/\*\* The logger\. \*/", "/** Logger for this class. */", new)
    new = re.sub(r"/\*\* The env\. \*/", "/** Spring environment for property resolution. */", new)
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
