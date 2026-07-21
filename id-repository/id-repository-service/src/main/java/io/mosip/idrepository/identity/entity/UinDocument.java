package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Proof-of-identity document linked to a {@link Uin} ({@code idrepo.uin_document}).
 */
@Getter
@Setter
@ToString(exclude = { "uin" })
@Entity
@NoArgsConstructor
@IdClass(DocumentPK.class)
@Table(schema = "idrepo", name="uin_document")
@JsonIgnoreProperties(value = { "uin" })
public class UinDocument {

	public UinDocument(String uinRefId, String doccatCode, String doctypCode, String docId, String docName,
			String docfmtCode, String docHash, String langCode, String createdBy, LocalDateTime createdDateTime,
			String updatedBy, LocalDateTime updatedDateTime, Boolean isDeleted, LocalDateTime deletedDateTime) {
		super();
		this.uinRefId = uinRefId;
		this.doccatCode = doccatCode;
		this.doctypCode = doctypCode;
		this.docId = docId;
		this.docName = docName;
		this.docfmtCode = docfmtCode;
		this.docHash = docHash;
		this.langCode = langCode;
		this.createdBy = createdBy;
		this.createdDateTime = createdDateTime;
		this.updatedBy = updatedBy;
		this.updatedDateTime = updatedDateTime;
		this.isDeleted = isDeleted;
		this.deletedDateTime = deletedDateTime;
	}

	/** Salt-shard reference id (primary key; maps to {@code uin_ref_id}). */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** Doccat code ({@code doccat_code} column). */
	@Id
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uin_ref_id", insertable = false, updatable = false)
	@JsonBackReference
	/** Uin. */
	private Uin uin;
}
