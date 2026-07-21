package io.mosip.idrepository.identity.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-auth-type lock state for a UIN ({@code idrepo.uin_auth_lock}).
 * <p>
 * Updated by {@link io.mosip.idrepository.identity.service.impl.AuthTypeStatusImpl}.
 * </p>
 */
@NoArgsConstructor
@Data
@Table(name = "uin_auth_lock", schema = "idrepo")
@Entity
@IdClass(AuthtypeLock.Compositeclass.class)
public class AuthtypeLock {

	@NotNull
	/** SHA-256 hash of UIN used for indexed lookup without decryption. */
	@Column(name = "uin_hash")
	private String hashedUin;

	@Id
	@NotNull
	/** Authtypecode ({@code auth_type_code} column). */
	@Column(name = "auth_type_code")
	private String authtypecode;

	@Id
	@NotNull
	/** Lockrequest dttimes ({@code lock_request_datetime} column). */
	@Column(name = "lock_request_datetime")
	private LocalDateTime lockrequestDTtimes;

	@NotNull
	/** Lockstart dttimes ({@code lock_start_datetime} column). */
	@Column(name = "lock_start_datetime")
	private LocalDateTime lockstartDTtimes;

	/** Lockend dttimes ({@code lock_end_datetime} column). */
	@Column(name = "lock_end_datetime")
	private LocalDateTime lockendDTtimes;

	/** Unlock expiry dttimes ({@code unlock_expiry_datetime} column). */
	@Column(name = "unlock_expiry_datetime")
	private LocalDateTime unlockExpiryDTtimes;

	@NotNull
	/** Identity lifecycle status (ACTIVATED, BLOCKED, etc.). */
	@Column(name = "status_code")
	private String statuscode;

	@NotNull
	@Size(max = 3)
	/** Preferred language code for identity attributes. */
	@Column(name = "lang_code")
	private String langCode;

	@NotNull
	/** Audit — creator user or service id. */
	@Column(name = "cr_by")
	private String createdBy;

	@NotNull
	/** Audit — row creation timestamp (UTC). */
	@Column(name = "cr_dtimes")
	private LocalDateTime crDTimes;

	/** Audit — last updater user or service id. */
	@Column(name = "upd_by")
	private String updatedBy;

	/** Audit — last update timestamp (UTC). */
	@Column(name = "upd_dtimes")
	private LocalDateTime updDTimes;

	/** Soft-delete flag. */
	@Column(name = "is_deleted")
	private boolean isDeleted;

	/** Soft-delete timestamp (UTC). */
	@Column(name = "del_dtimes")
	/** Del dtimes. */
	private LocalDateTime delDTimes;

	/**
	 * Instantiates a new compositeclass.
	 */
	@Data
	static class Compositeclass implements Serializable {
		
		private static final long serialVersionUID = -2748591036581927460L;
		
		private String hashedUin;
		
		private String authtypecode;
		
		private LocalDateTime lockrequestDTtimes;
	}
	
	/**
	 * The constructor used in retrieval of the specific fields.
	 * 
	 * @param authtypecode
	 * @param statuscode
	 */
	public AuthtypeLock(String authtypecode,  String statuscode, LocalDateTime unlockExpiryDTtimes) {
		this.authtypecode = authtypecode;
		this.statuscode = statuscode;
		this.unlockExpiryDTtimes = unlockExpiryDTtimes;
	}
	
	

}
