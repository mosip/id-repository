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

	// ── V2 methods — enhanced/new behaviour introduced by MOSIP-082 ───────── //
	//
	// V2 drafts keep their object-store files under _draft/{ridHash}/Biometrics|Demographics/
	// (ridHash = HMAC of the registration id) and move them to the live {uinHash}/ path only
	// when the draft is published. V2 also supports LOST packets, whose UIN is only known
	// after ABIS deduplication.

	/**
	 * Creates a V2 draft for a registration.
	 * <ul>
	 *   <li>{@code uin} given — UPDATE, ACTIVATED or DEACTIVATED packet: the draft is built
	 *       from the live identity and its live files are copied to the draft path.</li>
	 *   <li>{@code uin} {@code null}, registration id still the latest applied to a UIN —
	 *       reprocessing: the draft is built from that identity.</li>
	 *   <li>{@code uin} {@code null} and {@code generateUin=true} — NEW packet: a UIN is
	 *       allocated and a minimal identity object is stored.</li>
	 *   <li>{@code uin} {@code null} and {@code generateUin=false} — LOST packet: a draft with
	 *       no UIN is created; the UIN is stamped later through {@link #updateDraftUinData}.</li>
	 * </ul>
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN for UPDATE packets; {@code null} for NEW and LOST packets
	 * @param generateUin    {@code true} to allocate a UIN when {@code uin} is {@code null};
	 *                       {@code false} for LOST packets
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code RECORD_EXISTS} when a draft already exists;
	 *         {@code RID_OLDER_THAN_LATEST_PROCESSED} when a newer registration has already
	 *         been applied; {@code NO_RECORD_FOUND} when {@code uin} does not exist;
	 *         {@code DRAFT_OBJECT_COPY_FAILED} when live files cannot be copied;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public RESPONSE createDraftV2(String registrationId, String uin, boolean generateUin) throws IdRepoAppException;

	/**
	 * Stamps a UIN on a LOST draft after ABIS matches the packet to an existing identity.
	 * Missing demographic fields are back-filled from the live identity; values already in
	 * the draft win. Biometrics and documents are not changed.
	 *
	 * @param registrationId registration id of the LOST draft
	 * @param uin            UIN of the matched live identity
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code UIN_NOT_FOUND} when the UIN has no live identity;
	 *         {@code DRAFT_UIN_HASH_MISMATCH} when the draft already carries a different UIN;
	 *         {@code RECORD_EXISTS} when another draft already holds this UIN;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public RESPONSE updateDraftUinData(String registrationId, String uin) throws IdRepoAppException;

	/**
	 * Merges packet identity data, biometrics and documents into an existing V2 draft.
	 * Files are written to the draft path, not the live path.
	 *
	 * @param registrationId registration id of the draft
	 * @param request        identity update payload
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_UIN_DETAILS_NOT_FOUND} when the data cannot be merged because the
	 *         draft has no UIN yet; {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public RESPONSE updateDraftV2(String registrationId, REQUEST request) throws IdRepoAppException;

	/**
	 * Publishes a V2 draft as the live identity. Draft files are moved to the live path
	 * first, then the identity is added or updated in its own transaction, then the draft
	 * rows are deleted. A failed publish can be retried: files already moved are skipped.
	 *
	 * @param registrationId registration id of the draft
	 * @return response with the live identity status and, for a new identity, the draft VID
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_UIN_DETAILS_NOT_FOUND} when the draft has no UIN or identity data
	 *         (a LOST draft that was never stamped); {@code DRAFT_OBJECT_MOVE_FAILED} when a
	 *         file cannot be moved; {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public RESPONSE publishDraftV2(String registrationId) throws IdRepoAppException;

	/**
	 * Reads a V2 draft, optionally limited to one kind of content.
	 *
	 * @param registrationId    registration id of the draft
	 * @param extractionFormats modality key to SDK format; when not empty, biometrics are
	 *                          returned with templates in these formats
	 * @param type              {@code demographics}, {@code biometrics},
	 *                          {@code supportingdocuments} or {@code all}; {@code null} or
	 *                          blank means {@code all}; case-insensitive
	 * @return draft identity data and/or Base64URL-encoded files
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code INVALID_INPUT_PARAMETER} when {@code type} is not recognised;
	 *         {@code FILE_NOT_FOUND} or {@code FILE_STORAGE_ACCESS_ERROR} when a file cannot be read
	 */
	public RESPONSE getDraftV2(String registrationId, Map<String, String> extractionFormats, String type)
			throws IdRepoAppException;

	/**
	 * Re-extracts biometric templates for a V2 draft. Previously extracted files for the
	 * requested formats are deleted first, so the extraction runs again and the results
	 * are stored on the draft path. Does nothing when {@code extractionFormats} is empty.
	 *
	 * @param registrationId    registration id of the draft
	 * @param extractionFormats modality key to SDK format
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code BIO_EXTRACTION_ERROR} when extraction fails
	 */
	public RESPONSE extractBiometricsV2(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;

	/**
	 * Discards a V2 draft. Draft files are deleted first, so a failure keeps the draft rows
	 * and the call can be retried; then the draft, biometric draft and document draft rows
	 * are deleted.
	 *
	 * @param registrationId registration id of the draft
	 * @return response with status {@code DISCARDED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_OBJECT_DELETE_FAILED} when draft files cannot be deleted;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public RESPONSE discardDraftV2(String registrationId) throws IdRepoAppException;
}
