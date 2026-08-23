package io.mosip.idrepository.identity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.identity.entity.UinHistory;

/**
 * The Interface UinHistoryRepo.
 *
 * @author Manoj SP
 */
public interface UinHistoryRepo extends JpaRepository<UinHistory, String> {
	
	/**
	 * Exists by reg id.
	 *
	 * @param regId the reg id
	 * @return true, if successful
	 */
	boolean existsByRegId(String regId);
	
	/**
	 * Gets the uin by refId .
	 *
	 * @param regId the reg id
	 * @return the Uin
	 */
	@Query("select uinHash from UinHistory where regId = :regId order by effectiveDateTime desc")
	List<String> getUinHashByRid(@Param("regId") String regId);

	@Query("select regId from UinHistory where uinHash = :uinHash order by effectiveDateTime desc")
	List<String> getLatestRegIdByUinHash(@Param("uinHash") String uinHash);
}
