package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.domain.Persistable;

import io.mosip.idrepository.core.entity.UinInfo;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Primary UIN aggregate mapped to {@code idrepo.uin}.
 * <p>
 * Root entity for resident identity: encrypted {@link #uinData}, linked {@link UinBiometric}
 * and {@link UinDocument} children, and lifecycle {@link #statusCode}. {@link #uinRefId} is the
 * salt-shard key; demographic updates flow through {@link io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl}.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.entity.UinInfo
 */
@Getter
@Setter
@ToString(exclude = { "biometrics", "documents" })
@Entity
@NoArgsConstructor
@Table(schema = "idrepo")
public class Uin implements Persistable<String>, UinInfo {

	public Uin(String uinRefId, String uin, String uinHash, byte[] uinData, String uinDataHash, String regId,
			String statusCode, String createdBy, LocalDateTime createdDateTime,
			String updatedBy, LocalDateTime updatedDateTime, Boolean isDeleted, LocalDateTime deletedDateTime,
			List<UinBiometric> biometrics, List<UinDocument> documents) {
		this.uinRefId = uinRefId;
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

	/** Salt-shard reference id (primary key; maps to {@code uin_ref_id}). */
	@Id
	@Column(name="uin_ref_id", insertable = false, updatable = false, nullable = false)
	private String uinRefId;

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
	@Column(name="uin_data", nullable = false)
	private byte[] uinData;

	/** Integrity hash of decrypted {@code uin_data} payload. */
	@Column(name="uin_data_hash")
	private String uinDataHash;

	/** Registration ID (RID) from registration processor. */
	@Column(name="reg_id")
	private String regId;
	
	/** Biometric reference id linking CBEFF rows. */
	@Column(name="bio_ref_id")
	private String bioRefId;

	/** Identity lifecycle status (ACTIVATED, BLOCKED, etc.). */
	@Column(name="status_code")
	private String statusCode;
	
	/** Preferred language code for identity attributes. */
	@Column(name="lang_code")
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
	@Column(name="is_deleted")
	private Boolean isDeleted;

	/** Soft-delete timestamp (UTC). */
	@Column(name = "del_dtimes")
	private LocalDateTime deletedDateTime;

	/** Child biometric rows (CBEFF) for this UIN. */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	private List<UinBiometric> biometrics;

	/** Child proof-of-identity document rows for this UIN. */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	private List<UinDocument> documents;

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
