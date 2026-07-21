package io.mosip.idrepository.core.spi;

import java.util.Map;

import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * SPI for UIN draft lifecycle — staging identity data before publish to the
 * live {@code uin} table.
 * <p>
 * Drafts allow pre-registration data to be stored, updated, and either
 * published (promoted to active identity) or discarded without affecting
 * production UIN records.
 * </p>
 * <p>
 * <b>Implementor:</b> {@code IdRepoDraftServiceImpl} in {@code id-repository-service}.
 * </p>
 * <p>
 * <b>Caller:</b> {@code IdRepoDraftController}.
 * </p>
 *
 * @author Manoj SP
 * @param <REQUEST>  inbound request DTO type (typically {@code IdRequestDTO})
 * @param <RESPONSE> outbound response DTO type (typically {@code IdResponseDTO})
 * @see io.mosip.idrepository.core.spi.IdRepoService
 */
public interface IdRepoDraftService<REQUEST, RESPONSE> {

	/**
	 * Creates a new draft row for the given registration id and UIN.
	 *
	 * @param registrationId pre-registration / RID
	 * @param uin            assigned UIN
	 * @return draft response
	 * @throws IdRepoAppException on validation or persistence failure
	 */
	public RESPONSE createDraft(String registrationId, String uin) throws IdRepoAppException;

	/**
	 * Updates an existing draft with new demographic or biometric data.
	 *
	 * @param registrationId draft registration identifier
	 * @param request        update payload
	 * @return updated draft response
	 * @throws IdRepoAppException if the draft does not exist or validation fails
	 */
	public RESPONSE updateDraft(String registrationId, REQUEST request) throws IdRepoAppException;

	/**
	 * Publishes a draft — promotes staged data to the active {@code uin} table.
	 *
	 * @param registrationId draft registration identifier
	 * @return published identity response
	 * @throws IdRepoAppException if the draft is invalid or publish fails
	 */
	public RESPONSE publishDraft(String registrationId) throws IdRepoAppException;

	/**
	 * Discards a draft without publishing.
	 *
	 * @param registrationId draft registration identifier
	 * @return discard confirmation response
	 * @throws IdRepoAppException if the draft does not exist
	 */
	public RESPONSE discardDraft(String registrationId) throws IdRepoAppException;

	/**
	 * Checks whether a draft exists for the given registration id.
	 *
	 * @param registrationId draft registration identifier
	 * @return {@code true} if a draft row exists
	 * @throws IdRepoAppException on lookup failure
	 */
	public boolean hasDraft(String registrationId) throws IdRepoAppException;

	/**
	 * Retrieves draft identity data, optionally applying biometric extraction formats.
	 *
	 * @param registrationId      draft registration identifier
	 * @param extractionFormats     per-modality biometric extraction format map
	 * @return draft identity response
	 * @throws IdRepoAppException if the draft does not exist
	 */
	public RESPONSE getDraft(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;

	/**
	 * Extracts biometric templates from draft biometrics without full identity retrieval.
	 *
	 * @param registrationId      draft registration identifier
	 * @param extractionFormats     per-modality biometric extraction format map
	 * @return response containing extracted biometric data
	 * @throws IdRepoAppException if extraction fails
	 */
	public RESPONSE extractBiometrics(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;

	/**
	 * Looks up draft metadata by UIN (reverse lookup from assigned UIN to draft).
	 *
	 * @param uin assigned UIN
	 * @return draft summary DTO
	 * @throws IdRepoAppException if no draft exists for the UIN
	 */
    public DraftResponseDto getDraftUin(String uin) throws IdRepoAppException;
}