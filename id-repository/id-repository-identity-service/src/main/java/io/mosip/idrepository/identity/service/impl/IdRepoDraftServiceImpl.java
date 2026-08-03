package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CREATE_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DISCARD_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DOT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DRAFTED;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DRAFT_RECORD_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXCLUDED_ATTRIBUTE_LIST;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX;
import static io.mosip.idrepository.core.constant.IdRepoConstants.GENERATE_UIN;
import static io.mosip.idrepository.core.constant.IdRepoConstants.GET_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ID_REPO_DRAFT_SERVICE_IMPL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_KERNEL_IDREPO_JSON_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.PUBLISH_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ROOT_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UPDATE_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VERIFIED_ATTRIBUTES;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RECORD_EXISTS;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_GENERATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.dto.DraftUinResponseDto;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.IdRepoDraftService;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UinBiometric;
import io.mosip.idrepository.identity.entity.UinBiometricDraft;
import io.mosip.idrepository.identity.entity.UinDocument;
import io.mosip.idrepository.identity.entity.UinDocumentDraft;
import io.mosip.idrepository.identity.entity.UinDraft;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.idrepository.identity.helper.VidDraftHelper;
import io.mosip.idrepository.identity.repository.UinBiometricDraftRepo;
import io.mosip.idrepository.identity.repository.UinBiometricRepo;
import io.mosip.idrepository.identity.repository.UinDocumentDraftRepo;
import io.mosip.idrepository.identity.repository.UinDocumentRepo;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.DateUtils2;

import org.hibernate.exception.JDBCConnectionException;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * @author Manoj SP (original)
 */
