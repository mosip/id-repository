package io.mosip.idrepository.vid.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Virtual ID row mapped to {@code idmap.vid}.
 * <p>
 * Links a generated VID to a UIN hash; {@link #uin} is encrypted by
 * {@link io.mosip.idrepository.vid.interceptor.IdRepoVidEntityInterceptor} on persist.
 * </p>
 *
 * @author Prem Kumar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vid", schema = "idmap")
@Entity
public class Vid implements Comparable<Vid> {

	/** Primary key — VID row identifier ({@code id} column). */
	@Id
	@Column(name = "id")
	private String id;

	/** Virtual ID token (encrypted at rest in {@code idmap.vid}). */
	@Column(name = "vid")
	private String vid;
	
	/** SHA-256 hash of linked UIN. */
	@Column(name = "uin_hash")
	private String uinHash;
	
	/** Encrypted UIN token linked to this VID. */
	@Column(name = "uin")
	private String uin;

	/** VID type (PERPETUAL, TEMPORARY, etc.). */
	@Column(name = "vidtyp_code")
	private String vidTypeCode;

	/** When the VID was generated. */
	@Column(name = "generated_dtimes")
	private LocalDateTime generatedDTimes;

	/** VID expiry; {@code null} for non-expiring types. */
	@Column(name = "expiry_dtimes")
	private LocalDateTime expiryDTimes;

	/** VID lifecycle status (ACTIVE, EXPIRED, etc.). */
	@Column(name = "status_code")
	private String statusCode;

	/** Audit — user or service that created the VID row. */
	@Column(name = "cr_by")
	private String createdBy;

	/** Audit — row creation timestamp (UTC). */
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDTimes;

	/** Audit — last updater. */
	@Column(name = "upd_by", nullable = true)
	private String updatedBy;

	/** Audit — last update timestamp (UTC). */
	@Column(name = "upd_dtimes", nullable = true)
	private LocalDateTime updatedDTimes;

	/** Soft-delete flag. */
	@Column(name="is_deleted", nullable = true)
	private boolean isDeleted;

	/** Soft-delete timestamp (UTC). */
	@Column(name = "del_dtimes", nullable = true)
	private LocalDateTime deletedDTimes;

	/**
	 * Orders VIDs by creation time (newest first when used in descending sort).
	 *
	 * @param vid other VID to compare against
	 * @return comparison result by {@link #createdDTimes}
	 */
	@Override
	public int compareTo(Vid vid) {
		return vid.getCreatedDTimes().compareTo(createdDTimes);
	}

}