package io.mosip.idrepository.core.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

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

	@Column(name = "individual_id")
	private String individualId;

	@Id
	@Column(name = "individual_id_hash")
	private String individualIdHash;

	@Id
	@Column(name = "partner_id")
	private String partnerId;

	@Column(name = "request_id")
	private String requestId;

	@Column(name = "token_id")
	private String tokenId;

	@Column(name = "status")
	private String status;

	private String triggerAction;

    @Column(name = "id_transaction_limit")
	private Integer idTransactionLimit;

	@Column(name = "id_expiry_timestamp")
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
