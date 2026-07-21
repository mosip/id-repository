package io.mosip.idrepository.core.spi;

import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * SPI for Virtual ID (VID) lifecycle operations.
 * <p>
 * Covers generation, retrieval, update, regeneration, and bulk deactivation/
 * reactivation of VIDs bound to a UIN. VID policy constraints (count limits,
 * expiry) are enforced in the implementation layer.
 * </p>
 * <p>
 * <b>Implementor:</b> {@code VidServiceImpl} in {@code id-repository-service}.
 * </p>
 * <p>
 * <b>Callers:</b> VID REST controllers in {@code id-repository-service}.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar.
 * @param <REQUEST>      inbound request DTO type (typically {@code VidRequestDTO})
 * @param <RESPONSE>     outbound response wrapper type
 * @param <NOTIFICATION> list response type for VID enumeration by UIN
 * @see io.mosip.idrepository.core.constant.IdType#VID
 */
public interface VidService<REQUEST, RESPONSE, NOTIFICATION> {

	/**
	 * Generates a new VID for the UIN in the request, subject to VID policy limits.
	 *
	 * @param vidRequest generation request with UIN and VID type
	 * @return response containing the new VID
	 * @throws IdRepoAppException if policy limits are exceeded or generation fails
	 */
	RESPONSE generateVid(REQUEST vidRequest) throws IdRepoAppException;

	/**
	 * Resolves the UIN associated with a given VID.
	 *
	 * @param vid virtual identifier value
	 * @return response containing the linked UIN
	 * @throws IdRepoAppException if the VID is not found or is inactive
	 */
	RESPONSE retrieveUinByVid(String vid) throws IdRepoAppException;

	/**
	 * Updates VID status (e.g. activate/deactivate) per VID policy.
	 *
	 * @param vid     target VID value
	 * @param request update payload with desired status
	 * @return updated VID response
	 * @throws IdRepoAppException if the update violates policy or the VID is not found
	 */
	RESPONSE updateVid(String vid, REQUEST request) throws IdRepoAppException;

	/**
	 * Regenerates a VID — invalidates the old value and issues a replacement.
	 *
	 * @param vid existing VID to regenerate
	 * @return response containing the new VID
	 * @throws IdRepoAppException if regeneration is not permitted by policy
	 */
	RESPONSE regenerateVid(String vid) throws IdRepoAppException;

	/**
	 * Deactivates all active VIDs for the given UIN (e.g. on identity deactivation).
	 *
	 * @param uin UIN whose VIDs should be deactivated
	 * @return summary response of deactivated VIDs
	 * @throws IdRepoAppException on persistence failure
	 */
	RESPONSE deactivateVIDsForUIN(String uin) throws IdRepoAppException;

	/**
	 * Reactivates previously deactivated VIDs for the given UIN.
	 *
	 * @param uin UIN whose VIDs should be reactivated
	 * @return summary response of reactivated VIDs
	 * @throws IdRepoAppException on persistence failure
	 */
	RESPONSE reactivateVIDsForUIN(String uin) throws IdRepoAppException;

	/**
	 * Lists all VIDs currently associated with a UIN.
	 *
	 * @param uin UIN to enumerate
	 * @return notification/list response with VID details
	 * @throws IdRepoAppException if the UIN is not found
	 */
	NOTIFICATION retrieveVidsByUin(String uin) throws IdRepoAppException;;
}