package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The Class UinDocument - Entity class for uin_document table.
 *
 * @author Manoj SP
 */
@Getter
@Setter
@ToString(exclude = { "uin" })
@Entity
@NoArgsConstructor
@IdClass(DocumentDraftPK.class)
@Table(schema = "idrepo")
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

	/** The uin ref id. */
	@Id
	private String regId;

	/** The doccat code. */
	@Id
	private String doccatCode;

	/** The doctyp code. */
	@Id
	private String doctypCode;

	/** The doc id. */
	private String docId;

	/** The doc name. */
	private String docName;

	/** The docfmt code. */
	private String docfmtCode;

	/** The doc hash. */
	private String docHash;

	/** The created by. */
	@Column(name = "cr_by")
	private String createdBy;

	/** The created date time. */
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDateTime;

	/** The updated by. */
	@Column(name = "upd_by")
	private String updatedBy;

	/** The updated date time. */
	@Column(name = "upd_dtimes")
	private LocalDateTime updatedDateTime;

	/** The is deleted. */
	private Boolean isDeleted;

	/** The deleted date time. */
	@Column(name = "del_dtimes")
	private LocalDateTime deletedDateTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "regId", insertable = false, updatable = false)
	@Setter(value = AccessLevel.NONE)
	@JsonBackReference
	private UinDraft uin;
}
