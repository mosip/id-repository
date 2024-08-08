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
 * The Class Uin -Entity class for uin table.
 *
 * @author Manoj SP
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
	@Column(name="reg_id", insertable = false, updatable = false, nullable = false)
	private String regId;

	/** The uin. */
	@Column(name="uin")
	private String uin;

	@Column(name="uin_hash")
	private String uinHash;

	/** The uin data. */
	@Basic(fetch = FetchType.LAZY)
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Column(name="uin_data")
	private byte[] uinData;

	/** The uin data hash. */
	@Column(name="uin_data_hash")
	private String uinDataHash;

	/** The status code. */
	@Column(name="status_code")
	private String statusCode;
	
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

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	private List<UinBiometricDraft> biometrics;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	private List<UinDocumentDraft> documents;

	/**
	 * Gets the uin data.
	 *
	 * @return the uin data
	 */
	@Override
	public byte[] getUinData() {
		return uinData;
	}

	/**
	 * Sets the uin data.
	 *
	 * @param uinData the new uin data
	 */
	@Override
	public void setUinData(byte[] uinData) {
		this.uinData = uinData;
	}

	@Override
	public String getUin() {
		return uin;
	}

	@Override
	public void setUin(String uin) {
		this.uin = uin;
	}

	@Override
	public String getId() {
		return regId;
	}

	@Override
	public boolean isNew() {
		return true;
	}
}
