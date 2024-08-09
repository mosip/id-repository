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
 * The Class UinDocumentHistory - Entity class for uin_document_h table.
 *
 * @author Manoj SP
 */

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(HistoryPK.class)
@Table(name = "uin_document_h", schema = "idrepo")
public class UinDocumentHistory {

	/** The uin ref id. */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** The effective date time. */
	@Id
	@Column(name = "eff_dtimes")
	private LocalDateTime effectiveDateTime;

	/** The doccat code. */
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
}
