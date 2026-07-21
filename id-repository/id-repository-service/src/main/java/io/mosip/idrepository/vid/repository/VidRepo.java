package io.mosip.idrepository.vid.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.vid.entity.Vid;

/**
 * Spring Data repository for {@link Vid} ({@code idmap.vid}).
 * <p>
 * Supports lookup by VID token, UIN hash, type, and expiry for policy enforcement.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 */
@Repository
public interface VidRepo extends JpaRepository<Vid, String> {

	/**
	 * Loads the full VID entity by its token.
	 *
	 * @param vid virtual id token
	 * @return matching row or {@code null}
	 */
	Vid findByVid(String vid);

	/**
	 * Lists active VIDs of a given type for a UIN hash that have not expired.
	 *
	 * @param uinHash     hashed UIN
	 * @param statusCode  lifecycle status (for example ACTIVE)
	 * @param vidTypeCode VID type code
	 * @param currentTime reference time for expiry comparison
	 * @return matching VID rows
	 */
	List<Vid> findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(String uinHash, String statusCode,
			String vidTypeCode, LocalDateTime currentTime);

	/**
	 * Resolves the encrypted UIN token linked to a VID without loading the full entity.
	 *
	 * @param vid virtual id token
	 * @return encrypted UIN column value
	 */
	@Query("select uin from Vid where vid = :vid")
	String retrieveUinByVid(@Param("vid") String vid);

	/**
	 * Lists non-expired VIDs for a UIN hash and status (any VID type).
	 *
	 * @param uinHash     hashed UIN
	 * @param statusCode  lifecycle status
	 * @param currentTime reference time for expiry comparison
	 * @return matching VID rows
	 */
	List<Vid> findByUinHashAndStatusCodeAndExpiryDTimesAfter(String uinHash, String statusCode,
			LocalDateTime currentTime);

}
