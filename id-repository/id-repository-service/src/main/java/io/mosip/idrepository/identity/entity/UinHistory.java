package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import io.mosip.idrepository.core.entity.UinInfo;
import org.hibernate.annotations.Type;
import org.springframework.data.domain.Persistable;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Historical snapshot of a UIN row ({@code idrepo.uin_h}).
 * <p>
 * Written on each identity update; composite key {@link HistoryPK} ties version to effective time.
 * </p>
 */
@Data
@Entity
@Table(name = "uin_h", schema = "idrepo")
@IdClass(HistoryPK.class)
public class UinHistory implements UinInfo, Persistable<String> {
	
	/**
	 * Instantiates a new uin history.
	 */
	public UinHistory() {
		
	}

	/**
	 * Instantiates a new uin history.
	 *
	 * @param uinRefId the uin ref id
	 * @param effectiveDateTime the effective date time
	 * @param uin the uin
	 * @param uinHash the uin hash
	 * @param uinData the uin data
	 * @param uinDataHash the uin data hash
	 * @param regId the reg id
	 * @param bioRefId the bio ref id
	 * @param statusCode the status code
	 * @param langCode the lang code
	 * @param createdBy the created by
	 * @param createdDateTime the created date time
	 * @param updatedBy the updated by
	 * @param updatedDateTime the updated date time
	 * @param isDeleted the is deleted
	 * @param deletedDateTime the deleted date time
	 */
	public UinHistory(String uinRefId, LocalDateTime effectiveDateTime, String uin, String uinHash, byte[] uinData,
			String uinDataHash, String regId, String statusCode, String createdBy,
			LocalDateTime createdDateTime, String updatedBy, LocalDateTime updatedDateTime, Boolean isDeleted,
			LocalDateTime deletedDateTime) {
		this.uinRefId = uinRefId;
		this.effectiveDateTime = effectiveDateTime;
		this.uin = uin;
		this.uinHash = uinHash;
		this.uinData = uinData.clone();
		this.uinDataHash = uinDataHash;
		this.regId = regId;
		this.statusCode = statusCode;
		this.createdBy = createdBy;
		this.createdDateTime = createdDateTime;
		this.updatedBy = updatedBy;
		this.updatedDateTime = updatedDateTime;
		this.isDeleted = isDeleted;
		this.deletedDateTime = deletedDateTime;
	}

	/** Salt-shard reference id (primary key; maps to {@code uin_ref_id}). */
	@Id
	@Column(name = "uin_ref_id")
	private String uinRefId;

	/** Effective datetime from which the record version is valid. */
	@Id
	@Column(name = "eff_dtimes")
	private LocalDateTime effectiveDateTime;

	/** Tokenized UIN stored for lookup (encrypted at rest). */
	@Column(name = "uin")
	private String uin;
	
	/** SHA-256 hash of UIN used for indexed lookup without decryption. */
	@Column(name = "uin_hash")
	private String uinHash;

	/** Encrypted demographic identity JSON blob ({@code uin_data}). */
	@Basic(fetch = FetchType.LAZY)
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Column(name = "uin_data")
	private byte[] uinData;

	/** Integrity hash of decrypted {@code uin_data} payload. */
	@Column(name = "uin_data_hash")
	private String uinDataHash;

	/** Registration ID (RID) from registration processor. */
	@Column(name = "reg_id")
	private String regId;
	
	/** Biometric reference id linking CBEFF rows. */
	@Column(name = "bio_ref_id")
	private String bioRefId;

	/** Identity lifecycle status (ACTIVATED, BLOCKED, etc.). */
	@Column(name = "status_code")
	private String statusCode;

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

	/**
	 * @return uin data
	 */
	public byte[] getUinData() {
		return uinData.clone();
	}

	/**
	 * Sets the uin data.
	 *
	 * @param uinData the new uin data
	 */
	public void setUinData(byte[] uinData) {
		this.uinData = uinData.clone();
	}

	@Override
	/**
	 * @return id
	 */
	public String getId() {
		return uinRefId;
	}

	@Override
	/**
	 * @return whether new
	 */
	public boolean isNew() {
		return true;
	}

}