@Service
@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
public class IdRepoDraftServiceImpl extends IdRepoServiceImpl
		implements IdRepoDraftService<IdRequestDTO, IdResponseDTO> {

	private static final Logger idrepoDraftLogger = IdRepoLogger.getLogger(IdRepoDraftServiceImpl.class);

	private static final String COMMA = ",";
	private static final String DEFAULT_ATTRIBUTE_LIST = "UIN,verifiedAttributes,IDSchemaVersion";

	@Value("${" + MOSIP_KERNEL_IDREPO_JSON_PATH + "}")
	private String uinPath;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Value("${mosip.idrepo.create-identity.enable-force-merge:false}")
	private boolean isForceMergeEnabled;

	@Autowired
	private UinDraftRepo uinDraftRepo;

	@Autowired
	private UinBiometricDraftRepo uinBiometricDraftRepo;

	@Autowired
	private UinDocumentDraftRepo uinDocumentDraftRepo;

	@Autowired
	private IdRequestValidator validator;

	@Autowired
	private UinBiometricRepo uinBiometricRepo;

	@Autowired
	private UinDocumentRepo uinDocumentRepo;

	@Autowired
	private IdRepoProxyServiceImpl proxyService;

	@Autowired
	private VidDraftHelper vidDraftHelper;

	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	@Autowired
	private Environment environment;

	/**
	 * Creates a draft WITHOUT allocating a UIN (for LOST packets).
	 * Uses SHA-256 of registrationId as the object-store path prefix (pathKey).
	 */
	@Override
	public IdResponseDTO createDraftV2(String registrationId) throws IdRepoAppException {
		try {
			if (!isForceMergeEnabled
					&& (super.uinHistoryRepo.existsByRegId(registrationId)
					|| uinDraftRepo.existsByRegId(registrationId))) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "RID ALREADY EXIST | regId=" + registrationId);
				throw new IdRepoAppException(RECORD_EXISTS);
			}
			UinDraft newDraft = new UinDraft();
			newDraft.setRegId(registrationId);
			newDraft.setStatusCode("DRAFT");
			newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
			newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
			uinDraftRepo.saveAndFlush(newDraft);
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public IdResponseDTO updateDraftUin(String registrationId, String uin) throws IdRepoAppException {
		try {
			if (!uinDraftRepo.existsByRegId(registrationId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "RID NOT FOUND IN DB | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			// We must replicate the interceptor's encryptWithSalt step here so that the
			// value stored in the DB is "{saltId}_{encryptedBase64}", which is what
			// decryptUin() expects.
			String preEncryptFormat = super.getUinToEncrypt(uin);
			List<String> uinParts = Arrays.asList(preEncryptFormat.split(SPLITTER));
			byte[] encryptedUinBytes = securityManager.encryptWithSalt(
					uinParts.get(1).getBytes(),
					CryptoUtil.decodePlainBase64(uinParts.get(2)),
					uinRefId);
			String encryptedUin = uinParts.get(0) + SPLITTER + new String(encryptedUinBytes);
			uinDraftRepo.updateUinByRegId(
					registrationId,
					encryptedUin,
					super.getUinHash(uin),
					IdRepoSecurityManager.getUser(),
					DateUtils2.getUTCCurrentDateTime());
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public IdResponseDTO createDraft(String registrationId, String uin) throws IdRepoAppException {
		try {
			if (!isForceMergeEnabled
					&& (super.uinHistoryRepo.existsByRegId(registrationId)
					|| uinDraftRepo.existsByRegId(registrationId))) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "RID ALREADY EXIST | regId=" + registrationId);
				throw new IdRepoAppException(RECORD_EXISTS);
			}

			UinDraft newDraft;

			if (isForceMergeEnabled) {
				// Resolve UIN from existing identity record.
				IdResponseDTO response = proxyService.retrieveIdentityByRid(registrationId, uin, null);
				LinkedHashMap<String, Object> map =
						mapper.convertValue(response.getResponse().getIdentity(), new TypeReference<LinkedHashMap<String, Object>>() {});
				uin = String.valueOf(map.get("UIN"));
			}

			if (Objects.nonNull(uin)) {
				Optional<Uin> uinObjectOptional = super.uinRepo.findByUinHash(super.getUinHash(uin));
				if (uinObjectOptional.isEmpty()) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							CREATE_DRAFT, "UIN NOT EXIST | uin=<redacted>");
					throw new IdRepoAppException(NO_RECORD_FOUND);
				}
				// CreateDraftStage already discards a draft for the same reg_id before calling
				// here. But if a stale draft from a *different* reg_id holds the same uin_hash
				// (e.g. a previously failed packet that was never cleaned up), the uin_hash
				// UNIQUE constraint would fire on saveAndFlush. Discard it here as a safety net.
				UinDraft staleDraft = uinDraftRepo.findByUinHash(super.getUinHash(uin));
				if (staleDraft != null && !registrationId.equals(staleDraft.getRegId())) {
					idrepoDraftLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							CREATE_DRAFT, "Discarding stale draft | old regId=" + staleDraft.getRegId()
									+ " | new regId=" + registrationId);
					discardDraft(staleDraft.getRegId());
					// deleteDraftDbRecords uses @Modifying bulk-deletes that send SQL immediately
					// (bypassing Hibernate's action queue), so the stale uin_draft row is already
					// gone within this transaction before the INSERT below.
					uinDraftRepo.flush();
				}
				Uin uinObject = uinObjectOptional.get();
				newDraft = mapper.convertValue(uinObject, UinDraft.class);
				updateBiometricAndDocumentDrafts(registrationId, newDraft, uinObject);
				newDraft.setUin(super.getUinToEncrypt(uin));

				// Copy existing live files to the draft path so that updateDraft can read and
				// merge them. Live files live at {livePrefix}/{type}/{fileId}; draft path is
				// _draft/{ridHash}/{type}/{fileId}.
				String ridHash = objectStoreHelper.getRidHash(registrationId);
				String livePrefix = uinObject.getUinHash().split(SPLITTER)[1];
				if (newDraft.getBiometrics() != null) {
					for (UinBiometricDraft bio : newDraft.getBiometrics()) {
						objectStoreHelper.copyBiometricLiveToDraft(livePrefix, ridHash, bio.getBioFileId());
					}
				}
				if (newDraft.getDocuments() != null) {
					for (UinDocumentDraft doc : newDraft.getDocuments()) {
						objectStoreHelper.copyDemographicLiveToDraft(livePrefix, ridHash, doc.getDocId());
					}
				}
			} else {
				// Brand-new identity — generate a UIN.
				newDraft = new UinDraft();
				// Delegated to IdRepoServiceHelper so the HTTP call runs with
				// Propagation.NOT_SUPPORTED, suspending the transaction and
				// releasing the DB connection for the duration of the REST call.
				uin = idRepoServiceHelper.generateUin();
				newDraft.setUin(super.getUinToEncrypt(uin));
				newDraft.setUinHash(super.getUinHash(uin));
				byte[] uinData = convertToBytes(generateIdentityObject(uin));
				newDraft.setUinData(uinData);
				newDraft.setUinDataHash(securityManager.hash(uinData));
			}

			// Set common fields once, after both branches.
			newDraft.setRegId(registrationId);
			newDraft.setStatusCode("DRAFT");
			newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
			newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
			// saveAndFlush forces the SQL to execute immediately so any constraint
			// violations are caught inside this try-catch (not at transaction commit time).
			uinDraftRepo.saveAndFlush(newDraft);

			return constructIdResponse(null, DRAFTED, null, null);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	// NOT_SUPPORTED: releases DB connection before S3 uploads.
	// uinData, biometrics, and documents are all eager-loaded via @EntityGraph
	// on findByRegId(), so they remain accessible after the entity detaches.
	//@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO updateDraft(String registrationId, IdRequestDTO request) throws IdRepoAppException {
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(registrationId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "RID NOT FOUND IN DB | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			UinDraft draftToUpdate = uinDraft.get();

			if (Objects.isNull(draftToUpdate.getUinData())) {
				// First update — no existing identity data in the draft yet.
				byte[] uinData = super.convertToBytes(request.getRequest().getIdentity());
				draftToUpdate.setUinData(uinData);
				draftToUpdate.setUinDataHash(securityManager.hash(uinData));
				updateDocuments(request.getRequest(), draftToUpdate);
				draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
				draftToUpdate.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
			} else {
				// Subsequent update — merge changes into existing draft data.
				updateDemographicData(request, draftToUpdate);
				updateDocuments(request.getRequest(), draftToUpdate);
			}

			uinDraftRepo.save(draftToUpdate);

		} catch (JSONException | IOException | InvalidJsonException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}

		return constructIdResponse(null, DRAFTED, null, null);
	}

	@Override
	public IdResponseDTO publishDraft(String regId) throws IdRepoAppException {
		anonymousProfileHelper.setRegId(regId);
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, DRAFT_RECORD_NOT_FOUND + " | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			UinDraft draft = uinDraft.get();

			// LOST drafts have no UIN until updateDraftUin is called after ABIS match.
			if (draft.getUinHash() == null || draft.getUin() == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, "Cannot publish draft without UIN — updateDraftUin must be called first | regId=" + regId
								+ " | uinNull=" + (draft.getUin() == null) + " | uinHashNull=" + (draft.getUinHash() == null));
				throw new IdRepoAppException(UNKNOWN_ERROR,
						"Draft UIN has not been assigned; updateDraftUin was not called for regId=" + regId);
			}

			// Resolve the last biometric file ID safely — biometrics may be empty.
			String lastBioFileId = (Objects.nonNull(draft.getBiometrics()) && !draft.getBiometrics().isEmpty())
					? draft.getBiometrics().get(draft.getBiometrics().size() - 1).getBioFileId()
					: null;

			anonymousProfileHelper.setNewCbeff(
					draft.getUinHash().split("_")[1],
					!anonymousProfileHelper.isNewCbeffPresent() ? lastBioFileId : null);

			IdRequestDTO idRequest = buildRequest(regId, draft);
			validateRequest(idRequest.getRequest());

			String uin = decryptUin(draft.getUin(), draft.getUinHash());
			String draftVid = null;
			final Uin uinObject;

			if (uinRepo.existsByUinHash(draft.getUinHash())) {
				uinObject = super.updateIdentity(idRequest, uin);
			} else {
				draftVid = vidDraftHelper.generateDraftVid(uin);
				uinObject = super.addIdentity(idRequest, uin);
				vidDraftHelper.activateDraftVid(draftVid);
			}

			publishDocuments(draft, uinObject);

			// srcPrefix is always _draft/{ridHash}/ — ridHash derived on the fly from regId.
			// destPrefix is always the live uinHash slot.
			String srcPrefix = objectStoreHelper.getRidHash(draft.getRegId());
			String destPrefix = draft.getUinHash().split(SPLITTER)[1];
			if (draft.getBiometrics() != null) {
				for (UinBiometricDraft bio : draft.getBiometrics()) {
					if (objectStoreHelper.draftBiometricObjectExists(srcPrefix, bio.getBioFileId())) {
						objectStoreHelper.copyAndReplaceBiometricDraftToLive(srcPrefix, destPrefix, bio.getBioFileId());
						objectStoreHelper.deleteDraftBiometricObject(srcPrefix, bio.getBioFileId());
					}
				}
			}
			if (draft.getDocuments() != null) {
				for (UinDocumentDraft doc : draft.getDocuments()) {
					if (objectStoreHelper.draftDemographicObjectExists(srcPrefix, doc.getDocId())) {
						objectStoreHelper.copyAndReplaceDemographicDraftToLive(srcPrefix, destPrefix, doc.getDocId());
						objectStoreHelper.deleteDraftDemographicObject(srcPrefix, doc.getDocId());
					}
				}
			}

			// Build anonymous profile after biometrics are in the live path so
			// AnonymousProfileHelper can read {uinHash}/Biometrics/{fileId} successfully.
			anonymousProfileHelper.buildAndsaveProfile(true);
			cleanupDraft(regId,draft);
			return constructIdResponse(null, uinObject.getStatusCode(), null, draftVid);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public IdResponseDTO discardDraft(String regId) throws IdRepoAppException {
		try {
			Optional<UinDraft> draftOptional = uinDraftRepo.findByRegId(regId);
			if (draftOptional.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						DISCARD_DRAFT, "RID NOT FOUND IN DB | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			cleanupDraft(regId,draftOptional.get());
			return constructIdResponse(null, "DISCARDED", null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					DISCARD_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}


	public void cleanupDraft(String regId,UinDraft draft) throws IdRepoAppException {
			String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
			if (draft.getBiometrics() != null) {
				for (UinBiometricDraft bio : draft.getBiometrics()) {
					objectStoreHelper.deleteDraftBiometricObject(ridHash, bio.getBioFileId());
				}
			}
			if (draft.getDocuments() != null) {
				for (UinDocumentDraft doc : draft.getDocuments()) {
					objectStoreHelper.deleteDraftDemographicObject(ridHash, doc.getDocId());
				}
			}
			deleteDraftDbRecords(regId);
	}

	private void deleteDraftDbRecords(String regId) {
		uinBiometricDraftRepo.deleteByRegId(regId);
		uinDocumentDraftRepo.deleteByRegId(regId);
		uinDraftRepo.deleteByRegId(regId);
	}

	@Override
	public boolean hasDraft(String regId) throws IdRepoAppException {
		try {
			return uinDraftRepo.existsByRegId(regId);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					"hasDraft", e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	// Cannot use NOT_SUPPORTED here: draft.getUinData() and draft.getDocuments()
	// are lazy fields — they require an open Hibernate session to load.
	public IdResponseDTO getDraft(String regId, Map<String, String> extractionFormats) throws IdRepoAppException {
		return getDraft(regId, extractionFormats, null);
	}

	@Override
	public IdResponseDTO getDraft(String regId, Map<String, String> extractionFormats, String type)
			throws IdRepoAppException {
		final String requestedType = normalizeType(type);
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, DRAFT_RECORD_NOT_FOUND + " | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			UinDraft draft = uinDraft.get();
			String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
			List<DocumentsDTO> documents = new ArrayList<>();

			final boolean includeBiometrics = "biometrics".equals(requestedType) || "all".equals(requestedType);
			final boolean includeSupportingDocuments = "all".equals(requestedType);
			final boolean includeIdentity = "demographics".equals(requestedType) || "all".equals(requestedType);

			if (includeBiometrics && draft.getBiometrics() != null) {
				for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
					byte[] cbeff = extractAndGetCombinedCbeff(ridHash, bioDraft.getBioFileId(), extractionFormats);
					documents.add(new DocumentsDTO(bioDraft.getBiometricFileType(),
							CryptoUtil.encodeToURLSafeBase64(cbeff)));
				}
			}
			if (includeSupportingDocuments && draft.getDocuments() != null) {
				for (UinDocumentDraft docDraft : draft.getDocuments()) {
					byte[] docBytes = objectStoreHelper.getDraftDemographicObject(ridHash, docDraft.getDocId());
					documents.add(new DocumentsDTO(docDraft.getDoccatCode(),
							CryptoUtil.encodeToURLSafeBase64(docBytes)));
				}
			}

			byte[] identityPayload = includeIdentity ? draft.getUinData() : null;
			List<DocumentsDTO> documentsPayload = documents.isEmpty() ? null : documents;
			return constructIdResponse(identityPayload, draft.getStatusCode(), documentsPayload, null);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Validates and normalizes the {@code type} query parameter. Returns the
	 * canonical lowercase value; defaults to {@code "all"} when null/blank.
	 */
	private String normalizeType(String type) throws IdRepoAppException {
		if (type == null || type.trim().isEmpty()) {
			return "all";
		}
		String normalized = type.trim().toLowerCase();
		switch (normalized) {
			case "demographics":
			case "biometrics":
			case "all":
				return normalized;
			default:
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, "Invalid type query parameter: " + type);
				throw new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "type"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO extractBiometrics(String registrationId, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		if (extractionFormats.isEmpty()) {
			return constructIdResponse(null, DRAFTED, null, null);
		}
		try {
			Optional<UinDraft> draftOpt = uinDraftRepo.findByRegId(registrationId);
			if (draftOpt.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, DRAFT_RECORD_NOT_FOUND + " | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			extractBiometricsDraft(extractionFormats, draftOpt.get());
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
		return constructIdResponse(null, DRAFTED, null, null);
	}

	@Override
	public DraftResponseDto getDraftUin(String uin) throws IdRepoAppException {
		String uinHash = super.getUinHash(uin);
		DraftResponseDto draftResponseDto = new DraftResponseDto();
		try {
			UinDraft uinDraft = uinDraftRepo.findByUinHash(uinHash);
			if (uinDraft != null) {
				DraftUinResponseDto dto = new DraftUinResponseDto();
				dto.setRid(uinDraft.getRegId());
				dto.setCreatedDTimes(uinDraft.getCreatedDateTime().toString());
				dto.setAttributes(getAttributeListFromUinData(uinDraft.getUinData()));
				draftResponseDto.setDrafts(List.of(dto));
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (JsonProcessingException e) {
			/*
			 * JsonProcessingException is a data-integrity problem (corrupt UIN data),
			 * not a database access error. Wrapping it as DATABASE_ACCESS_ERROR would
			 * mislead operations teams. Classified as UNKNOWN_ERROR here.
			 */
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, "Failed to parse UIN data | error=" + e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
		return draftResponseDto;
	}
	/**
	 * Wraps a UIN value in the nested identity object structure expected by the
	 * schema (e.g. {@code {"identity": {"UIN": "..."}}} ).
	 */
	private Object generateIdentityObject(Object uin) {
		List<String> pathList = new ArrayList<>(Arrays.asList("identity.UIN".split("\\.")));
		pathList.remove(ROOT_PATH);
		Collections.reverse(pathList);
		for (String segment : pathList) {
			uin = new HashMap<>(Map.of(segment, uin));
		}
		return uin;
	}


	private void updateDemographicData(IdRequestDTO request, UinDraft draftToUpdate)
			throws JSONException, IdRepoAppException, IOException {
		if (Objects.isNull(request.getRequest()) || Objects.isNull(request.getRequest().getIdentity())) {
			return;
		}
		RequestDTO requestDTO = request.getRequest();
		Configuration cfg = Configuration.builder()
				.jsonProvider(new JacksonJsonProvider())
				.mappingProvider(new JacksonMappingProvider())
				.build();
		DocumentContext inputData = JsonPath.using(cfg).parse(requestDTO.getIdentity());
		DocumentContext dbData    = JsonPath.using(cfg).parse(new String(draftToUpdate.getUinData()));

		super.updateVerifiedAttributes(requestDTO, inputData, dbData);

		JSONCompareResult diff = JSONCompare.compareJSON(
				inputData.jsonString(), dbData.jsonString(), JSONCompareMode.LENIENT);
		if (diff.failed()) {
			super.updateJsonObject(draftToUpdate.getUinHash(), inputData, dbData, diff, false);
		}

		draftToUpdate.setUinData(convertToBytes(
				mapper.readValue(dbData.jsonString().getBytes(), new TypeReference<Map<String, Object>>() {})));
		draftToUpdate.setUinDataHash(securityManager.hash(draftToUpdate.getUinData()));
		draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
		draftToUpdate.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
	}

	private void updateDocuments(RequestDTO requestDTO, UinDraft draftToUpdate) throws IdRepoAppException {
		if (Objects.isNull(requestDTO.getDocuments()) || requestDTO.getDocuments().isEmpty()) {
			return;
		}

		Set<String> oldBioFileIds = draftToUpdate.getBiometrics() != null
				? draftToUpdate.getBiometrics().stream().map(UinBiometricDraft::getBioFileId).collect(Collectors.toSet())
				: Collections.emptySet();
		Set<String> oldDocIds = draftToUpdate.getDocuments() != null
				? draftToUpdate.getDocuments().stream().map(UinDocumentDraft::getDocId).collect(Collectors.toSet())
				: Collections.emptySet();

		Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
		String ridHash = objectStoreHelper.getRidHash(draftToUpdate.getRegId());
		// Redirect updateCbeff (in parent) to look for the old biometric at the draft
		// path _draft/{ridHash}/ rather than the live uinHash path.
		uinObject.setUinHash("0_" + ridHash);
		super.updateDocuments(ridHash, uinObject, requestDTO, true);
		updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);

		// Delete orphaned draft files — IDs that were replaced by updateCbeff but are
		// no longer referenced in the draft DB records.
		Set<String> newBioFileIds = draftToUpdate.getBiometrics() != null
				? draftToUpdate.getBiometrics().stream().map(UinBiometricDraft::getBioFileId).collect(Collectors.toSet())
				: Collections.emptySet();
		oldBioFileIds.stream().filter(id -> !newBioFileIds.contains(id))
				.forEach(id -> objectStoreHelper.deleteDraftBiometricObject(ridHash, id));

		Set<String> newDocIds = draftToUpdate.getDocuments() != null
				? draftToUpdate.getDocuments().stream().map(UinDocumentDraft::getDocId).collect(Collectors.toSet())
				: Collections.emptySet();
		oldDocIds.stream().filter(id -> !newDocIds.contains(id))
				.forEach(id -> objectStoreHelper.deleteDraftDemographicObject(ridHash, id));
	}

	private void updateBiometricAndDocumentDrafts(String regId, UinDraft draftToUpdate, Uin uinObject) {
		// ── Biometrics ────────────────────────────────────────────────── //
		Map<String, UinBiometricDraft> draftBioByType = draftToUpdate.getBiometrics().stream()
				.collect(Collectors.toMap(UinBiometricDraft::getBiometricFileType, d -> d));

		Set<String> matchedBioFileIds = new HashSet<>();

		for (UinBiometric uinBio : uinObject.getBiometrics()) {
			UinBiometricDraft existing = draftBioByType.get(uinBio.getBiometricFileType());
			if (existing != null) {
				matchedBioFileIds.add(uinBio.getBioFileId());
				if (!uinBio.getBioFileId().contentEquals(existing.getBioFileId())) {
					existing.setRegId(regId);
					existing.setBioFileId(uinBio.getBioFileId());
					existing.setBiometricFileName(uinBio.getBiometricFileName());
					existing.setBiometricFileHash(uinBio.getBiometricFileHash());
					existing.setUpdatedBy(IdRepoSecurityManager.getUser());
					existing.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
				}
			}
		}

		// Append entity biometrics that have no draft counterpart yet.
		List<UinBiometricDraft> newBioDrafts = uinObject.getBiometrics().stream()
				.filter(b -> !matchedBioFileIds.contains(b.getBioFileId()))
				.map(b -> mapper.convertValue(b, UinBiometricDraft.class))
				.collect(Collectors.toList());
		draftToUpdate.getBiometrics().addAll(newBioDrafts);

		// ── Documents ─────────────────────────────────────────────────── //
		Map<String, UinDocumentDraft> draftDocByCategory = draftToUpdate.getDocuments().stream()
				.collect(Collectors.toMap(UinDocumentDraft::getDoccatCode, d -> d));

		Set<String> matchedDocIds = new HashSet<>();

		for (UinDocument uinDoc : uinObject.getDocuments()) {
			UinDocumentDraft existing = draftDocByCategory.get(uinDoc.getDoccatCode());
			if (existing != null) {
				matchedDocIds.add(uinDoc.getDocId());
				if (!uinDoc.getDocId().contentEquals(existing.getDocId())) {
					existing.setRegId(regId);
					existing.setDocId(uinDoc.getDocId());
					existing.setDoctypCode(uinDoc.getDoctypCode());
					existing.setDocName(uinDoc.getDocName());
					existing.setDocfmtCode(uinDoc.getDocfmtCode());
					existing.setDocHash(uinDoc.getDocHash());
					existing.setUpdatedBy(IdRepoSecurityManager.getUser());
					existing.setUpdatedDateTime(uinDoc.getUpdatedDateTime());
				}
			}
		}

		// Append entity documents that have no draft counterpart yet.
		List<UinDocumentDraft> newDocDrafts = uinObject.getDocuments().stream()
				.filter(d -> !matchedDocIds.contains(d.getDocId()))
				.map(d -> mapper.convertValue(d, UinDocumentDraft.class))
				.collect(Collectors.toList());
		draftToUpdate.getDocuments().addAll(newDocDrafts);

		// Stamp all draft entries with the current registration ID.
		draftToUpdate.getBiometrics().forEach(b -> b.setRegId(regId));
		draftToUpdate.getDocuments().forEach(d -> d.setRegId(regId));
	}

	private IdRequestDTO buildRequest(String regId, UinDraft draft) {
		IdRequestDTO idRequest = new IdRequestDTO();
		RequestDTO request = new RequestDTO();
		request.setRegistrationId(regId);
		try {
			Map<String, Object> identityData = mapper.readValue(draft.getUinData(),
					new TypeReference<Map<String, Object>>() {});
			request.setVerifiedAttributes(
					mapper.convertValue(identityData.get(VERIFIED_ATTRIBUTES),
							new TypeReference<List<String>>() {}));
			identityData.remove(VERIFIED_ATTRIBUTES);
			request.setIdentity(identityData);
		} catch (IOException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, "Failed to parse draft UIN data | error=" + e.getMessage());
			throw new IdRepoAppUncheckedException(UNKNOWN_ERROR, e);
		}

		idRequest.setRequest(request);
		return idRequest;
	}

	private void validateRequest(RequestDTO request) throws IdRepoDataValidationException {
		Errors errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
		validator.validateRequest(request, errors, "create");
		DataValidationUtil.validate(errors);
	}

	private void publishDocuments(UinDraft draft, Uin uinObject) {
		List<UinBiometric> uinBiometricList = draft.getBiometrics() == null ? Collections.emptyList()
				: draft.getBiometrics().stream().map(bio -> {
					UinBiometric uinBio = mapper.convertValue(bio, UinBiometric.class);
					uinBio.setUinRefId(uinObject.getUinRefId());
					uinBio.setLangCode("");
					return uinBio;
				}).collect(Collectors.toList());
		uinBiometricRepo.saveAll(uinBiometricList);

		List<UinDocument> uinDocumentList = draft.getDocuments() == null ? Collections.emptyList()
				: draft.getDocuments().stream().map(doc -> {
					UinDocument uinDoc = mapper.convertValue(doc, UinDocument.class);
					uinDoc.setUinRefId(uinObject.getUinRefId());
					uinDoc.setLangCode("");
					return uinDoc;
				}).collect(Collectors.toList());
		uinDocumentRepo.saveAll(uinDocumentList);
	}

	private String decryptUin(String encryptedUin, String uinHash) throws IdRepoAppException {
		String salt = uinEncryptSaltRepo.getOne(
				Integer.valueOf(encryptedUin.split(SPLITTER)[0])).getSalt();
		String uin = new String(securityManager.decryptWithSalt(
				CryptoUtil.decodeURLSafeBase64(StringUtils.substringAfter(encryptedUin, SPLITTER)),
				CryptoUtil.decodePlainBase64(salt),
				uinRefId));
		if (!StringUtils.equals(super.getUinHash(uin), uinHash)) {
			throw new IdRepoAppUncheckedException(UIN_HASH_MISMATCH);
		}
		return uin;
	}

	private void extractBiometricsDraft(Map<String, String> extractionFormats, UinDraft draft)
			throws IdRepoAppException {
		String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
		if (draft.getBiometrics() == null) {
			return;
		}
		for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
			try {
				deleteExistingExtractedBioData(extractionFormats, ridHash, bioDraft);
				extractAndGetCombinedCbeff(ridHash, bioDraft.getBioFileId(), extractionFormats);
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, "DB error during bio extraction | fileId=" + bioDraft.getBioFileId()
								+ " | error=" + e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			} catch (IdRepoAppException e) {
				throw e;
			} catch (Exception e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, "Bio extraction failed | error=" + e.getMessage());
				throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
			}
		}
	}

	private void deleteExistingExtractedBioData(
			Map<String, String> extractionFormats,
			String uinHash,
			UinBiometricDraft bioDraft) {
		for (Entry<String, String> extractionFormat : extractionFormats.entrySet()) {
			String targetFile = buildExtractionFileName(extractionFormat, bioDraft.getBioFileId());
			try {
				super.objectStoreHelper.deleteBiometricObject(uinHash, targetFile);
			} catch (Exception e) {
				idrepoDraftLogger.warn(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						"deleteExistingExtractedBioData",
						"Failed to delete extraction file (non-fatal) | file=" + targetFile
								+ " | error=" + e.getMessage());
				// Continue — a stale file is preferable to aborting the entire extraction.
			}
		}
	}

	private byte[] extractAndGetCombinedCbeff(String uinHash, String bioFileId,
											  Map<String, String> extractionFormats) throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormats(uinHash, bioFileId, extractionFormats,
				super.objectStoreHelper.getDraftBiometricObject(uinHash, bioFileId));
	}

	private String buildExtractionFileName(Entry<String, String> extractionFormat, String bioFileId) {
		return bioFileId.split("\\.")[0]
				+ DOT
				+ getModalityForFormat(extractionFormat.getKey())
				+ DOT
				+ extractionFormat.getValue();
	}

	private String getModalityForFormat(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, "");
	}

	private IdResponseDTO constructIdResponse(byte[] uinData, String status,
											  List<DocumentsDTO> documents, String vid) {
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus(status);

		if (Objects.nonNull(documents)) {
			response.setDocuments(documents);
		}

		if (Objects.nonNull(uinData)) {
			/*
			 * Use mapper.readValue directly for ObjectNode — convertToObject in the
			 * parent class only exposes a TypeReference overload, and ObjectNode is
			 * a concrete Jackson type that readValue handles natively without any cast.
			 */
			try {
				ObjectNode identityObject = mapper.readValue(uinData, ObjectNode.class);
				response.setVerifiedAttributes(
						mapper.convertValue(identityObject.get(VERIFIED_ATTRIBUTES),
								new TypeReference<List<String>>() {}));
				identityObject.remove(VERIFIED_ATTRIBUTES); // intentional — field is promoted to top-level
				response.setIdentity(identityObject);
			} catch (IOException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						"constructIdResponse", "Failed to parse UIN data | error=" + e.getMessage());
				throw new IdRepoAppUncheckedException(UNKNOWN_ERROR, e);
			}
		}

		idResponse.setResponse(response);

		if (Objects.nonNull(vid)) {
			idResponse.setMetadata(Map.of("vid", vid));
		}

		return idResponse;
	}

	private List<String> getAttributeListFromUinData(byte[] uinData) throws JsonProcessingException {
		String excludedAttributeListProperty =
				environment.getProperty(EXCLUDED_ATTRIBUTE_LIST, DEFAULT_ATTRIBUTE_LIST);
		Set<String> excludedAttributes = new HashSet<>(
				Arrays.asList(excludedAttributeListProperty.split(COMMA)));

		JsonNode jsonNode = mapper.readTree(new String(uinData, StandardCharsets.UTF_8));
		List<String> attributeList = new ArrayList<>();
		jsonNode.fieldNames().forEachRemaining(key -> {
			if (!excludedAttributes.contains(key)) {
				attributeList.add(key);
			}
		});
		return attributeList;
	}
}