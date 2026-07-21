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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Draft document row pending publish ({@code idrepo.uin_document_draft}).
 */
@Getter
@Setter
@ToString(exclude = { "uin" })
@Entity
@NoArgsConstructor
@IdClass(DocumentDraftPK.class)
@Table(schema = "idrepo", name = "uin_document_draft" )
public class UinDocumentDraft {

	public UinDocumentDraft(String regId, String doccatCode, String doctypCode, String docId, String docName,
			String docfmtCode, String docHash, String createdBy, LocalDateTime createdDateTime,
			String updatedBy, LocalDateTime updatedDateTime, Boolean isDeleted, LocalDateTime deletedDateTime) {
		super();
		this.regId = regId;
		this.doccatCode = doccatCode;
		this.doctypCode = doctypCode;
		this.docId = docId;
		this.docName = docName;
		this.docfmtCode = docfmtCode;
		this.docHash = docHash;
		this.createdBy = createdBy;
		this.createdDateTime = createdDateTime;
		this.updatedBy = updatedBy;
		this.updatedDateTime = updatedDateTime;
		this.isDeleted = isDeleted;
		this.deletedDateTime = deletedDateTime;
	}

	/** Registration ID (RID) from registration processor. */
	@Id
	@Column(name = "reg_id")
	private String regId;

	/** Doccat code ({@code doccat_code} column). */
	@Id
	@Column(name = "doccat_code")
	private String doccatCode;

	/** Doctyp code ({@code doctyp_code} column). */
	//@Id
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
	@JoinColumn(name = "reg_id", insertable = false, updatable = false)
	@Setter(value = AccessLevel.NONE)
	@JsonBackReference
	/** Uin. */
	private UinDraft uin;
}
