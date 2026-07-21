package io.mosip.idrepository.identity.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "channel_info", schema = "idrepo")
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelInfo {
	
	@Id
	/** Hashed channel ({@code hashed_channel} column). */
	@Column(name = "hashed_channel")
	private String hashedChannel;
	
	/** Channel type ({@code channel_type} column). */
	@Column(name = "channel_type")
	private String channelType;
	
	/** No of records ({@code no_of_records} column). */
	@Column(name = "no_of_records")
	private Integer noOfRecords;
	
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

}
