package io.mosip.idrepository.core.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

/**
 * Spring Data repository for {@link CredentialRequestStatus} rows in
 * {@code idrepo.credential_request_status}.
 *
 * <p>
 * Tracks per-partner credential issuance state for an individual (UIN/VID). Primary
 * consumers create/update rows on identity lifecycle events and poll {@code NEW} /
 * stale {@code REQUESTED} rows for issuance and reprocessing.
 * </p>
 *
 * <h2>Persistence unit</h2>
 * <p>
 * Enabled only when bean {@code idRepoDataSource} exists
 * ({@link ConditionalOnBean}). Bound to PU1 ({@code mosip_idrepo}).
 * </p>
 *
 * <h2>Concurrency</h2>
 * <p>
 * {@link #findByStatus(String, int)} uses native {@code SELECT … FOR UPDATE SKIP LOCKED}
 * so multiple job replicas can claim disjoint batches without blocking on rows already
 * locked by another pod. Prefer that method for multi-replica polling; the derived
 * {@link #findByStatus(String)} does <strong>not</strong> lock rows.
 * </p>
 *
 * <h2>Soft delete</h2>
 * <p>
 * Methods ending in {@code AndIsDeleted} accept an explicit flag. Default overloads
 * without the flag filter {@code is_deleted = false} (active rows only).
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * // claim NEW rows for processing
 * List&lt;CredentialRequestStatus&gt; batch =
 *     statusRepo.findByStatus(CredentialRequestStatusLifecycle.NEW.toString(), 50);
 *
 * // lookup by hash + partner
 * Optional&lt;CredentialRequestStatus&gt; row =
 *     statusRepo.findByIndividualIdHashAndPartnerId(idHash, partnerId);
 * </pre>
 *
 * @author Manoj SP
 * @see CredentialRequestStatus
 * @see CredentialRequestStatusLifecycle
 * @see IdRepoSecurityManager#ID_HASH
 */
