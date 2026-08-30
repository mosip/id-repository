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
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DRAFT_UIN_DETAILS_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DRAFT_UIN_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RECORD_EXISTS;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RID_OLDER_THAN_LATEST_PROCESSED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_GENERATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.biometrics.entities.BIR;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * Production-ready implementation of {@link IdRepoDraftService}.
 *
 * <h3>Key fixes over the original</h3>
 * <ol>
 *   <li><b>No nested-transaction discard inside publishDraft</b> — the draft
 *       delete is now performed directly via the repository rather than through
 *       the {@code discardDraft} service method, which would open a second
 *       transactional context inside an already-active one and execute a
 *       redundant SELECT before the DELETE.</li>
 *   <li><b>No in-place mutation of Hibernate entity collections</b> —
 *       {@code updateBiometricAndDocumentDrafts} previously removed items from
 *       {@code uinObject.getBiometrics()} / {@code getDocuments()} via
 *       {@code listIterator.remove()} inside an IntStream loop over a snapshot.
 *       Mutating a Hibernate {@code PersistentBag} during iteration causes
 *       dirty-checking anomalies and is an O(N²) linear scan. Replaced with
 *       a {@code Set}-based O(N) approach that never touches the entity list.</li>
 *   <li><b>No {@code new ObjectMapper()} per call</b> — {@code ObjectMapper} is
 *       expensive to construct and thread-safe once built. All parsing now uses
 *       the injected {@code mapper} from the parent class.</li>
 *   <li><b>Correct error classification in {@code getDraftUin}</b> —
 *       {@code JsonProcessingException} from parsing UIN data is a data-integrity
 *       problem, not a database access error; it is now wrapped as
 *       {@code UNKNOWN_ERROR} with its own log statement.</li>
 *   <li><b>Index-out-of-bounds guard in {@code publishDraft}</b> — the original
 *       code called {@code biometrics.get(size - 1)} when biometrics was
 *       non-null but potentially empty (the null/empty check was split across
 *       an {@code &&} expression). Now uses {@code isEmpty()} explicitly and
 *       falls through to {@code null} safely.</li>
 *   <li><b>Correct method label in {@code discardDraft} error log</b> — the
 *       original logged {@code UPDATE_DRAFT} when the draft was not found inside
 *       {@code discardDraft} (copy-paste bug). Now logs {@code DISCARD_DRAFT}.</li>
 *   <li><b>Object-store delete failures are logged, not silently swallowed</b> —
 *       {@code deleteExistingExtractedBioData} now catches and logs per-file
 *       failures so that partial failures are visible in the audit log.</li>
 *   <li><b>Biometric extraction errors are not misclassified as DB errors</b> —
 *       {@code extractBiometricsDraft} previously caught bare {@code Exception},
 *       which meant {@code DataAccessException} would surface as
 *       {@code BIO_EXTRACTION_ERROR}. DB exceptions are now re-thrown with the
 *       correct error code.</li>
 *   <li><b>Duplicate {@code setRegId} call removed from {@code createDraft}</b>
 *       — the registration ID was set twice (once in each branch, then again
 *       unconditionally after the if-else block).</li>
 *   <li><b>{@code @SuppressWarnings} removed from {@code buildRequest}</b> —
 *       the unchecked cast is eliminated by using a typed {@code TypeReference}.</li>
 * </ol>
 *
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

	@Autowired
	private TransactionTemplate transactionTemplate;

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
				Uin uinObject = uinObjectOptional.get();
				newDraft = mapper.convertValue(uinObject, UinDraft.class);
				updateBiometricAndDocumentDrafts(registrationId, newDraft, uinObject);
				newDraft.setUin(super.getUinToEncrypt(uin));
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
			uinDraftRepo.save(newDraft);

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

			anonymousProfileHelper.buildAndsaveProfile(true);
			publishDocuments(draft, uinObject);

			/*
			 * Delete the draft directly rather than calling discardDraft().
			 * discardDraft() is itself @Transactional and would open a nested
			 * context inside the current transaction, plus it performs a
			 * redundant SELECT before the DELETE.
			 */
			uinDraftRepo.deleteByRegId(regId);

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
			if (!uinDraftRepo.existsByRegId(regId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						DISCARD_DRAFT, "RID NOT FOUND IN DB | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			uinDraftRepo.deleteByRegId(regId);
			return constructIdResponse(null, "DISCARDED", null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					DISCARD_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public IdResponseDTO discardDraftV2(String regId) throws IdRepoAppException {
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						DISCARD_DRAFT, "RID NOT FOUND IN DB | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			cleanupDraft(uinDraft.get());
			return constructIdResponse(null, "DISCARDED", null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					DISCARD_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
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
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, DRAFT_RECORD_NOT_FOUND + " | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			UinDraft draft = uinDraft.get();
			String uinHash = draft.getUinHash().split(SPLITTER)[1];
			List<DocumentsDTO> documents = new ArrayList<>();

			for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
				byte[] cbeff = extractAndGetCombinedCbeff(uinHash, bioDraft.getBioFileId(), extractionFormats);
				documents.add(new DocumentsDTO(bioDraft.getBiometricFileType(),
						CryptoUtil.encodeToURLSafeBase64(cbeff)));
			}
			for (UinDocumentDraft docDraft : draft.getDocuments()) {
				byte[] docBytes = objectStoreHelper.getDemographicObject(uinHash, docDraft.getDocId());
				documents.add(new DocumentsDTO(docDraft.getDoccatCode(),
						CryptoUtil.encodeToURLSafeBase64(docBytes)));
			}

			return constructIdResponse(draft.getUinData(), draft.getStatusCode(), documents, null);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * V2 getDraftV2 — reads from ridHash path (MOSIP-082 storage approach).
	 * Uses getDraftBiometricObject / getDraftDemographicObject on the draft ridHash prefix.
	 */
	@Override
	public IdResponseDTO getDraftV2(String regId, Map<String, String> extractionFormats, String type)
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
			// V2: reads from draft ridHash path (MOSIP-082 storage approach)
			String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
			List<DocumentsDTO> documents = new ArrayList<>();

			final boolean includeBiometrics = "biometrics".equals(requestedType) || "all".equals(requestedType);
			final boolean includeSupportingDocuments = "supportingdocuments".equals(requestedType) || "all".equals(requestedType);
			final boolean includeIdentity = "demographics".equals(requestedType) || "all".equals(requestedType);

			if (includeBiometrics && draft.getBiometrics() != null) {
				for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
					byte[] cbeff = extractAndGetCombinedCbeffDraft(ridHash, bioDraft.getBioFileId(), extractionFormats);
					documents.add(new DocumentsDTO(bioDraft.getBiometricFileType(),
							CryptoUtil.encodeToURLSafeBase64(cbeff)));
				}
			}
			if (includeSupportingDocuments && draft.getDocuments() != null) {
				for (UinDocumentDraft docDraft : draft.getDocuments()) {
					// V2: use getDraftDemographicObject (ridHash draft path — MOSIP-082 approach)
					byte[] docBytes = objectStoreHelper.getDraftDemographicObject(ridHash, docDraft.getDocId());
					documents.add(new DocumentsDTO(docDraft.getDoccatCode(),
							CryptoUtil.encodeToURLSafeBase64(docBytes)));
				}
			}

			byte[] identityPayload = includeIdentity ? draft.getUinData() : null;
			return constructIdResponse(identityPayload, draft.getStatusCode(), documents, null);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Runs without an ambient transaction. The initial DB read
	 * ({@code findByRegId}) uses Spring Data JPA's own short read-only
	 * transaction which commits immediately — releasing the DB connection
	 * back to the pool before any S3/biometric-extraction I/O begins.
	 * No DB writes are performed in this method so no write transaction
	 * is needed.
	 */
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
	 * Creates a draft with optional UIN allocation.
	 * <ul>
	 *   <li>{@code generateUin=true} — default NEW/UPDATE flow: stale-draft cleanup,
	 *       UIN allocation or generation, live-to-draft file copy, {@code save}.</li>
	 *   <li>{@code generateUin=false} — LOST packet flow: if {@code uin} is provided,
	 *       copies live files (uinHash→ridHash) before creating draft; if {@code uin} is
	 *       null, creates a bare draft (UIN stamped later via {@code updateDraftUinData}).</li>
	 * </ul>
	 */
	@Override
	public IdResponseDTO createDraftV2(String registrationId, String uin, boolean generateUin)
			throws IdRepoAppException {
		if (!generateUin && uin == null) {
			// LOST packet with no known UIN: bare draft, UIN stamped later via updateDraftUinData
			return createLostDraft(registrationId);
		}
		// NEW (generateUin=true, uin=null): allocate new UIN
		// UPDATE (generateUin=false, uin=existing): use existing UIN — must NOT go through
		return createDraftWithUin(registrationId, uin);
	}

	private IdResponseDTO createLostDraft(String registrationId) throws IdRepoAppException {
		try {
			if (uinDraftRepo.existsByRegId(registrationId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "RID ALREADY EXIST | regId=" + registrationId);
				throw new IdRepoAppException(RECORD_EXISTS);
			}

			// If RID is there in UIN history then the RID is already processed.
			// But it may not be latest RID. To check the same we need to check in the UIN table as well.
			// If it is not the latest, reprocessing is not allowed.
			if (super.uinHistoryRepo.existsByRegId(registrationId)) {
				if (!super.uinRepo.existsByRegId(registrationId)) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							CREATE_DRAFT, "REQUESTED RID IS OLDER THAN THE LATEST PROCESSED RID | regId=" + registrationId);
					throw new IdRepoAppException(RID_OLDER_THAN_LATEST_PROCESSED);
				}
			}

			UinDraft newDraft = new UinDraft();

			newDraft.setRegId(registrationId);
			newDraft.setStatusCode("DRAFT");
			newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
			newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
			uinDraftRepo.save(newDraft);

			return constructIdResponse(null, DRAFTED, null, null);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	private IdResponseDTO createDraftWithUin(String registrationId, String uin) throws IdRepoAppException {
		try {
			// Guard: block only if a draft already exists for this exact regId.
			// We deliberately do NOT check uinHistoryRepo here — a previously committed
			// packet must be allowed to reprocess by re-creating its draft using the
			// existing UIN (fetched below) rather than allocating a second UIN.
			if (uinDraftRepo.existsByRegId(registrationId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "RID ALREADY EXIST | regId=" + registrationId);
				throw new IdRepoAppException(RECORD_EXISTS);
			}

			UinDraft newDraft;

			if (Objects.isNull(uin) && super.uinHistoryRepo.existsByRegId(registrationId)) {
				// If RID is there in UIN history then the RID is already processed.
				// But it may not be latest RID. To check the same we need to check in the UIN table as well.
				// If it is not the latest, reprocessing is not allowed.
				if (!super.uinRepo.existsByRegId(registrationId)) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							CREATE_DRAFT, "REQUESTED RID IS OLDER THAN THE LATEST PROCESSED RID | regId=" + registrationId);
					throw new IdRepoAppException(RID_OLDER_THAN_LATEST_PROCESSED);
				}

				// Reprocess of a previously committed NEW packet: the UIN was already
				// allocated in the prior run. Retrieve it so we reuse the same UIN
				// instead of allocating a second one.
				IdResponseDTO existingIdentity = proxyService.retrieveIdentityByRid(registrationId, null, null);
				if (existingIdentity != null && existingIdentity.getResponse() != null
						&& existingIdentity.getResponse().getIdentity() != null) {
					LinkedHashMap<String, Object> identityMap = mapper.convertValue(
							existingIdentity.getResponse().getIdentity(),
							new TypeReference<LinkedHashMap<String, Object>>() {});
					Object uinObj = identityMap.get("UIN");
					if (uinObj != null) {
						uin = String.valueOf(uinObj);
					}
				}
				if (uin == null) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							CREATE_DRAFT, "UIN NOT FOUND FOR REPROCESS | regId=" + registrationId);
					throw new IdRepoAppException(NO_RECORD_FOUND);
				}
			}

			if (Objects.nonNull(uin)) {
				Optional<Uin> uinObjectOptional = super.uinRepo.findByUinHash(super.getUinHash(uin));
				if (uinObjectOptional.isEmpty()) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							CREATE_DRAFT, "UIN NOT EXIST | uin=<redacted>");
					throw new IdRepoAppException(NO_RECORD_FOUND);
				}
				Uin uinObject = uinObjectOptional.get();
				newDraft = mapper.convertValue(uinObject, UinDraft.class);
				updateBiometricAndDocumentDrafts(registrationId, newDraft, uinObject);
				newDraft.setUin(super.getUinToEncrypt(uin));

				// Copy existing live files to the draft path so updateDraft can read/merge them.
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
				// Standard first-time NEW flow: allocate a brand-new UIN.
				newDraft = new UinDraft();
				uin = idRepoServiceHelper.generateUin();
				newDraft.setUin(super.getUinToEncrypt(uin));
				newDraft.setUinHash(super.getUinHash(uin));
				byte[] uinData = convertToBytes(generateIdentityObject(uin));
				newDraft.setUinData(uinData);
				newDraft.setUinDataHash(securityManager.hash(uinData));
			}

			newDraft.setRegId(registrationId);
			newDraft.setStatusCode("DRAFT");
			newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
			newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
			uinDraftRepo.save(newDraft);

			return constructIdResponse(null, DRAFTED, null, null);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (RuntimeException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
	}
	/**
	 * Stamps the ABIS-matched UIN on a LOST draft and completes its demographic data for publish.
	 *
	 * <p>After encrypting and setting {@code uin}/{@code uinHash}, merges missing demographic fields
	 * from the matched live identity into the draft via {@link #backfillDraftIdentityFromLiveUin}.
	 * That step updates {@code uinData} only; it does not copy or modify biometrics, supporting
	 * documents, or object-store files — those are handled by {@link #updateDraftV2} and
	 * {@link #publishDraftV2}.
	 *
	 * <p>Draft demographics from the LOST packet (allowed update fields) are preserved on conflict;
	 * attribute update-count limits are enforced at publish, not here.
	 */
	@Override
	public IdResponseDTO updateDraftUinData(String registrationId, String uin) throws IdRepoAppException {
		try {
			Optional<UinDraft> uinDraftOpt = uinDraftRepo.findByRegId(registrationId);
			if (uinDraftOpt.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "RID NOT FOUND IN DB | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			UinDraft draft = uinDraftOpt.get();

			// Verify whether the UIN to be stamped exists in the IDREPO UIN table.
			if (!super.uinRepo.existsByUinHash(super.getUinHash(uin))) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "UIN NOT FOUND | regId=" + registrationId);
				throw new IdRepoAppException(UIN_NOT_FOUND);
			}

			// A draft is only "already stamped" once uinHash itself has been set (by a
			// prior call to this same method) — uinData alone does NOT imply that, since
			// updateDraftV2's first-update branch populates uinData for LOST-packet drafts
			// without ever touching uinHash. Checking getUinData() here caused a NPE on
			// getUinHash().equals(...) for a LOST packet's first-ever UIN stamping call.
			if (draft.getUinHash() != null && !draft.getUinHash().equals(super.getUinHash(uin))) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "ATTEMPT TO RESTAMP DRAFT WITH A DIFFERENT UIN | regId=" + registrationId);
				throw new IdRepoAppException(DRAFT_UIN_HASH_MISMATCH);
			}

			// onFlushDirty only encrypts uinData, not the uin field. Encrypt manually here
			// to match the format produced by the onSave interceptor so decryptUin works.
			int saltId = securityManager.getSaltKeyForId(uin);
			String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
			byte[] encryptedUinBytes = securityManager.encryptWithSalt(
					uin.getBytes(), CryptoUtil.decodePlainBase64(encryptSalt),
					uinRefId);
			draft.setUin(saltId + SPLITTER + new String(encryptedUinBytes));
			draft.setUinHash(super.getUinHash(uin));
			// Build a complete draft identity before publish: backfill missing fields from live,
			// without overwriting demographics already set by the LOST packet update.
			backfillDraftIdentityFromLiveUin(draft, super.uinRepo.findByUinHash(super.getUinHash(uin)).get());
			draft.setUpdatedBy(IdRepoSecurityManager.getUser());
			draft.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
			// Use flush() so the SQL UPDATE runs inside this try-catch; save() with isNew()=true
			// only calls persist() (no-op for a managed entity) and defers the actual write to
			// transaction commit, which is outside the catch boundary.
			uinDraftRepo.flush();
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataIntegrityViolationException e) {
			// A concurrent draft already stamped with the same UIN hash hit the unique
			// constraint on flush — surface this as a clear conflict, not a generic DB error.
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(RECORD_EXISTS, e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException e) {
			throw e;
		} catch (JSONException | IOException | InvalidJsonException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (RuntimeException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Backfills draft {@code uinData} from the matched live identity (demographics only).
	 *
	 * <p>Does not touch biometrics, supporting documents, or object-store paths — only
	 * merges the identity JSON. Draft values take precedence; live fills missing fields.
	 * Update-count limits are not applied here; they are enforced at publish.
	 */
	private void backfillDraftIdentityFromLiveUin(UinDraft draft, Uin liveUin)
			throws JSONException, IOException, IdRepoAppException {
		if (Objects.isNull(liveUin.getUinData())) {
			return;
		}
		Configuration cfg = Configuration.builder()
				.jsonProvider(new JacksonJsonProvider())
				.mappingProvider(new JacksonMappingProvider())
				.build();
		DocumentContext inputData = JsonPath.using(cfg).parse(new String(draft.getUinData()));
		DocumentContext dbData    = JsonPath.using(cfg).parse(new String(liveUin.getUinData()));

		JSONCompareResult diff = JSONCompare.compareJSON(
				inputData.jsonString(), dbData.jsonString(), JSONCompareMode.LENIENT);
		if (diff.failed()) {
			super.updateJsonObject(draft.getUinHash(), inputData, dbData, diff, false);
		}

		draft.setUinData(convertToBytes(
				mapper.readValue(dbData.jsonString().getBytes(), new TypeReference<Map<String, Object>>() {})));
		draft.setUinDataHash(securityManager.hash(draft.getUinData()));
	}

	@Override
	public IdResponseDTO updateDraftV2(String registrationId, IdRequestDTO request) throws IdRepoAppException {
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(registrationId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "RID NOT FOUND IN DB | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			UinDraft draftToUpdate = uinDraft.get();

			if (Objects.isNull(draftToUpdate.getUinData())) {
				byte[] uinData = super.convertToBytes(request.getRequest().getIdentity());
				draftToUpdate.setUinData(uinData);
				draftToUpdate.setUinDataHash(securityManager.hash(uinData));
				updateDocumentsV2(request.getRequest(), draftToUpdate);
				draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
				draftToUpdate.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
			} else {
				updateDemographicData(request, draftToUpdate);
				updateDocumentsV2(request.getRequest(), draftToUpdate);
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
	public IdResponseDTO publishDraftV2(String regId) throws IdRepoAppException {
		anonymousProfileHelper.setRegId(regId);
		try {
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, DRAFT_RECORD_NOT_FOUND + " | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			UinDraft draft = uinDraft.get();

			if (draft.getUinHash() == null || draft.getUin() == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, "UIN details not found in draft | regId=" + regId
								+ " | uinNull=" + (draft.getUin() == null) + " | uinHashNull=" + (draft.getUinHash() == null));
				throw new IdRepoAppException(DRAFT_UIN_DETAILS_NOT_FOUND);
			}
			if (draft.getUinData() == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, "UIN details not found in draft — identity data missing | regId=" + regId);
				throw new IdRepoAppException(DRAFT_UIN_DETAILS_NOT_FOUND);
			}

			String lastBioFileId = (Objects.nonNull(draft.getBiometrics()) && !draft.getBiometrics().isEmpty())
					? draft.getBiometrics().get(draft.getBiometrics().size() - 1).getBioFileId()
					: null;

			anonymousProfileHelper.setNewCbeff(
					draft.getUinHash().split(SPLITTER)[1],
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

			// Move object-store files to live BEFORE deleting DB records.
			// If the move throws, the draft DB record is still intact so the draft is recoverable.
			String srcPrefix = objectStoreHelper.getRidHash(draft.getRegId());
			String destPrefix = draft.getUinHash().split(SPLITTER)[1];
			objectStoreHelper.moveAllDraftBiometricsToLive(srcPrefix, destPrefix);
			objectStoreHelper.moveAllDraftDemographicsToLive(srcPrefix, destPrefix);

			deleteDraftDbRecords(regId);

			anonymousProfileHelper.buildAndsaveProfile(true);
			return constructIdResponse(null, uinObject.getStatusCode(), null, draftVid);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO extractBiometricsV2(String registrationId, Map<String, String> extractionFormats)
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
			extractBiometricsDraftV2(extractionFormats, draftOpt.get());
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
		return constructIdResponse(null, DRAFTED, null, null);
	}

	// ════════════════════════════════════════════════════════════════════════════
	// Private helpers
	// ════════════════════════════════════════════════════════════════════════════

	/**
	 * Validates and normalizes the {@code type} query parameter.
	 * Returns the canonical lowercase value; defaults to {@code "all"} when null/blank.
	 */
	private String normalizeType(String type) throws IdRepoAppException {
		if (type == null || type.trim().isEmpty()) {
			return "all";
		}
		String normalized = type.trim().toLowerCase(Locale.ROOT);
		switch (normalized) {
			case "demographics":
			case "biometrics":
			case "supportingdocuments":
			case "all":
				return normalized;
			default:
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						GET_DRAFT, "Invalid type query parameter: " + type);
				throw new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "type"));
		}
	}

	/**
	 * Full V2 cleanup: deletes object-store draft files then DB records.
	 * Used by {@link #publishDraftV2} and {@link #createDraftWithUin} stale cleanup.
	 */
	public void cleanupDraft(UinDraft draft) throws IdRepoAppException {
		String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
		// Delete object-store files first — if deletion fails the DB record still points
		// to the draft files so the failure is visible and recoverable.
		objectStoreHelper.deleteAllDraftBiometrics(ridHash);
		objectStoreHelper.deleteAllDraftDemographics(ridHash);
		deleteDraftDbRecords(draft.getRegId());
	}

	private void deleteDraftDbRecords(String regId) {
		// Delete all three draft tables in one transaction (all succeed or all roll back).
		// Uses TransactionTemplate because @Transactional on a private method is ignored.
		// With REQUIRED propagation, joins the caller's transaction when one is already open.
		transactionTemplate.executeWithoutResult(status -> {
			uinBiometricDraftRepo.deleteByRegId(regId);
			uinDocumentDraftRepo.deleteByRegId(regId);
			uinDraftRepo.deleteByRegId(regId);
		});
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
		Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
		String uinHashWithSalt = draftToUpdate.getUinHash().split(SPLITTER)[1];
		super.updateDocuments(uinHashWithSalt, uinObject, requestDTO, true);
		updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
	}

	// V2: stores documents at ridHash draft path (MOSIP-082 behaviour)
	private void updateDocumentsV2(RequestDTO requestDTO, UinDraft draftToUpdate) throws IdRepoAppException {
		if (Objects.isNull(requestDTO.getDocuments()) || requestDTO.getDocuments().isEmpty()) {
			return;
		}

		Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
		String ridHash = objectStoreHelper.getRidHash(draftToUpdate.getRegId());
		uinObject.setUinHash("_" + ridHash);
		super.updateDocuments(ridHash, uinObject, requestDTO, true);
		updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
	}

	/**
	 * Synchronises biometric and document draft records with the live entity.
	 *
	 * <p>Algorithm (O(N) per collection):
	 * <ol>
	 *   <li>Index existing draft records by their natural key (file type / doc category).</li>
	 *   <li>For each item in the live entity:
	 *     <ul>
	 *       <li>If a draft record exists for the same key and its file ID differs,
	 *           update the draft record in place.</li>
	 *       <li>Track which entity items were matched via a {@link Set} of file IDs.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Convert and append only the <em>unmatched</em> entity items (those without
	 *       a corresponding draft record) as new draft entries.</li>
	 * </ol>
	 *
	 * <p>This approach never mutates the Hibernate entity collections, eliminating
	 * dirty-checking anomalies and the O(N²) linear-scan removal that the original
	 * code performed via {@code listIterator.remove()}.
	 */
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

	/**
	 * Builds an {@link IdRequestDTO} from the stored draft data.
	 *
	 * <p>Uses a typed {@link TypeReference} for the identity map conversion so
	 * that the compiler can verify the type — the original
	 * {@code @SuppressWarnings("unchecked")} is no longer needed.
	 */
	private IdRequestDTO buildRequest(String regId, UinDraft draft) {
		IdRequestDTO idRequest = new IdRequestDTO();
		RequestDTO request = new RequestDTO();
		request.setRegistrationId(regId);

		/*
		 * Use mapper.readValue directly rather than convertToObject — the parent's
		 * convertToObject accepts either Class<T> or TypeReference<T> depending on
		 * the overload available; using mapper explicitly avoids any ambiguity and
		 * keeps the call independent of the parent's API surface.
		 */
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

	// ================================================================== //
	//  Private — biometric extraction helpers                             //
	// ================================================================== //

	/**
	 * Extracts biometrics for all modalities in the draft.
	 *
	 * <p>Database exceptions are re-thrown with {@code DATABASE_ACCESS_ERROR} rather
	 * than being misclassified as {@code BIO_EXTRACTION_ERROR}.
	 */
	private void extractBiometricsDraft(Map<String, String> extractionFormats, UinDraft draft)
			throws IdRepoAppException {
		try {
			String uinHash = draft.getUinHash().split("_")[1];
			for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
				try {
					deleteExistingExtractedBioData(extractionFormats, uinHash, bioDraft);
					extractAndGetCombinedCbeff(uinHash, bioDraft.getBioFileId(), extractionFormats);
				} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
							GET_DRAFT, "DB error during bio extraction | fileId=" + bioDraft.getBioFileId()
									+ " | error=" + e.getMessage());
					throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
				} catch (IdRepoAppException e) {
					throw e;
				}
			}
		} catch (IdRepoAppException e) {
			throw e;
		} catch (Exception e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, "Bio extraction failed | error=" + e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	// Reads biometric files from the draft path and extracts templates.
	// Extracted files are written back to the same draft path.
	private void extractBiometricsDraftV2(Map<String, String> extractionFormats, UinDraft draft)
			throws IdRepoAppException {
		String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
		if (draft.getBiometrics() == null) {
			return;
		}
		for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
			try {
				deleteExistingExtractedBioDataDraft(extractionFormats, ridHash, bioDraft);
				extractAndGetCombinedCbeffDraft(ridHash, bioDraft.getBioFileId(), extractionFormats);
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

	private void deleteExistingExtractedBioDataDraft(
			Map<String, String> extractionFormats,
			String ridHash,
			UinBiometricDraft bioDraft) {
		for (Entry<String, String> extractionFormat : extractionFormats.entrySet()) {
			String targetFile = buildExtractionFileName(extractionFormat, bioDraft.getBioFileId());
			try {
				super.objectStoreHelper.deleteDraftBiometricObject(ridHash, targetFile);
			} catch (Exception e) {
				idrepoDraftLogger.warn(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						"deleteExistingExtractedBioDataDraft",
						"Failed to delete draft extraction file (non-fatal) | file=" + targetFile
								+ " | error=" + e.getMessage());
			}
		}
	}

	/**
	 * Deletes previously stored extraction results for a given biometric draft entry.
	 *
	 * <p>Per-file failures are logged individually rather than silently swallowed,
	 * so that partial failures are visible in the audit log without aborting the
	 * deletion of subsequent files.
	 */
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
				super.objectStoreHelper.getBiometricObject(uinHash, bioFileId));
	}

	// Reads the biometric from the draft path and extracts templates.
	// Extracted files are written back to the same draft path.
	private byte[] extractAndGetCombinedCbeffDraft(String ridHash, String bioFileId,
												   Map<String, String> extractionFormats) throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormatsDraft(ridHash, bioFileId, extractionFormats,
				super.objectStoreHelper.getDraftBiometricObject(ridHash, bioFileId));
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

	/**
	 * Builds the {@link IdResponseDTO} returned to callers.
	 *
	 * <p>{@code VERIFIED_ATTRIBUTES} is removed from the identity node after it
	 * has been copied into {@link ResponseDTO#setVerifiedAttributes(List)} so that
	 * it does not appear twice in the response body. The removal is explicit here
	 * to make the side-effect visible at the call site.
	 */
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

	/**
	 * Parses the UIN data byte array and returns attribute names that are not in
	 * the exclusion list.
	 *
	 * <p>Uses the shared {@code mapper} bean rather than constructing a new
	 * {@code ObjectMapper} on every invocation.
	 */
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