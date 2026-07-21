package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Historical document version ({@code idrepo.uin_document_h}).
 */

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(HistoryPK.class)
@Table(name = "uin_document_h", schema = "idrepo")
public class UinDocumentHistory {

	/** Salt-shard reference id (primary key; maps to {@code uin_ref_id}). */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** Effective datetime from which the record version is valid. */
	@Id
	@Column(name = "eff_dtimes")
	private LocalDateTime effectiveDateTime;

	/** Doccat code ({@code doccat_code} column). */
	@Column(name = "doccat_code")
	private String doccatCode;

	/** Doctyp code ({@code doctyp_code} column). */
	@Column(name = "doctyp_code")
	private String doctypCode;

	/** Document category or type identifier. */
	@Column(name = "doc_id")
	private String docId;

	/** Doc name ({@code doc_name} column). */
	@Column(name = "doc_name")
	private String docName;

	/** Docfmt code ({@code docfmt_code} column). */
	@Column(name = "docfmt_code")
	private String docfmtCode;

	/** Hash of document bytes for deduplication. */
	@Column(name = "doc_hash")
	private String docHash;

	/** Preferred language code for identity attributes. */
	@Column(name = "lang_code")
	private String langCode;

	/** Audit — creator user or service id. */
	@Column(name = "cr_by")
	private String createdBy;

	/** Audit — row creation timestamp (UTC). */
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDateTime;

	/** Audit — last updater user or service id. */
	@Column(name = "upd_by")
	private String updatedBy;

	/** Audit — last update timestamp (UTC). */
	@Column(name = "upd_dtimes")
	private LocalDateTime updatedDateTime;

	/** Soft-delete flag. */
	@Column(name = "is_deleted")
	private Boolean isDeleted;

	/** Soft-delete timestamp (UTC). */
	@Column(name = "del_dtimes")
	/** Deleted date time. */
	private LocalDateTime deletedDateTime;
}