@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface CredentialRequestStatusRepo extends JpaRepository<CredentialRequestStatus, String> {

	/**
	 * Finds rows by plain individual id, optionally including soft-deleted rows.
	 *
	 * @param individualId plain UIN/VID as stored in the table
	 * @param isDeleted    soft-delete flag filter ({@code true} = deleted only,
	 *                     {@code false} = active only)
	 * @return matching status rows (may be empty)
	 */
	List<CredentialRequestStatus> findByIndividualIdAndIsDeleted(String individualId, boolean isDeleted);

	/**
	 * Finds active (non-deleted) rows by plain individual id.
	 * <p>
	 * Delegates to {@link #findByIndividualIdAndIsDeleted(String, boolean)} with
	 * {@code isDeleted = false}.
	 * </p>
	 *
	 * @param individualId plain UIN/VID
	 * @return matching status rows where {@code is_deleted = false}
	 */
	default List<CredentialRequestStatus> findByIndividualId(String individualId) {
		return this.findByIndividualIdAndIsDeleted(individualId, false);
	}

	/**
	 * Finds rows by salted individual-id hash, optionally including soft-deleted rows.
	 *
	 * @param individualIdHash salted hash of the individual identifier
	 *                         ({@link IdRepoSecurityManager#ID_HASH})
	 * @param isDeleted        soft-delete flag filter
	 * @return matching status rows (may be empty)
	 */
	List<CredentialRequestStatus> findByIndividualIdHashAndIsDeleted(String individualIdHash, boolean isDeleted);

	/**
	 * Finds active (non-deleted) rows by salted individual-id hash.
	 * <p>
	 * Delegates to {@link #findByIndividualIdHashAndIsDeleted(String, boolean)} with
	 * {@code isDeleted = false}.
	 * </p>
	 *
	 * @param individualIdHash salted hash of the individual identifier
	 * @return matching status rows where {@code is_deleted = false}
	 */
	default List<CredentialRequestStatus> findByIndividualIdHash(String individualIdHash) {
		return this.findByIndividualIdHashAndIsDeleted(individualIdHash, false);
	}

	/**
	 * Finds a single row by composite key parts, optionally including soft-deleted rows.
	 *
	 * @param idHash    salted hash of the individual identifier
	 * @param partnerId credential partner identifier
	 * @param isDeleted soft-delete flag filter
	 * @return optional matching row
	 */
	Optional<CredentialRequestStatus> findByIndividualIdHashAndPartnerIdAndIsDeleted(String idHash, String partnerId,
			boolean isDeleted);

	/**
	 * Finds an active row by salted individual-id hash and partner id.
	 * <p>
	 * Delegates to
	 * {@link #findByIndividualIdHashAndPartnerIdAndIsDeleted(String, String, boolean)}
	 * with {@code isDeleted = false}.
	 * </p>
	 *
	 * @param individualIdHash salted hash of the individual identifier
	 * @param partnerId        credential partner identifier
	 * @return optional matching row where {@code is_deleted = false}
	 */
	default Optional<CredentialRequestStatus> findByIndividualIdHashAndPartnerId(String individualIdHash,
			String partnerId) {
		return this.findByIndividualIdHashAndPartnerIdAndIsDeleted(individualIdHash, partnerId, false);
	}

	/**
	 * Finds all rows with the given lifecycle status string.
	 * <p>
	 * Derived query — <strong>no</strong> row locking. Prefer
	 * {@link #findByStatus(String, int)} when multiple replicas poll concurrently.
	 * </p>
	 *
	 * @param status lifecycle value, e.g. {@link CredentialRequestStatusLifecycle#DELETED}
	 * @return matching status rows (may be empty)
	 */
	List<CredentialRequestStatus> findByStatus(String status);

	/**
	 * Finds rows whose credential expiry timestamp is before the given time.
	 *
	 * @param idExpiryTimestamp cutoff timestamp (exclusive upper bound via
	 *                          {@code Before} semantics)
	 * @return expired credential request rows (may be empty)
	 */
	List<CredentialRequestStatus> findByIdExpiryTimestampBefore(LocalDateTime idExpiryTimestamp);

	/**
	 * JPQL projection query for stale {@code REQUESTED} (or other) rows eligible for
	 * reprocessing.
	 * <p>
	 * Returns only the columns needed to rebuild a credential request via a constructor
	 * expression on {@link CredentialRequestStatus}. Filters by creation time older than
	 * {@code beforeCreateDtimes} and matching {@code status}.
	 * </p>
	 *
	 * @param beforeCreateDtimes cutoff creation timestamp ({@code crDTimes <} this value)
	 * @param status             lifecycle status to match (typically
	 *                           {@link CredentialRequestStatusLifecycle#REQUESTED})
	 * @param pageable           pagination for batch chunking
	 * @return page of projection rows
	 */
	@Query(value = "SELECT new CredentialRequestStatus( individualId, idExpiryTimestamp, idTransactionLimit, tokenId, partnerId ) "
			+ "FROM CredentialRequestStatus crs "
			+ "WHERE crs.crDTimes < :beforeCreateDtimes AND crs.status=:status")
	Page<CredentialRequestStatus> findByRequestedStatusBeforeCrDtimes(
			@Param("beforeCreateDtimes") LocalDateTime beforeCreateDtimes, @Param("status") String status,
			Pageable pageable);

	/**
	 * Claims a batch of rows in the given status for concurrent job processing.
	 * <p>
	 * Uses {@code SELECT … FOR UPDATE SKIP LOCKED} so each replica skips rows already
	 * locked by another transaction. Rows are ordered by {@code cr_dtimes} ascending
	 * (oldest first). Typically called with
	 * {@link CredentialRequestStatusLifecycle#NEW} and a configured page size.
	 * </p>
	 * <p>
	 * Requires an active transaction ({@link Transactional}) so locks are held until
	 * commit/rollback.
	 * </p>
	 *
	 * @param status   lifecycle status to poll (typically {@code NEW})
	 * @param pageSize maximum number of rows to claim in one batch
	 * @return locked status rows ready for processing (may be empty)
	 */
	@Transactional
	@Query(value = "SELECT * FROM credential_request_status crs"
			+ " WHERE crs.status=:status ORDER BY crs.cr_dtimes asc FOR UPDATE SKIP LOCKED LIMIT :pageSize", nativeQuery = true)
	List<CredentialRequestStatus> findByStatus(@Param("status") String status, @Param("pageSize") int pageSize);

}
