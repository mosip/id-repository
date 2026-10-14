package io.mosip.idrepository.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

/**
 * Spring Data repository for {@link Handle} rows in {@code idrepo.handle}.
 *
 * <p>
 * Maps handle values (salted hashes) to UIN hashes so residents can be retrieved by
 * {@link IdType#HANDLE} and so credential issuance can resolve all handles for an
 * individual. Supports uniqueness checks at registration time, reverse lookups by
 * UIN hash, and handle lifecycle status updates.
 * </p>
 *
 * <h2>Persistence unit</h2>
 * <p>
 * Enabled only when bean {@code idRepoDataSource} exists
 * ({@link ConditionalOnBean}). Bound to PU1 ({@code mosip_idrepo}).
 * </p>
 *
 * <h2>Hashing</h2>
 * <p>
 * {@code handleHash} and {@code uinHash} arguments are salted HMAC digests produced via
 * {@link IdRepoSecurityManager} — never pass raw handle or UIN strings.
 * </p>
 *
 * @see Handle
 * @see IdType#HANDLE
 * @see IdRepoSecurityManager
 */
@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface HandleRepo extends JpaRepository<Handle, String> {

	/**
	 * Checks whether a handle with the given salted hash already exists.
	 *
	 * @param handleHash salted hash of the handle value
	 * @return {@code true} if a row exists with this hash; {@code false} otherwise
	 */
	boolean existsByHandleHash(String handleHash);

	/**
	 * Returns all handles linked to the given UIN hash.
	 *
	 * @param uinHash salted hash of the UIN
	 * @return list of handle rows for the individual (never {@code null}; may be empty)
	 */
	List<Handle> findByUinHash(String uinHash);

	/**
	 * Finds a single handle row by its salted hash.
	 *
	 * @param handleHash salted hash of the handle value
	 * @return matching handle entity, or empty when not found
	 */
	Optional<Handle> findByHandleHash(String handleHash);

	/**
	 * Returns UIN hashes for any of the given handle hashes (uniqueness / same-user check).
	 *
	 * @param hashes salted handle hashes from the request
	 * @return UIN hashes that already own one or more of those handles
	 */
	@Query("SELECT uinHash FROM Handle WHERE handleHash IN (:hashes)")
	List<String> findUinHashByHandleHashes(@Param("hashes") List<String> hashes);

	/**
	 * Sets the lifecycle status of a single handle row.
	 *
	 * @param handleHash salted hash of the handle
	 * @param status     new {@code HandleStatusLifecycle} name
	 * @return number of rows updated
	 */
	@Modifying
	@Transactional
	@Query("UPDATE Handle SET status=:status WHERE handleHash=:handleHash")
	int updateStatusByHandleHash(@Param("handleHash") String handleHash, @Param("status") String status);

	/**
	 * Sets the lifecycle status of every handle linked to a UIN.
	 *
	 * @param uinHash salted UIN hash
	 * @param status  new {@code HandleStatusLifecycle} name
	 * @return number of rows updated
	 */
	@Modifying
	@Transactional
	@Query("UPDATE Handle SET status=:status WHERE uinHash=:uinHash")
	int updateStatusByUinHash(@Param("uinHash") String uinHash, @Param("status") String status);

	/**
	 * Transitions handles for a UIN from one lifecycle status to another.
	 *
	 * @param uinHash        salted UIN hash
	 * @param currentStatus  expected current status
	 * @param newStatus      status to set
	 * @return number of rows updated
	 */
	@Modifying
	@Transactional
	@Query("UPDATE Handle SET status=:newStatus WHERE uinHash=:uinHash AND status=:currentStatus")
	int updateStatusByUinHashAndStatus(@Param("uinHash") String uinHash, @Param("currentStatus") String currentStatus,
			@Param("newStatus") String newStatus);
}
