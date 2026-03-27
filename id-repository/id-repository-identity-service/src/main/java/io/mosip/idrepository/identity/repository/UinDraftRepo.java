package io.mosip.idrepository.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.identity.entity.UinDraft;

/**
 * The Interface UinRepo.
 *
 * @author Manoj SP
 */
public interface UinDraftRepo extends JpaRepository<UinDraft, String> {
	
	/**
	 * Gets the uin by refId .
	 *
	 * @param regId the reg id
	 * @return the Uin
	 */
	@Query("select uinHash from Uin where regId = :regId")
	String getUinHashByRid(@Param("regId") String regId);

	/**
	 * Exists by reg id.
	 *
	 * @param regId the reg id
	 * @return true, if successful
	 */
	boolean existsByRegId(String regId);

	/**
	 * Gets the status by uin.
	 *
	 * @param uin the uin
	 * @return the status by uin
	 */
	@Query("select statusCode from Uin where uin = :uin")
	String getStatusByUin(@Param("uin") String uin);
	
	/**
	 * Find by uin.
	 *
	 * @param uinHash the uin hash
	 * @return the uin
	 */
	UinDraft findByUinHash(String uinHash);
	
	/**
	 * Find by RegId.
	 *
	 * @param regId the registration id
	 * @return the uin draft
	 */
	Optional<UinDraft> findByRegId(String regId);

	/**
	 * Find by RegId with biometrics and documents eagerly loaded in a single query.
	 * Use this in place of {@link #findByRegId} whenever the collections will be
	 * accessed, to avoid lazy-load N+1 SELECT statements.
	 *
	 * @param regId the registration id
	 * @return the uin draft with collections initialised
	 */
	@Query("SELECT u FROM UinDraft u LEFT JOIN FETCH u.biometrics LEFT JOIN FETCH u.documents WHERE u.regId = :regId")
	Optional<UinDraft> findByRegIdWithCollections(@Param("regId") String regId);

	/**
	 * Delete by RegId.
	 *
	 * @param regId the registration id
	 */
	void deleteByRegId(String regId);

	/**
	 * Exists by uinHash.
	 *
	 * @param uinHash the uin Hash.
	 * @return true, if successful.
	 */
	boolean existsByUinHash(String uinHash);

}
