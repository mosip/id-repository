package io.mosip.idrepository.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.identity.entity.Uin;

/**
 * Spring Data repository for {@link io.mosip.idrepository.identity.entity.Uin} ({@code idrepo.uin}).
 * <p>
 * Lookup by RID, UIN hash, and status; used by {@link io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl}.
 * </p>
 *
 * @author Manoj SP
 */
public interface UinRepo extends JpaRepository<Uin, String> {
	
	/**
	 * Returns the UIN hash for a registration id without loading the full entity.
	 *
	 * @param regId registration id (RID)
	 * @return UIN hash stored for the RID
	 */
	@Query("select uinHash from Uin where regId = :regId")
	String getUinHashByRid(@Param("regId") String regId);

	/**
	 * Returns the tokenized UIN string for a registration id.
	 *
	 * @param regId registration id (RID)
	 * @return encrypted UIN token
	 */
	@Query("select uin from Uin where regId = :regId")
	String getUinByRid(@Param("regId") String regId);

	/**
	 * Returns whether a UIN row exists for the given registration id.
	 *
	 * @param regId registration id (RID)
	 * @return {@code true} when a row exists
	 */
	boolean existsByRegId(String regId);

	/**
	 * Retrieves the UIN entity by reg id.
	 *
	 * @param regId the reg id
	 * @return the UIN entity if found
	 */
	Optional<Uin> findByRegId(String regId);

	/**
	 * Returns lifecycle status code for the given tokenized UIN.
	 *
	 * @param uin encrypted UIN token
	 * @return status code (ACTIVATED, BLOCKED, etc.)
	 */
	@Query("select statusCode from Uin where uin = :uin")
	String getStatusByUin(@Param("uin") String uin);

	/**
	 * Loads the UIN aggregate by UIN hash.
	 *
	 * @param uinHash SHA-256 hash of UIN
	 * @return matching entity if present
	 */
	Optional<Uin> findByUinHash(String uinHash);

	/**
	 * Returns whether a row exists for the given UIN hash.
	 *
	 * @param uinHash SHA-256 hash of UIN
	 * @return {@code true} when a row exists
	 */
	boolean existsByUinHash(String uinHash);

	/**
	 * Resolves registration id from UIN hash.
	 *
	 * @param uinHash SHA-256 hash of UIN
	 * @return RID linked to the hash
	 */
	@Query("select regId from Uin where uinHash = :uinHash")
	String getRidByUinHash(@Param("uinHash") String uinHash);

	/**
	 * Loads the live UIN row with biometrics fetched in the same query.
	 * Documents remain {@code @BatchSize}-loadable; the caller must initialize
	 * them in the same transaction before the session closes.
	 * Dedicated method so {@link #findByUinHash} is unchanged for other APIs.
	 */
	@EntityGraph(attributePaths = {"biometrics"})
	@Query("SELECT u FROM Uin u WHERE u.uinHash = :uinHash")
	Optional<Uin> findWithBiometricsByUinHash(@Param("uinHash") String uinHash);

	/**
	 * Finds a live identity by the registration id that last updated it, with its
	 * biometrics fetched in the same query. Used when a V2 draft is created again for a
	 * registration id that is still the latest one applied to the identity (reprocessing),
	 * so the draft can be built from that identity. Documents are loaded as for
	 * {@link #findWithBiometricsByUinHash}.
	 *
	 * @param regId registration id
	 * @return the identity with biometrics loaded, or empty when none matches
	 */
	@EntityGraph(attributePaths = {"biometrics"})
	@Query("SELECT u FROM Uin u WHERE u.regId = :regId")
	Optional<Uin> findWithBiometricsByRegId(@Param("regId") String regId);
}
