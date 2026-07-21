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

/**
 * JPA entity for the {@code idrepo.credential_request_status} table.
 *
 * <p>
 * Tracks per-partner credential issuance requests for an individual (UIN or VID).
 * Rows are created when identity is activated or updated and consumed by the
 * credential status / request pipeline, which polls lifecycle states such as
 * {@link io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle#NEW}
 * and hands work off to credential-request processing.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Acts as the durable queue and status ledger for partner-bound credential
 * issuance. Each row represents one {@code (individual, partner)} issuance
 * intent, including request id, token binding, trigger action, transaction
 * limits, and expiry metadata used when building outbound credential requests.
 * </p>
 *
 * <h2>Table / persistence unit</h2>
 * <ul>
 *   <li>Schema / table: {@code mosip_idrepo.credential_request_status}
 *       ({@code @Table(..., schema = "idrepo")})</li>
 *   <li>Persistence unit: PU1 ({@code mosip_idrepo}) via
 *       {@code idRepoDataSource} / {@code IdRepoDataSourceConfig}</li>
 *   <li>Composite primary key: {@link #individualIdHash} + {@link #partnerId}
 *       ({@link Compositeclass})</li>
 *   <li>Entity is registered only when bean {@code idRepoDataSource} exists
 *       ({@link ConditionalOnBean})</li>
 * </ul>
 *
 * <h2>Salt / identifier notes</h2>
 * <p>
 * {@link #individualId} holds the plain identifier (UIN/VID) for outbound
 * credential requests. {@link #individualIdHash} is the salted hash used for
 * lookups and uniqueness. Hashing uses idrepo salt tables
 * ({@code uin_hash_salt}) via {@link io.mosip.idrepository.core.security.IdRepoSecurityManager}
 * — not idmap / VID salt repositories. Do not confuse this entity with
 * credential-store rows on {@code mosip_credential}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link io.mosip.idrepository.core.repository.CredentialRequestStatusRepo}
 *       — create, poll ({@code FOR UPDATE SKIP LOCKED}), and update status</li>
 *   <li>{@code CredentialStatusManager} — scheduled handler that claims
 *       {@code NEW} / stale {@code REQUESTED} rows</li>
 *   <li>Identity lifecycle paths that enqueue partner credentials on activate/update</li>
 *   <li>Credential-request pipeline (in-process or HTTP) that consumes
 *       {@link #requestId} and related fields</li>
 * </ul>
 *
 * <h2>IDA note</h2>
 * <p>
 * ID Authentication does <strong>not</strong> read this table or id-repo salt
 * tables. IDA observes credential outcomes via WebSub / Datashare / REST. Keep
 * status and payload contracts stable for downstream partners; this entity is
 * an internal id-repository ledger.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.repository.CredentialRequestStatusRepo
 * @see io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle
 * @see io.mosip.idrepository.core.constant.CredentialTriggerAction
 * @see io.mosip.idrepository.manager.CredentialStatusManager
 */
@NoArgsConstructor
@Data
@Table(name = "credential_request_status", schema = "idrepo")
@Entity
@IdClass(CredentialRequestStatus.Compositeclass.class)
@ConditionalOnBean(name = { "idRepoDataSource" })
public class CredentialRequestStatus {

	/**
	 * Plain individual identifier (UIN or VID) sent to the credential pipeline.
	 * <p>
	 * Stored for outbound issuance; lookups and uniqueness typically use
	 * {@link #individualIdHash} instead.
	 * </p>
	 */
	@Column(name = "individual_id")
	private String individualId;

	/**
	 * Composite PK part — salted hash of the individual identifier.
	 * <p>
	 * Derived with idrepo hash salt ({@code uin_hash_salt}) so the same
	 * individual maps to a stable key across partners.
	 * </p>
	 */
	@Id
	@Column(name = "individual_id_hash")
	private String individualIdHash;

	/**
	 * Composite PK part — credential partner (MISP) identifier.
	 * <p>
	 * Together with {@link #individualIdHash}, uniquely identifies one
	 * per-partner issuance row.
	 * </p>
	 */
	@Id
	@Column(name = "partner_id")
	private String partnerId;

	/**
	 * Unique request identifier for idempotent credential issuance.
	 * <p>
	 * Propagated into the credential-request pipeline so retries can be
	 * correlated without creating duplicate partner credentials.
	 * </p>
	 */
	@Column(name = "request_id")
	private String requestId;

