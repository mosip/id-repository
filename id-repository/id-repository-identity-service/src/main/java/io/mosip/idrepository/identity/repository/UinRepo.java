package io.mosip.idrepository.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.identity.entity.Uin;

/**
 * The Interface UinRepo.
 *
 * @author Manoj SP
 */
public interface UinRepo extends JpaRepository<Uin, String> {
	
	/**
	 * Gets the uin by refId .
	 *
	 * @param regId the reg id
	 * @return the Uin
	 */
	@Query("select uinHash from Uin where regId = :regId")
	String getUinHashByRid(@Param("regId") String regId);
	
	@Query("select uin from Uin where regId = :regId")
	String getUinByRid(@Param("regId") String regId);

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
	Optional<Uin> findByUinHash(String uinHash);
	
	
	/**
	 * Exists by uinHash.
	 *
	 * @param uinHash the uin Hash.
	 * @return true, if successful.
	 */
	boolean existsByUinHash(String uinHash);

	@Query("select regId from Uin where uinHash = :uinHash")
	String getRidByUinHash(@Param("uinHash") String uinHash);
}
