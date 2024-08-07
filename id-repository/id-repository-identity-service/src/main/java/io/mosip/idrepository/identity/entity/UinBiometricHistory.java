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
 * The Class UinBiometricHistory - Entity class for uin_biometric_h table.
 *
 * @author Manoj SP
 */

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(HistoryPK.class)
@Table(name = "uin_biometric_h", schema = "idrepo")
public class UinBiometricHistory {

	/** The uin ref id. */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** The effective date time. */
	@Id
	@Column(name = "eff_dtimes")
	private LocalDateTime effectiveDateTime;

	/** The bio file id. */
	@Column(name = "bio_file_id")
	private String bioFileId;

	/** The bio file id. */
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
}
