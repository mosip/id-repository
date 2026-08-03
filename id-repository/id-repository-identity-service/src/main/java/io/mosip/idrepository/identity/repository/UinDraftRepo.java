package io.mosip.idrepository.identity.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
	@EntityGraph(attributePaths = {"biometrics"})
	Optional<UinDraft> findByRegId(String regId);

	/**
	 * Delete by RegId.
	 *
	 * @param regId the registration id
	 */
	@Modifying
	@Transactional
	void deleteByRegId(String regId);

	/**
	 * Exists by uinHash.
	 *
	 * @param uinHash the uin Hash.
	 * @return true, if successful.
	 */
	boolean existsByUinHash(String uinHash);

	/**
	 * Stamps the UIN on a LOST draft. Uses a bulk-update to bypass isNew()=true
	 * which would otherwise cause save(entity) to call em.persist() (a no-op for
	 * managed entities) instead of generating the UPDATE SQL.
	 */
	@Modifying
	@Transactional
	@Query("UPDATE UinDraft SET uin = :uin, uinHash = :uinHash, updatedBy = :updatedBy, updatedDateTime = :updatedDateTime WHERE regId = :regId")
	void updateUinByRegId(@Param("regId") String regId,
	                      @Param("uin") String uin,
	                      @Param("uinHash") String uinHash,
	                      @Param("updatedBy") String updatedBy,
	                      @Param("updatedDateTime") LocalDateTime updatedDateTime);

}
