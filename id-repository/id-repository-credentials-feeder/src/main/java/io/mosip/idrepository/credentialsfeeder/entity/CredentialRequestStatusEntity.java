package io.mosip.idrepository.credentialsfeeder.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Class CredentialRequestStatusEntity {.
 *
 * @author Loganathan Sekar
 */
@Entity
@Table(name = "credential_request_status", schema = "idrepo")
@IdClass(CredentialRequestStatusEntity.CompositeKey.class)
@Data
@NoArgsConstructor
public class CredentialRequestStatusEntity {

	@Id
	@Column(name = "individual_id_hash", updatable = false, nullable = false, unique = true)
	private String individualIdHash;
	
	@Id
	@Column(name = "partner_id", updatable = false, nullable = false, unique = true)
	private String partnerId;

	@Column(name = "individual_id", updatable = false, nullable = false, unique = true)
	private String individualId;
	
	@Column(name = "request_id", updatable = true, nullable = false, unique = false)
	private String requestId;
	
	@Column(name = "token_id", updatable = true, nullable = false, unique = false)
	private String tokenId;

	@Column(name = "status", updatable = true, nullable = false, unique = false)
	private String status;
	
	@Column(name = "id_expiry_timestamp", updatable = true, nullable = true, unique = false)
	private LocalDateTime idExpiryDtimes;
	
	@Column(name = "id_transaction_limit", updatable = true, nullable = true, unique = false)
	private Integer idTransactionLimit;
	
	/** The created by. */
	@Column(name = "cr_by", updatable = true, nullable = false, unique = false)
	private String createdBy;

	/** The create dtimes. */
	@Column(name = "cr_dtimes", updatable = true, nullable = false, unique = true)
	private LocalDateTime createDtimes;

	/** The updated by. */
	@Column(name = "upd_by", updatable = true, nullable = true, unique = false)
	private String updatedBy;

	/** The updated dtimes. */
	@Column(name = "upd_dtimes", updatable = true, nullable = true, unique = true)
	private LocalDateTime updatedDtimes;
	
	
	public CredentialRequestStatusEntity(String individualId, LocalDateTime idExpiryDtimes, Integer idTransactionLimit, String tokenId, String partnerId) {
		this.individualId = individualId;
		this.idExpiryDtimes = idExpiryDtimes;
		this.idTransactionLimit = idTransactionLimit;
		this.tokenId = tokenId;
		this.partnerId = partnerId;
	}
	
	@Data
	public static class CompositeKey implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6021483025397857224L;
		
		private String individualIdHash;
		private String partnerId;
	}

}
