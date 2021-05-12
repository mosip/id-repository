package io.mosip.idrepository.core.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Table(name = "credential_request_status", schema = "idrepo")
@Entity
@IdClass(CredentialRequestStatus.Compositeclass.class)
@ConditionalOnBean(name = { "idRepoDataSource" })
public class CredentialRequestStatus {

	public String individualId;

	@Id
	private String individualIdHash;

	@Id
	private String partnerId;

	private String requestId;

	private String tokenId;

	private String status;

	private Integer idTransactionLimit;

	private LocalDateTime idExpiryTimestamp;

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
	
	public CredentialRequestStatus(String individualId, LocalDateTime idExpiryTimestamp,Integer idTransactionLimit, String tokenId, String partnerId ) {
		this.individualId =  individualId;
		this.idExpiryTimestamp = idExpiryTimestamp;
		this.idTransactionLimit = idTransactionLimit;
		this.tokenId = tokenId;
		this.partnerId = partnerId;
	}

	@Data
	static class Compositeclass implements Serializable {

		private static final long serialVersionUID = -5429439416551847211L;

		public String individualIdHash;

		public String partnerId;
	}

}
