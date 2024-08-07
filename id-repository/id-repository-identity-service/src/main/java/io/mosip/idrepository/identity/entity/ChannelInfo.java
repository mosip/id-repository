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
	@Column(name = "hashed_channel")
	private String hashedChannel;
	
	@Column(name = "channel_type")
	private String channelType;
	
	@Column(name = "no_of_records")
	private Integer noOfRecords;
	
	@NotNull
	@Column(name = "cr_by")
	private String createdBy;

	@NotNull
	@Column(name = "cr_dtimes")
	private LocalDateTime crDTimes;

	@Column(name = "upd_by")
	private String updatedBy;

	@Column(name = "upd_dtimes")
	private LocalDateTime updDTimes;

	@Column(name = "is_deleted")
	private boolean isDeleted;

	@Column(name = "del_dtimes")
	private LocalDateTime delDTimes;

}
