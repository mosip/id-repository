package io.mosip.idrepository.identity.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import io.mosip.idrepository.core.entity.UinInfo;
import org.springframework.data.domain.Persistable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Staged identity draft before activation ({@code idrepo.uin_draft}).
 * <p>
 * Holds in-progress demographic/biometric/document changes until publish via draft APIs.
 * </p>
 *
 * @see io.mosip.idrepository.identity.service.impl.IdRepoDraftServiceImpl
 */
@Getter
@Setter
@ToString(exclude = { "biometrics", "documents" })
@Entity
@NoArgsConstructor
@Table(schema = "idrepo", name = "uin_draft")
public class UinDraft implements Persistable<String>, UinInfo, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8675162242795264386L;

	public UinDraft(String uin, String uinHash, byte[] uinData, String uinDataHash, String regId, String statusCode,
			String createdBy, LocalDateTime createdDateTime, String updatedBy, LocalDateTime updatedDateTime,
			Boolean isDeleted, LocalDateTime deletedDateTime, List<UinBiometricDraft> biometrics,
			List<UinDocumentDraft> documents) {
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
		this.biometrics = biometrics;
		this.documents = documents;
	}

	@Id
	/** Registration ID (RID) from registration processor. */
	@Column(name="reg_id", insertable = false, updatable = false, nullable = false)
	private String regId;

	/** Tokenized UIN stored for lookup (encrypted at rest). */
	@Column(name="uin")
	private String uin;

	/** SHA-256 hash of UIN used for indexed lookup without decryption. */
	@Column(name="uin_hash")
	private String uinHash;

	/** Encrypted demographic identity JSON blob ({@code uin_data}). */
	@Basic(fetch = FetchType.LAZY)
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Column(name="uin_data")
	private byte[] uinData;

	/** Integrity hash of decrypted {@code uin_data} payload. */
	@Column(name="uin_data_hash")
	private String uinDataHash;

	/** Identity lifecycle status (ACTIVATED, BLOCKED, etc.). */
	@Column(name="status_code")
	private String statusCode;
	
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

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	/** Biometrics (List<UinBiometricDraft>). */
	private List<UinBiometricDraft> biometrics;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	/** Documents (List<UinDocumentDraft>). */
	private List<UinDocumentDraft> documents;

	/**
	 * @return uin data
	 */
	@Override
	/**
	 * @return uin data
	 */
	public byte[] getUinData() {
		return uinData;
	}

	/**
	 * Sets the uin data.
	 *
	 * @param uinData the new uin data
	 */
	@Override
	/**
	 * @param uinData uin data
	 */
	public void setUinData(byte[] uinData) {
		this.uinData = uinData;
	}

	@Override
	/**
	 * @return uin
	 */
	public String getUin() {
		return uin;
	}

	@Override
	/**
	 * @param uin uin
	 */
	public void setUin(String uin) {
		this.uin = uin;
	}

	@Override
	/**
	 * @return id
	 */
	public String getId() {
		return regId;
	}

	@Override
	/**
	 * @return whether new
	 */
	public boolean isNew() {
		return true;
	}
}
