package io.mosip.idrepository.credentialsfeeder.entity;

import java.io.Serializable;
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
	private String uinRefId;

	/** The bio file id. */
	private String bioFileId;

	@Id
	private String biometricFileType;

	/** The biometric file name. */
	private String biometricFileName;

	/** The biometric file hash. */
	private String biometricFileHash;

	/** The lang code. */
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
	private Boolean isDeleted;

	/** The deleted date time. */
	@Column(name = "del_dtimes")
	private LocalDateTime deletedDateTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uinRefId", insertable = false, updatable = false)
	@JsonBackReference
	private Uin uin;
}
