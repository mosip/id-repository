package io.mosip.idrepository.core.spi;

import java.util.Map;

import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * @author Manoj SP
 *
 * @param <REQUEST>
 * @param <RESPONSE>
 */
public interface IdRepoDraftService<REQUEST, RESPONSE> {

	public RESPONSE createDraft(String registrationId, String uin) throws IdRepoAppException;

	/**
	 * Creates a draft WITHOUT allocating a UIN. Intended for LOST packets where
	 * the UIN is resolved later (after ABIS deduplication). Uses SHA-256 of
	 * registrationId as the object-store path prefix (pathKey).
	 */
	public RESPONSE createDraftV2(String registrationId) throws IdRepoAppException;

	/**
	 * Stamps UIN and uinHash on an existing LOST draft after ABIS finds the match.
	 * Called by Bio-Dedupe after resolving the matched registration's UIN.
	 */
	public RESPONSE updateDraftUin(String registrationId, String uin) throws IdRepoAppException;
	
	public RESPONSE updateDraft(String registrationId, REQUEST request) throws IdRepoAppException;
	
	public RESPONSE publishDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE discardDraft(String registrationId) throws IdRepoAppException;
	
	public boolean hasDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE getDraft(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;
	
	/**
	 * Granular draft retrieval.
	 *
	 * @param type one of {@code "demographics"}, {@code "biometrics"}, {@code "all"},
	 *             or {@code null}/empty (defaults to {@code "all"}).
	 *             <ul>
	 *               <li>{@code "demographics"} — identity (UIN data) only, no documents.</li>
	 *               <li>{@code "biometrics"} — biometric documents only, no identity.</li>
	 *               <li>{@code "all"} (default) — both identity and all documents.</li>
	 *             </ul>
	 */
	public RESPONSE getDraft(String registrationId, Map<String, String> extractionFormats, String type)
			throws IdRepoAppException;

	public RESPONSE extractBiometrics(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;

    public DraftResponseDto getDraftUin(String uin) throws IdRepoAppException;
}