	/**
	 * Token reference used when the credential is bound to a VID.
	 * <p>
	 * May be {@code null} for UIN-only issuance paths.
	 * </p>
	 */
	@Column(name = "token_id")
	private String tokenId;

	/**
	 * Current lifecycle state of this issuance row.
	 * <p>
	 * Values align with
	 * {@link io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle}
	 * (e.g. {@code NEW}, {@code REQUESTED}, completed / failed states).
	 * </p>
	 */
	@Column(name = "status")
	private String status;

	/**
	 * Action that triggered credential issuance for this row.
	 * <p>
	 * See {@link io.mosip.idrepository.core.constant.CredentialTriggerAction}
	 * (e.g. create / update / status-driven triggers).
	 * </p>
	 */
	@Column(name = "trigger_action")
	private String triggerAction;

	/**
	 * Maximum number of authentication transactions allowed for the issued
	 * credential, when applicable.
	 */
	@Column(name = "id_transaction_limit")
	private Integer idTransactionLimit;

	/**
	 * Expiry timestamp for time-bound credentials (e.g. VID-bound).
	 * <p>
	 * Used by reprocessing / projection queries that need expiry without
	 * loading the full entity graph.
	 * </p>
	 */
	@Column(name = "id_expiry_timestamp")
	private LocalDateTime idExpiryTimestamp;

	/**
	 * Audit: user or system that created this row.
	 */
	@NotNull
	@Column(name = "cr_by")
	private String createdBy;

	/**
	 * Audit: UTC timestamp when this row was created.
	 */
	@NotNull
	@Column(name = "cr_dtimes")
	private LocalDateTime crDTimes;

	/**
	 * Audit: user or system that last updated this row.
	 */
	@Column(name = "upd_by")
	private String updatedBy;

	/**
	 * Audit: UTC timestamp of the last update.
	 */
	@Column(name = "upd_dtimes")
	private LocalDateTime updDTimes;

	/**
	 * Soft-delete flag; deleted rows are excluded by default repository queries.
	 */
	@Column(name = "is_deleted")
	private boolean isDeleted;

	/**
	 * UTC timestamp when this row was soft-deleted.
	 */
	@Column(name = "del_dtimes")
	private LocalDateTime delDTimes;

	/**
	 * Projection constructor used by JPQL queries that fetch a subset of columns
	 * for credential reprocessing.
	 * <p>
	 * Populates only the fields needed to rebuild an outbound credential request
	 * without loading audit / status columns.
	 * </p>
	 *
	 * @param individualId       plain individual identifier
	 * @param idExpiryTimestamp  credential expiry timestamp
	 * @param idTransactionLimit authentication transaction limit
	 * @param tokenId            VID token reference
	 * @param partnerId          credential partner identifier
	 */
	public CredentialRequestStatus(String individualId, LocalDateTime idExpiryTimestamp, Integer idTransactionLimit,
			String tokenId, String partnerId) {
		this.individualId = individualId;
		this.idExpiryTimestamp = idExpiryTimestamp;
		this.idTransactionLimit = idTransactionLimit;
		this.tokenId = tokenId;
		this.partnerId = partnerId;
	}

	/**
	 * Composite primary key class for {@code (individual_id_hash, partner_id)}.
	 *
	 * <p>
	 * Required by JPA {@link IdClass} mapping on {@link CredentialRequestStatus}.
	 * Field names and types must match the {@code @Id} fields on the entity.
	 * </p>
	 *
	 * <h2>Purpose</h2>
	 * <p>
	 * Encapsulates the natural key of one per-partner credential request status
	 * row so Spring Data / JPA can load and merge by composite identity.
	 * </p>
	 *
	 * @see CredentialRequestStatus
	 */
	@Data
	static class Compositeclass implements Serializable {

		/** Serialization id for the composite key type. */
		private static final long serialVersionUID = -5429439416551847211L;

		/**
		 * Composite PK part — salted hash of the individual identifier.
		 * <p>
		 * Must match {@link CredentialRequestStatus#individualIdHash}.
		 * </p>
		 */
		public String individualIdHash;

		/**
		 * Composite PK part — credential partner identifier.
		 * <p>
		 * Must match {@link CredentialRequestStatus#partnerId}.
		 * </p>
		 */
		public String partnerId;
	}
}
