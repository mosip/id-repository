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
 * The Class UinDocument - Entity class for uin_document table.
 *
 * @author Manoj SP
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

	/** The uin ref id. */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** The doccat code. */
	@Id
	@Column(name = "doccat_code")
	private String doccatCode;

	/** The doctyp code. */
	@Column(name = "doctyp_code")
	private String doctypCode;

	/** The doc id. */
	@Column(name = "doc_id")
	private String docId;

	/** The doc name. */
	@Column(name = "doc_name")
	private String docName;

	/** The docfmt code. */
	@Column(name = "docfmt_code")
	private String docfmtCode;

	/** The doc hash. */
	@Column(name = "doc_hash")
	private String docHash;

	/** The lang code. */
	@Column(name = "lang_code")
	private String langCode;

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
	@Column(name = "is_deleted")
	private Boolean isDeleted;

	/** The deleted date time. */
	@Column(name = "del_dtimes")
	private LocalDateTime deletedDateTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uin_ref_id", insertable = false, updatable = false)
	@JsonBackReference
	private Uin uin;
}
