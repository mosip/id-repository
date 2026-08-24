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
	
	public RESPONSE updateDraft(String registrationId, REQUEST request) throws IdRepoAppException;
	
	public RESPONSE publishDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE discardDraft(String registrationId) throws IdRepoAppException;
	
	public boolean hasDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE getDraft(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;
	
	public RESPONSE extractBiometrics(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;

	public DraftResponseDto getDraftUin(String uin) throws IdRepoAppException;

	// ── V2 methods — enhanced/new behaviour introduced by MOSIP-082 ───────── //

	/**
	 * Creates a draft with optional UIN allocation.
	 * <ul>
	 *   <li>{@code generateUin=true} (default) — allocates a UIN and copies live
	 *       biometric/demographic files to the draft ridHash path (NEW/UPDATE packets).</li>
	 *   <li>{@code generateUin=false} — creates a bare draft without a UIN
	 *       (LOST packets; UIN is resolved later via {@link #updateDraftUinData}).</li>
	 * </ul>
	 *
	 * @param uin         existing UIN for UPDATE packets; {@code null} for NEW/LOST packets
	 * @param generateUin when {@code true} a UIN is allocated; {@code false} for LOST packets
	 */
	public RESPONSE createDraftV2(String registrationId, String uin, boolean generateUin) throws IdRepoAppException;

	/**
	 * Stamps a UIN on an existing LOST draft after ABIS resolves the match.
	 */
	public RESPONSE updateDraftUinData(String registrationId, String uin) throws IdRepoAppException;

	/**
	 * Updates draft identity/biometric data (V2 — same behaviour as V1 currently).
	 */
	public RESPONSE updateDraftV2(String registrationId, REQUEST request) throws IdRepoAppException;

	/**
	 * Publishes a draft to the ID Repository (V2 — enhanced with object-store
	 * draft ridHash → live uinHash copy and full cleanup).
	 */
	public RESPONSE publishDraftV2(String registrationId) throws IdRepoAppException;

	/**
	 * Granular draft retrieval.
	 * Supports {@code type=demographics|biometrics|all} (default: all).
	 */
	public RESPONSE getDraftV2(String registrationId, Map<String, String> extractionFormats, String type)
			throws IdRepoAppException;

	/**
	 * Extracts biometrics for a draft (V2 — same behaviour as V1 currently).
	 */
	public RESPONSE extractBiometricsV2(String registrationId, Map<String, String> extractionFormats) throws IdRepoAppException;

	/**
	 * Discards a draft (V2 — deletes ridHash object-store draft files in
	 * addition to the DB records removed by {@link #discardDraft}).
	 */
	public RESPONSE discardDraftV2(String registrationId) throws IdRepoAppException;
}
