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
 * The Class UinBiometric - Entity class for uin_biometric table.
 *
 * @author Manoj SP
 */
@Data
@Entity
@IdClass(BiometricPK.class)
@Table(schema = "idrepo")
public class UinBiometric implements Serializable {
	
	public UinBiometric() {
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6571434413414922814L;

	public UinBiometric(String uinRefId, String bioFileId, String biometricFileType, String biometricFileName,
			String biometricFileHash, String langCode, String createdBy, LocalDateTime createdDateTime,
			String updatedBy, LocalDateTime updatedDateTime, Boolean isDeleted, LocalDateTime deletedDateTime) {
		super();
		this.uinRefId = uinRefId;
		this.bioFileId = bioFileId;
		this.biometricFileType = biometricFileType;
		this.biometricFileName = biometricFileName;
		this.biometricFileHash = biometricFileHash;
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

	/** The bio file id. */
	@Column(name = "bio_file_id")
	private String bioFileId;

	@Id
	@Column(name = "biometric_file_type")
	private String biometricFileType;

	/** The biometric file name. */
	@Column(name = "biometric_file_name")
	private String biometricFileName;

	/** The biometric file hash. */
	@Column(name = "biometric_file_hash")
	private String biometricFileHash;

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
	@JoinColumn(name = "uinRefId", insertable = false, updatable = false)
	@JsonBackReference
	private Uin uin;
}
