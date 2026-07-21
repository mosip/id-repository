package io.mosip.idrepository.core.repository;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link Handle} rows in {@code idrepo.handle}.
 *
 * <p>
 * Maps handle values (salted hashes) to UIN hashes so residents can be retrieved by
 * {@link IdType#HANDLE} and so credential issuance can resolve all handles for an
 * individual. Supports uniqueness checks at registration time and reverse lookups by
 * UIN hash.
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
 * <h2>Usage</h2>
 * <pre>
 * if (handleRepo.existsByHandleHash(handleHash)) {
 *     // reject duplicate handle registration
 * }
 * List&lt;Handle&gt; handles = handleRepo.findByUinHash(uinHash);
 * Handle row = handleRepo.findByHandleHash(handleHash);
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity create/update services — uniqueness and persistence</li>
 *   <li>Identity retrieve-by-handle paths</li>
 *   <li>Credential managers — enumerate handles for an individual</li>
 * </ul>
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
	 * <p>
	 * Used at registration / update time to enforce handle uniqueness before insert.
	 * </p>
	 *
	 * @param handleHash salted hash of the handle value
	 * @return {@code true} if a row exists with this hash; {@code false} otherwise
	 */
    boolean existsByHandleHash(String handleHash);

	/**
	 * Returns all handles linked to the given UIN hash.
	 * <p>
	 * Used during credential issuance and identity retrieve flows that need every handle
	 * associated with an individual.
	 * </p>
	 *
	 * @param uinHash salted hash of the UIN
	 * @return list of handle rows for the individual (never {@code null}; may be empty)
	 */
    List<Handle> findByUinHash(String uinHash);

	/**
	 * Finds a single handle row by its salted hash.
	 * <p>
	 * Used for {@link IdType#HANDLE} retrieval to resolve the linked UIN hash and related
	 * crypto fields.
	 * </p>
	 *
	 * @param handleHash salted hash of the handle value
	 * @return matching handle entity, or {@code null} if not found
	 */
    Handle findByHandleHash(String handleHash);
}
