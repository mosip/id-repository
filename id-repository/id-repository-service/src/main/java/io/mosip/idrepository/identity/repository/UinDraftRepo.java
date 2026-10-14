package io.mosip.idrepository.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.identity.entity.UinDraft;

/**
 * Spring Data JPA repository for `idrepo` UinDraft persistence operations.
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
	 * Deletes the draft row of a registration. Runs in its own transaction when none
	 * is active, so V2 publish and discard can call it outside a service transaction.
	 *
	 * @param regId the registration id
	 */
	@Transactional
	void deleteByRegId(String regId);

	/**
	 * Exists by uinHash.
	 *
	 * @param uinHash the uin Hash.
	 * @return true, if successful.
	 */
	boolean existsByUinHash(String uinHash);

}
