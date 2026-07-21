/*
 * 
 */
package io.mosip.idrepository.credential.request.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping rows in the {@code credential_transaction} table of the
 * {@code mosip_credential} database.
 * <p>
 * Each row represents one credential-issue request in the batch queue. The
 * {@link #request} column holds the serialized {@code CredentialIssueRequestDto}
 * JSON, which is transparently encrypted and decrypted at persistence time by
 * {@link io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor}.
 * Status transitions ({@link #statusCode}, {@link #statusComment}) are driven by
 * Spring Batch tasklets and the credential-service response.
 * </p>
 *
 * @see io.mosip.idrepository.credential.request.repository.CredentialRepository
 * @see io.mosip.idrepository.credential.request.constant.CredentialStatusCode
 */
@Entity
@Table(name = "credential_transaction")
public class CredentialEntity {

	/**
	 * Primary key — unique credential-request identifier.
	 * Maps to column {@code id}.
	 */
	@Id
	@Column(name = "id", nullable = false)
	private String requestId;

	/**
	 * Identifier of the issued credential returned by credential-service on success.
	 * Maps to column {@code credential_id}.
	 */
	@Column(name = "credential_id")
	private String credentialId;

	/**
	 * Serialized credential-issue request JSON (encrypted at rest in the database).
	 * Maps to column {@code request}.
	 */
	@Column(name = "request")
	private String request;

	/**
	 * Queue processing status code.
	 * Maps to column {@code status_code}; values defined in
	 * {@link io.mosip.idrepository.credential.request.constant.CredentialStatusCode}.
	 */
	@Column(name = "status_code", nullable = false)
	private String statusCode;

	/**
	 * Human-readable status description or failure reason from batch processing.
	 * Maps to column {@code status_comment}.
	 */
	@Column(name = "status_comment")
	private String statusComment;

	/**
	 * Datashare URL pointing to the issued credential payload, when available.
	 * Maps to column {@code datashareurl}.
	 */
	@Column(name = "datashareurl")
	private String dataShareUrl;

	/**
	 * Timestamp when the credential was successfully issued.
	 * Maps to column {@code issuancedate}.
	 */
	@Column(name = "issuancedate")
	private LocalDateTime issuanceDate;

	/**
	 * Digital signature over the credential payload produced during issuance.
	 * Maps to column {@code signature}.
	 */
	@Column(name = "signature")
	private String signature;

	/**
	 * Number of batch reprocess attempts for this queue row.
	 * Maps to column {@code trn_retry_count}.
	 */
	@Column(name = "trn_retry_count")
	private Integer retryCount;

	/**
	 * Audit field — user or service principal that created the row.
	 * Maps to column {@code cr_by}.
	 */
	@Column(name = "cr_by")
	private String createdBy;

	/**
	 * Audit field — row creation timestamp in UTC.
	 * Maps to column {@code cr_dtimes}; not updatable after insert.
	 */
	@Column(name = "cr_dtimes", updatable = false)
	private LocalDateTime createDateTime;

	/**
	 * Audit field — user or service principal that last updated the row.
	 * Maps to column {@code upd_by}.
	 */
	@Column(name = "upd_by")
	private String updatedBy;

	/**
	 * Audit field — last update timestamp in UTC.
	 * Maps to column {@code upd_dtimes}.
	 */
	@Column(name = "upd_dtimes")
	private LocalDateTime updateDateTime;

	/**
	 * Soft-delete flag; {@code true} when the row is logically deleted.
	 * Maps to column {@code is_deleted}.
	 */
	@Column(name = "is_deleted")
	private Boolean isDeleted;

	/**
	 * Soft-delete timestamp in UTC, set when {@link #isDeleted} is {@code true}.
	 * Maps to column {@code del_dtimes}.
	 */
	@Column(name = "del_dtimes")
	private LocalDateTime deletedDateTime;

	/**
	 * Returns the credential-request identifier ({@code id} column).
	 *
	 * @return primary key value used as request id across the credential pipeline
	 */
	public String getRequestId() {
		return requestId;
	}

	/**
	 * Sets the credential-request identifier ({@code id} column).
	 *
	 * @param requestId unique request id assigned at queue insert time
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * Returns the serialized credential-issue request JSON ({@code request} column).
	 * <p>
	 * When loaded through Hibernate, the value is decrypted by
	 * {@link io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor}.
	 * </p>
	 *
	 * @return plaintext or encrypted request payload depending on load path
	 */
	public String getRequest() {
		return request;
	}

	/**
	 * Sets the serialized credential-issue request JSON ({@code request} column).
	 *
	 * @param request credential-issue request JSON; encrypted on persist by the interceptor
	 */
	public void setRequest(String request) {
		this.request = request;
	}

	/**
	 * Returns the queue processing status code ({@code status_code} column).
	 *
	 * @return status code from {@link io.mosip.idrepository.credential.request.constant.CredentialStatusCode}
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets the queue processing status code ({@code status_code} column).
	 *
	 * @param statusCode new status code for batch or API processing
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Returns the batch reprocess attempt count ({@code trn_retry_count} column).
	 *
	 * @return number of times this row has been retried by the reprocess job
	 */
	public Integer getRetryCount() {
		return retryCount;
	}

	/**
	 * Sets the batch reprocess attempt count ({@code trn_retry_count} column).
	 *
	 * @param retryCount updated retry counter after a reprocess attempt
	 */
	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

	/**
	 * Returns the audit creator id ({@code cr_by} column).
	 *
	 * @return user or service that created the row
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * Sets the audit creator id ({@code cr_by} column).
	 *
	 * @param createdBy user or service principal performing the insert
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * Returns the row creation timestamp ({@code cr_dtimes} column).
	 *
	 * @return UTC creation time
	 */
	public LocalDateTime getCreateDateTime() {
		return createDateTime;
	}

	/**
	 * Sets the row creation timestamp ({@code cr_dtimes} column).
	 *
	 * @param createDateTime UTC creation time
	 */
	public void setCreateDateTime(LocalDateTime createDateTime) {
		this.createDateTime = createDateTime;
	}

	/**
	 * Returns the audit last-updater id ({@code upd_by} column).
	 *
	 * @return user or service that last modified the row
	 */
	public String getUpdatedBy() {
		return updatedBy;
	}

	/**
	 * Sets the audit last-updater id ({@code upd_by} column).
	 *
	 * @param updatedBy user or service principal performing the update
	 */
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	/**
	 * Returns the last update timestamp ({@code upd_dtimes} column).
	 *
	 * @return UTC time of the most recent update
	 */
	public LocalDateTime getUpdateDateTime() {
		return updateDateTime;
	}

	/**
	 * Sets the last update timestamp ({@code upd_dtimes} column).
	 *
	 * @param updateDateTime UTC time of the update
	 */
	public void setUpdateDateTime(LocalDateTime updateDateTime) {
		this.updateDateTime = updateDateTime;
	}

	/**
	 * Returns the soft-delete flag ({@code is_deleted} column).
	 *
	 * @return {@code true} if the row is logically deleted
	 */
	public Boolean getIsDeleted() {
		return isDeleted;
	}

	/**
	 * Sets the soft-delete flag ({@code is_deleted} column).
	 *
	 * @param isDeleted {@code true} to mark the row as deleted
	 */
	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	/**
	 * Returns the soft-delete timestamp ({@code del_dtimes} column).
	 *
	 * @return UTC time when the row was soft-deleted, or {@code null} if active
	 */
	public LocalDateTime getDeletedDateTime() {
		return deletedDateTime;
	}

	/**
	 * Sets the soft-delete timestamp ({@code del_dtimes} column).
	 *
	 * @param deletedDateTime UTC deletion time
	 */
	public void setDeletedDateTime(LocalDateTime deletedDateTime) {
		this.deletedDateTime = deletedDateTime;
	}

	/**
	 * Returns the issued credential identifier ({@code credential_id} column).
	 *
	 * @return credential id from credential-service after successful issuance
	 */
	public String getCredentialId() {
		return credentialId;
	}

	/**
	 * Sets the issued credential identifier ({@code credential_id} column).
	 *
	 * @param credentialId credential id returned on successful issuance
	 */
	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}

	/**
	 * Returns the datashare URL ({@code datashareurl} column).
	 *
	 * @return URL of the issued credential payload in datashare, if applicable
	 */
	public String getDataShareUrl() {
		return dataShareUrl;
	}

	/**
	 * Sets the datashare URL ({@code datashareurl} column).
	 *
	 * @param dataShareUrl datashare location of the issued credential
	 */
	public void setDataShareUrl(String dataShareUrl) {
		this.dataShareUrl = dataShareUrl;
	}

	/**
	 * Returns the credential issuance timestamp ({@code issuancedate} column).
	 *
	 * @return UTC time when the credential was issued
	 */
	public LocalDateTime getIssuanceDate() {
		return issuanceDate;
	}

	/**
	 * Sets the credential issuance timestamp ({@code issuancedate} column).
	 *
	 * @param issuanceDate UTC issuance time
	 */
	public void setIssuanceDate(LocalDateTime issuanceDate) {
		this.issuanceDate = issuanceDate;
	}

	/**
	 * Returns the digital signature ({@code signature} column).
	 *
	 * @return signature over the credential payload
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Sets the digital signature ({@code signature} column).
	 *
	 * @param signature signature produced during credential issuance
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * Returns the status comment ({@code status_comment} column).
	 *
	 * @return human-readable status or error description
	 */
	public String getStatusComment() {
		return statusComment;
	}

	/**
	 * Sets the status comment ({@code status_comment} column).
	 *
	 * @param statusComment descriptive status or failure message
	 */
	public void setStatusComment(String statusComment) {
		this.statusComment = statusComment;
	}

}
