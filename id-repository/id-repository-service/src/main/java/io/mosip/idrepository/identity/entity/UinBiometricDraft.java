package io.mosip.idrepository.identity.entity;

import java.io.Serializable;
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

import lombok.Data;

/**
 * Draft biometric row pending publish ({@code idrepo.uin_biometric_draft}).
 */
@Data
@Entity
@IdClass(BiometricDraftPK.class)
@Table(schema = "idrepo", name = "uin_biometric_draft")
public class UinBiometricDraft implements Serializable {
	
	public UinBiometricDraft() {
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6571434413414922814L;

	public UinBiometricDraft(String regId, String bioFileId, String biometricFileType, String biometricFileName,
			String biometricFileHash, String createdBy, LocalDateTime createdDateTime,
			String updatedBy, LocalDateTime updatedDateTime, Boolean isDeleted, LocalDateTime deletedDateTime) {
		super();
		this.regId = regId;
		this.bioFileId = bioFileId;
		this.biometricFileType = biometricFileType;
		this.biometricFileName = biometricFileName;
		this.biometricFileHash = biometricFileHash;
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

	/** Object-store or DB reference to biometric file. */
	@Column(name = "bio_file_id")
	private String bioFileId;

	@Id
	/** Biometric file type ({@code biometric_file_type} column). */
	@Column(name = "biometric_file_type")
	private String biometricFileType;

	/** Biometric file name ({@code biometric_file_name} column). */
	@Column(name = "biometric_file_name")
	private String biometricFileName;

	/** Biometric file hash ({@code biometric_file_hash} column). */
	@Column(name = "biometric_file_hash")
	private String biometricFileHash;

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
	@JsonBackReference
	/** Uin. */
	private UinDraft uin;
}
