package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import io.mosip.idrepository.core.entity.UinInfo;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;
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

	/** The uin ref id. */
	@Id
	@Column(insertable = false, updatable = false, nullable = false)
	private String uinRefId;

	/** The uin. */
	private String uin;

	private String uinHash;

	/** The uin data. */
	@Lob
	@Type(type = "org.hibernate.type.BinaryType")
	@Basic(fetch = FetchType.LAZY)
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private byte[] uinData;

	/** The uin data hash. */
	private String uinDataHash;

	/** The reg id. */
	private String regId;

	private String bioRefId;

	/** The status code. */
	private String statusCode;
	
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

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	@NotFound(action = NotFoundAction.IGNORE)
	private List<UinBiometric> biometrics;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "uin", cascade = CascadeType.ALL)
	@NotFound(action = NotFoundAction.IGNORE)
	private List<UinDocument> documents;

	/**
	 * Gets the uin data.
	 *
	 * @return the uin data
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
	public String getUin() {
		return uin;
	}

	@Override
	public void setUin(String uin) {
		this.uin = uin;
	}

	@Override
	public String getId() {
		return uinRefId;
	}

	@Override
	public boolean isNew() {
		return true;
	}
}
