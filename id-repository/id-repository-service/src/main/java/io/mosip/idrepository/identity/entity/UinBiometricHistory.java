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
 * Historical biometric version ({@code idrepo.uin_biometric_h}).
 */

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(HistoryPK.class)
@Table(name = "uin_biometric_h", schema = "idrepo")
public class UinBiometricHistory {

	/** Salt-shard reference id (primary key; maps to {@code uin_ref_id}). */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** Effective datetime from which the record version is valid. */
	@Id
	@Column(name = "eff_dtimes")
	private LocalDateTime effectiveDateTime;

	/** Object-store or DB reference to biometric file. */
	@Column(name = "bio_file_id")
	private String bioFileId;

	/** Biometric file type ({@code biometric_file_type} column). */
	@Column(name = "biometric_file_type")
	private String biometricFileType;

	/** Biometric file name ({@code biometric_file_name} column). */
	@Column(name = "biometric_file_name")
	private String biometricFileName;

	/** Biometric file hash ({@code biometric_file_hash} column). */
	@Column(name = "biometric_file_hash")
	private String biometricFileHash;

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
}
