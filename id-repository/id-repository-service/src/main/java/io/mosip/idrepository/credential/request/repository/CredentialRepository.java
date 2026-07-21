package io.mosip.idrepository.credential.request.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * Spring Data JPA repository for {@link CredentialEntity} on the credential datasource
 * ({@code mosip_credential.credential_transaction}).
 * <p>
 * Provides paginated lookups by status for the credential-request batch jobs.
 * Native queries use {@code FOR UPDATE SKIP LOCKED} so multiple job replicas can
 * dequeue rows safely without blocking one another.
 * </p>
 *
 * @author Sowmya
 * @see io.mosip.idrepository.credential.request.entity.CredentialEntity
 */
@Repository
public interface CredentialRepository extends BaseRepository<CredentialEntity, String> {

	/**
	 * Returns a page of queue rows matching a single status code (JPQL, no row lock).
	 *
	 * @param statusCode status to filter on ({@code status_code} column)
	 * @param pageable   pagination and sort parameters
	 * @return page of matching {@link CredentialEntity} rows
	 */
	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode= :statusCode")
	Page<CredentialEntity> findByStatusCode(@Param("statusCode") String statusCode, Pageable pageable);

	/**
	 * Returns a page of queue rows matching a status code updated on or after the given time.
	 *
	 * @param statusCode      status to filter on ({@code status_code} column)
	 * @param effectiveDTimes minimum {@code upd_dtimes} (inclusive)
	 * @param pageable        pagination and sort parameters
	 * @return page of matching rows eligible for reprocessing
	 */
	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode= :statusCode and crdn.updateDateTime>= :effectiveDTimes")
	Page<CredentialEntity> findByStatusCodeWithEffectiveDtimes(@Param("statusCode") String statusCode,
			@Param("effectiveDTimes") LocalDateTime effectiveDTimes,
			Pageable pageable);

	/**
	 * Returns a page of rows for the given status with pessimistic write lock and short lock timeout.
	 * <p>
	 * Used when exclusive access to a page of rows is required within a transaction.
	 * </p>
	 *
	 * @param statusCode status to filter on
	 * @param pageable   pagination parameters
	 * @return locked page of queue rows
	 */
	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "1") })
	@Query("select c from CredentialEntity c where c.statusCode=:statusCode")
	Page<CredentialEntity> findCredentialByStatusCode(@Param("statusCode") String statusCode, Pageable pageable);

	/**
	 * Dequeues up to {@code pageSize} rows for the given status using
	 * {@code FOR UPDATE SKIP LOCKED}, ordered by {@code cr_dtimes}.
	 * <p>
	 * Preferred path for the primary credential batch job on PostgreSQL.
	 * </p>
	 *
	 * @param statusCode status to dequeue
	 * @param pageSize   maximum number of rows to return
	 * @return list of locked queue rows ready for processing
	 */
	@Transactional
	@Query(value = "SELECT * FROM credential_transaction ct"
			+ " WHERE ct.status_code=:statusCode ORDER BY cr_dtimes FOR UPDATE SKIP LOCKED LIMIT :pageSize", nativeQuery = true)
	List<CredentialEntity> findCredentialByStatusCode(@Param("statusCode") String statusCode, @Param("pageSize") int pageSize);

	/**
	 * Returns a page of rows whose status is in the given set, with pessimistic write lock.
	 *
	 * @param statusCodes one or more status codes to match
	 * @param pageable    pagination parameters
	 * @return locked page of queue rows for reprocess or multi-status batch
	 */
	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "1") })
	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode in :statusCodes ")
	Page<CredentialEntity> findCredentialByStatusCodes(@Param("statusCodes") String[] statusCodes, Pageable pageable);

	/**
	 * Dequeues up to {@code pageSize} rows for any of the given statuses using
	 * {@code FOR UPDATE SKIP LOCKED}, ordered by {@code upd_dtimes}.
	 * <p>
	 * Used by the credential reprocess batch job.
	 * </p>
	 *
	 * @param statusCodes status codes eligible for reprocess
	 * @param pageSize    maximum number of rows to return
	 * @return list of locked queue rows
	 */
	@Transactional
	@Query(value = "SELECT * FROM credential_transaction ct"
			+ " WHERE ct.status_code in :statusCodes ORDER BY upd_dtimes FOR UPDATE SKIP LOCKED LIMIT :pageSize", nativeQuery = true)
	List<CredentialEntity> findCredentialByStatusCodes(@Param("statusCodes") String[] statusCodes, @Param("pageSize") int pageSize);
}
