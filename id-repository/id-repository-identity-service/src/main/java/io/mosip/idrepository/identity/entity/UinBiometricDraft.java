package io.mosip.idrepository.identity.entity;

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
@IdClass(BiometricDraftPK.class)
@Table(schema = "idrepo")
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

	/** The uin ref id. */
	@Id
	private String regId;

	/** The bio file id. */
	private String bioFileId;

	@Id
	private String biometricFileType;

	/** The biometric file name. */
	private String biometricFileName;

	/** The biometric file hash. */
	private String biometricFileHash;

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
	@JsonBackReference
	private UinDraft uin;
}
