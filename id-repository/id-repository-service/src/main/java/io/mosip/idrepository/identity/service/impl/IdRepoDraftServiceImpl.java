package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CREATE_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DISCARD_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DOT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DRAFTED;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DRAFT_RECORD_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXCLUDED_ATTRIBUTE_LIST;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX;
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
import io.mosip.idrepository.core.dto.VerificationMetadata;
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
import io.mosip.idrepository.manager.CredentialStatusManager;
import io.mosip.idrepository.pipeline.PendingCredentialSync;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.DateUtils2;

import org.hibernate.Hibernate;
import org.hibernate.exception.JDBCConnectionException;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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
 * Draft identity workflow: create, update, publish, and discard staged UIN changes.
 * <p>
 * A draft holds the identity changes of one registration packet in {@code uin_draft},
 * {@code uin_biometric_draft} and {@code uin_document_draft} until it is published to the
 * live identity or discarded. Two flows are implemented:
 * <ul>
 * <li><b>V1</b> ({@code createDraft}, {@code updateDraft}, {@code publishDraft}, ...):
 * draft files are written under the UIN hash path.</li>
 * <li><b>V2</b> ({@code createDraftV2}, {@code updateDraftUinData}, {@code updateDraftV2},
 * {@code publishDraftV2}, {@code getDraftV2}, {@code extractBiometricsV2},
 * {@code discardDraftV2}): draft files are written under {@code _draft/{ridHash}} and
 * moved to the live path only on publish. V2 also supports LOST packets, whose UIN is
 * stamped after ABIS deduplication.</li>
 * </ul>
 * Methods marked {@code NOT_SUPPORTED} open short {@link TransactionTemplate} transactions
 * only around database work, so no database connection is held during object-store calls.
 * <p>
 * Identity add and update queue credential issuance in {@link PendingCredentialSync}; both
 * publish methods run it through {@link CredentialStatusManager#runPendingAfterIdentityCommit()}
 * once the identity transaction has committed, and drop it when the publish fails.
 */
@Service
@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
public class IdRepoDraftServiceImpl extends IdRepoServiceImpl
		implements IdRepoDraftService<IdRequestDTO, IdResponseDTO> {

	/** Logger for draft operations. */
	private static final Logger idrepoDraftLogger = IdRepoLogger.getLogger(IdRepoDraftServiceImpl.class);

	/** Separator of the excluded-attribute list property. */
	private static final String COMMA = ",";

	/**
	 * Identity attributes left out of {@link #getDraftUin} attribute lists when
	 * {@code EXCLUDED_ATTRIBUTE_LIST} is not configured.
	 */
	private static final String DEFAULT_ATTRIBUTE_LIST = "UIN,verifiedAttributes,IDSchemaVersion";

	/** JSON path of the UIN inside the identity object, for example {@code identity.UIN}. */
	@Value("${" + MOSIP_KERNEL_IDREPO_JSON_PATH + "}")
	private String uinPath;

	/** Key reference id used to encrypt and decrypt the UIN with its salt. */
	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	/**
	 * When {@code true}, a V1 draft is created even if the registration id was already
	 * processed, and its UIN is taken from the identity of that registration id.
	 */
	@Value("${mosip.idrepo.create-identity.enable-force-merge:false}")
	private boolean isForceMergeEnabled;

	/** Repository of {@code uin_draft}. */
	@Autowired
	private UinDraftRepo uinDraftRepo;

	/** Repository of {@code uin_biometric_draft}; used to delete a V2 draft's biometric rows. */
	@Autowired
	private UinBiometricDraftRepo uinBiometricDraftRepo;

	/** Repository of {@code uin_document_draft}; used to delete a V2 draft's document rows. */
	@Autowired
	private UinDocumentDraftRepo uinDocumentDraftRepo;

	/** Validates the identity rebuilt from a draft before it is published. */
	@Autowired
	private IdRequestValidator validator;

	/** Repository of live {@code uin_biometric} rows, written when a draft is published. */
	@Autowired
	private UinBiometricRepo uinBiometricRepo;

	/** Repository of live {@code uin_document} rows, written when a draft is published. */
	@Autowired
	private UinDocumentRepo uinDocumentRepo;

	/**
	 * Reads an identity by registration id and builds combined CBEFFs with extracted
	 * templates.
	 */
	@Autowired
	private IdRepoProxyServiceImpl proxyService;

	/** Creates and activates the draft VID issued when a draft publishes a new identity. */
	@Autowired
	private VidDraftHelper vidDraftHelper;

	/** Allocates new UINs outside any transaction. */
	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	/** Source of the excluded-attribute list of {@link #getDraftUin}. */
	@Autowired
	private Environment environment;

	/** Programmatic transactions on the primary ({@code mosip_idrepo}) transaction manager. */
	@Autowired
	private TransactionTemplate transactionTemplate;

	/**
	 * Runs credential issuance queued during an identity transaction once that transaction
	 * has committed. Lazy to break a bean cycle.
	 */
	@Autowired
	@Lazy
	private CredentialStatusManager credentialStatusManager;

	/**
	 * Creates a V1 draft. With {@code uin} (or force merge, which reads the UIN of the
	 * registration id), the draft is copied from that live identity; without, a new UIN is
	 * allocated and a minimal identity object is stored. Draft files use the UIN hash path.
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN, or {@code null} for a new identity
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code RECORD_EXISTS} when the registration id already has a
	 *         draft or was already processed (unless force merge is enabled);
	 *         {@code NO_RECORD_FOUND} when {@code uin} does not exist;
	 *         {@code UIN_GENERATION_FAILED} when a UIN cannot be allocated;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
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

	/**
	 * Merges packet identity data and documents into a V1 draft. When the draft has no
	 * identity data yet the request identity is stored as is; otherwise it is merged by
	 * {@link #updateDemographicData}. Files are written under the UIN hash path.
	 *
	 * @param registrationId registration id of the draft
	 * @param request        identity update payload
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code UNKNOWN_ERROR} when the identity JSON cannot be merged;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
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

	/**
	 * Publishes a V1 draft as the live identity. The publish runs in its own transaction
	 * ({@link #publishDraftInTransaction}); queued credential issuance runs after it commits.
	 *
	 * @param regId registration id of the draft
	 * @return response with the live identity status and, for a new identity, the draft VID
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         a validation error when the draft identity is invalid;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO publishDraft(String regId) throws IdRepoAppException {
		try {
			return transactionTemplate.execute(status -> {
				try {
					return publishDraftInTransaction(regId);
				} catch (IdRepoAppException e) {
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			});
		} catch (IdRepoAppUncheckedException e) {
			PendingCredentialSync.clear();
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (RuntimeException e) {
			PendingCredentialSync.clear();
			throw e;
		} finally {
			credentialStatusManager.runPendingAfterIdentityCommit();
		}
	}

	/**
	 * Body of the V1 publish; must run inside the transaction opened by {@link #publishDraft}.
	 * Updates the identity when it already exists, or adds it and activates a draft VID when
	 * it is new; then saves the anonymous profile, publishes the file rows and deletes the
	 * draft row.
	 *
	 * @param regId registration id of the draft
	 * @return response with the live identity status and, for a new identity, the draft VID
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	private IdResponseDTO publishDraftInTransaction(String regId) throws IdRepoAppException {
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

			// Delete through the repository: discardDraft() would add a second transactional
			// call and a redundant SELECT before the DELETE.
			uinDraftRepo.deleteByRegId(regId);

			return constructIdResponse(null, uinObject.getStatusCode(), null, draftVid);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Discards a V1 draft by deleting its draft row (biometric and document draft rows are
	 * removed by cascade). Files are not deleted.
	 *
	 * @param regId registration id of the draft
	 * @return response with status {@code DISCARDED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
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

	/**
	 * Discards a V2 draft: deletes its {@code _draft/{ridHash}} files, then its draft rows
	 * (see {@link #cleanupDraft}). Runs with {@code NOT_SUPPORTED}.
	 *
	 * @param regId registration id of the draft
	 * @return response with status {@code DISCARDED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         an object-store error when the files cannot be deleted;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO discardDraftV2(String regId) throws IdRepoAppException {
		try {
			if (!uinDraftRepo.existsByRegId(regId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						DISCARD_DRAFT, "RID NOT FOUND IN DB | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			cleanupDraft(regId);
			return constructIdResponse(null, "DISCARDED", null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					DISCARD_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Reports whether a draft exists for a registration id (V1 or V2).
	 *
	 * @param regId registration id
	 * @return {@code true} when a draft row exists
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on lookup failure
	 */
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

	/**
	 * Reads a V1 draft with all its identity data, biometrics and documents. Files are read
	 * from the UIN hash path; biometrics are returned as combined CBEFFs with templates in
	 * the requested formats. Runs in the class transaction because the draft's identity
	 * data and file rows are loaded lazily.
	 *
	 * @param regId             registration id of the draft
	 * @param extractionFormats modality key to SDK format; may be empty
	 * @return draft identity data and Base64URL-encoded files
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         a file read or extraction error; {@code DATABASE_ACCESS_ERROR} on lookup failure
	 */
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
	 * Reads a V2 draft from the {@code _draft/{ridHash}} path. {@code type} selects what is
	 * returned: {@code demographics}, {@code biometrics}, {@code supportingdocuments} or
	 * {@code all} (the default). Biometrics are returned as combined CBEFFs with templates in
	 * the requested formats.
	 *
	 * @param regId             registration id of the draft
	 * @param extractionFormats modality key to SDK format; may be empty
	 * @param type              content to return; {@code null} or blank means {@code all}
	 * @return draft identity data (when requested) and Base64URL-encoded files
	 * @throws IdRepoAppException {@code INVALID_INPUT_PARAMETER} for an unknown {@code type};
	 *         {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         a file read or extraction error; {@code DATABASE_ACCESS_ERROR} on lookup failure
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
	 * Re-extracts biometric templates for a V1 draft under the UIN hash path. Does nothing
	 * when {@code extractionFormats} is empty. Runs with {@code NOT_SUPPORTED}: the draft
	 * lookup commits in its own read transaction before extraction starts, and nothing is
	 * written to the database.
	 *
	 * @param registrationId    registration id of the draft
	 * @param extractionFormats modality key to SDK format
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code BIO_EXTRACTION_ERROR} when extraction fails;
	 *         {@code DATABASE_ACCESS_ERROR} on lookup failure
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

	/**
	 * Looks up the draft held for a UIN and lists the identity attributes it contains,
	 * leaving out the configured excluded attributes.
	 *
	 * @param uin UIN of the identity
	 * @return response with the draft's registration id, creation time and attributes;
	 *         no drafts when none exists
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on lookup failure;
	 *         {@code UNKNOWN_ERROR} when the draft identity JSON cannot be parsed
	 */
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
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					GET_DRAFT, "Failed to parse UIN data | error=" + e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
		return draftResponseDto;
	}

	/**
	 * Creates a V2 draft.
	 * <ul>
	 *   <li>{@code generateUin=true}, or an existing {@code uin}: NEW, UPDATE or reprocess
	 *       flow handled by {@link #createDraftWithUin}.</li>
	 *   <li>{@code generateUin=false} and no {@code uin}: LOST packet; a bare draft is created
	 *       by {@link #createLostDraft} and the UIN is stamped later by
	 *       {@link #updateDraftUinData}.</li>
	 * </ul>
	 * Runs with {@code NOT_SUPPORTED} so no transaction is held while live files are copied.
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN, or {@code null}
	 * @param generateUin    whether a UIN may be allocated when there is no live identity
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException the errors of {@link #createLostDraft} or
	 *         {@link #createDraftWithUin}
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO createDraftV2(String registrationId, String uin, boolean generateUin)
			throws IdRepoAppException {
		if (!generateUin && uin == null) {
			// LOST packet with no known UIN: bare draft, UIN stamped later via updateDraftUinData
			return createLostDraft(registrationId);
		}
		// NEW (generateUin=true, uin=null): allocate new UIN
		// UPDATE (generateUin=false, uin=existing): use existing UIN
		return createDraftWithUin(registrationId, uin);
	}

	/**
	 * Creates a draft with no UIN, identity data or files for a LOST packet.
	 *
	 * @param registrationId registration id of the packet
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code RECORD_EXISTS} when a draft already exists;
	 *         {@code RID_OLDER_THAN_LATEST_PROCESSED} when the registration id is in the UIN
	 *         history but is no longer the latest; {@code DATABASE_ACCESS_ERROR} on failure
	 */
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

	/**
	 * Creates a draft for a NEW, UPDATE or reprocessed packet. The live identity, when
	 * there is one, is read in a short transaction; its files are then copied to the draft
	 * path outside the transaction. With no live identity a new UIN is allocated.
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN, or {@code null} to reprocess or allocate a new UIN
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException the errors of {@link #getDraftForExistingUin};
	 *         an object-store error when a live file cannot be copied;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure;
	 *         {@code UNKNOWN_ERROR} for any other runtime failure
	 */
	private IdResponseDTO createDraftWithUin(String registrationId, String uin) throws IdRepoAppException {
		try {
			UinDraft newDraft = transactionTemplate.execute(status -> {
				try {
					return getDraftForExistingUin(registrationId, uin);
				} catch (IdRepoAppException e) {
					// TransactionCallback cannot throw checked exceptions.
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			});

			if (newDraft != null) {
				// Live identity was already copied onto this draft (uinHash, bio/doc file ids).
				// Object-store copy uses those draft fields; the live Uin entity is not needed.
				copyLiveFilesToDraft(registrationId, newDraft);
			} else {
				newDraft = buildNewIdentityDraft(registrationId);
			}

			uinDraftRepo.save(newDraft);

			return constructIdResponse(null, DRAFTED, null, null);

		} catch (DataIntegrityViolationException e) {
			// A concurrent request created a draft for the same regId or UIN.
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(RECORD_EXISTS, e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (RuntimeException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Builds a draft from an identity that already exists in the live UIN table: by
	 * {@code uin} for an UPDATE packet, or by registration id for a reprocessed packet that
	 * is still the latest applied to the identity. Returns {@code null} for a first-time NEW
	 * registration. Must run inside a transaction because the identity's biometric and
	 * document collections are lazy.
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN, or {@code null}
	 * @return unsaved draft copied from the live identity, or {@code null} when there is none
	 * @throws IdRepoAppException {@code RECORD_EXISTS} when a draft already exists for the
	 *         registration id or for the UIN;
	 *         {@code RID_OLDER_THAN_LATEST_PROCESSED} when a newer registration has been
	 *         applied; {@code NO_RECORD_FOUND} when the identity cannot be found
	 */
	private UinDraft getDraftForExistingUin(String registrationId, String uin) throws IdRepoAppException {
		// Only an existing draft for this regId blocks. UIN history is not checked here: a
		// committed packet may be reprocessed with its existing UIN instead of a new one.
		if (uinDraftRepo.existsByRegId(registrationId)) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					CREATE_DRAFT, "RID ALREADY EXIST | regId=" + registrationId);
			throw new IdRepoAppException(RECORD_EXISTS);
		}

		Uin liveUin = null;
		if (Objects.isNull(uin) && super.uinHistoryRepo.existsByRegId(registrationId)) {
			// If RID is there in UIN history then the RID is already processed.
			// But it may not be latest RID. To check the same we need to check in the UIN table as well.
			// If it is not the latest, reprocessing is not allowed.
			if (!super.uinRepo.existsByRegId(registrationId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "REQUESTED RID IS OLDER THAN THE LATEST PROCESSED RID | regId=" + registrationId);
				throw new IdRepoAppException(RID_OLDER_THAN_LATEST_PROCESSED);
			}

			// Decrypt the UIN so the save interceptor can re-encrypt it via getUinToEncrypt.
			liveUin = loadLiveUinByRegId(registrationId);
			if (liveUin == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "UIN NOT FOUND FOR REPROCESS | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			uin = decryptUin(liveUin.getUin(), liveUin.getUinHash());
		} else if (Objects.nonNull(uin)) {
			String uinHash = super.getUinHash(uin);
			liveUin = loadLiveUinByHash(uinHash);
			if (liveUin == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "UIN NOT EXIST | uin=<redacted>");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			// uin_draft.uin_hash is unique: only one open draft per UIN.
			if (uinDraftRepo.existsByUinHash(uinHash)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						CREATE_DRAFT, "UIN ALREADY HAS A DRAFT | regId=" + registrationId);
				throw new IdRepoAppException(RECORD_EXISTS);
			}
		}

		if (liveUin == null) {
			// First-time NEW packet: no live identity exists yet.
			return null;
		}

		UinDraft newDraft = mapper.convertValue(liveUin, UinDraft.class);
		updateBiometricAndDocumentDrafts(registrationId, newDraft, liveUin);
		newDraft.setUin(super.getUinToEncrypt(uin));
		newDraft.setRegId(registrationId);
		newDraft.setStatusCode("DRAFT");
		newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
		newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
		return newDraft;
	}

	/**
	 * Builds a draft for a NEW packet: allocates a UIN and stores a minimal identity
	 * object containing it. The UIN is encrypted by the entity interceptor on insert.
	 *
	 * @param registrationId registration id of the packet
	 * @return unsaved draft
	 * @throws IdRepoAppException when a UIN cannot be allocated or the identity object
	 *         cannot be serialized
	 */
	private UinDraft buildNewIdentityDraft(String registrationId) throws IdRepoAppException {
		UinDraft newDraft = new UinDraft();
		String uin = idRepoServiceHelper.generateUin();
		newDraft.setUin(super.getUinToEncrypt(uin));
		newDraft.setUinHash(super.getUinHash(uin));
		byte[] uinData = convertToBytes(generateIdentityObject(uin));
		newDraft.setUinData(uinData);
		newDraft.setUinDataHash(securityManager.hash(uinData));
		newDraft.setRegId(registrationId);
		newDraft.setStatusCode("DRAFT");
		newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
		newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
		return newDraft;
	}

	/**
	 * Loads the live identity of a registration id with its biometric and document
	 * collections initialized. Must run inside a transaction.
	 *
	 * @param registrationId registration id of the latest packet applied to the identity
	 * @return the identity, or {@code null} when there is none
	 */
	private Uin loadLiveUinByRegId(String registrationId) {
		Optional<Uin> loaded = super.uinRepo.findWithBiometricsByRegId(registrationId);
		if (loaded.isEmpty()) {
			return null;
		}
		initializeUinCollections(loaded.get());
		return loaded.get();
	}

	/**
	 * Loads a live identity by UIN hash with its biometric and document collections
	 * initialized. Must run inside a transaction.
	 *
	 * @param uinHash salted UIN hash
	 * @return the identity, or {@code null} when there is none
	 */
	private Uin loadLiveUinByHash(String uinHash) {
		Optional<Uin> loaded = super.uinRepo.findWithBiometricsByUinHash(uinHash);
		if (loaded.isEmpty()) {
			return null;
		}
		initializeUinCollections(loaded.get());
		return loaded.get();
	}

	/**
	 * Initializes the lazy biometric and document collections of an identity so they can
	 * be used after the transaction ends.
	 *
	 * @param uinObject identity loaded in the current transaction
	 */
	private void initializeUinCollections(Uin uinObject) {
		Hibernate.initialize(uinObject.getBiometrics());
		Hibernate.initialize(uinObject.getDocuments());
	}

	/**
	 * Copies every live biometric and document file listed on the draft from
	 * {@code {uinHash}/} to {@code _draft/{ridHash}/}, one file at a time, so the V2 update
	 * and read paths find them. The live files are kept. Runs outside a transaction.
	 *
	 * @param registrationId registration id of the draft
	 * @param newDraft       draft built from the live identity
	 * @throws IdRepoAppException when a file cannot be copied
	 */
	private void copyLiveFilesToDraft(String registrationId, UinDraft newDraft)
			throws IdRepoAppException {
		List<UinBiometricDraft> bios = newDraft.getBiometrics();
		List<UinDocumentDraft> docs = newDraft.getDocuments();
		boolean hasBios = bios != null && !bios.isEmpty();
		boolean hasDocs = docs != null && !docs.isEmpty();
		if (!hasBios && !hasDocs) {
			return;
		}

		String ridHash = objectStoreHelper.getRidHash(registrationId);
		String livePrefix = newDraft.getUinHash().split(SPLITTER)[1];

		if (hasBios) {
			for (UinBiometricDraft bio : bios) {
				objectStoreHelper.copyBiometricLiveToDraft(livePrefix, ridHash, bio.getBioFileId());
			}
		}
		if (hasDocs) {
			for (UinDocumentDraft doc : docs) {
				objectStoreHelper.copyDemographicLiveToDraft(livePrefix, ridHash, doc.getDocId());
			}
		}
	}

	/**
	 * Stamps the ABIS-matched UIN on a LOST draft and completes its demographic data for
	 * publish.
	 * <p>
	 * The UIN is encrypted here because the save interceptor encrypts it only on insert.
	 * Missing demographic fields are then filled from the matched live identity by
	 * {@link #backfillDraftIdentityFromLiveUin}; demographics already set by the LOST packet
	 * win on conflict. Biometrics, documents and object-store files are not touched; they
	 * are handled by {@link #updateDraftV2} and {@link #publishDraftV2}. Attribute
	 * update-count limits are enforced at publish, not here. The draft is flushed so a
	 * unique-key clash surfaces inside this method.
	 *
	 * @param registrationId registration id of the LOST draft
	 * @param uin            UIN matched by deduplication
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code UIN_NOT_FOUND} when the UIN has no live identity;
	 *         {@code DRAFT_UIN_HASH_MISMATCH} when the draft is already stamped with another
	 *         UIN; {@code RECORD_EXISTS} when another draft already holds this UIN;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure;
	 *         {@code UNKNOWN_ERROR} when the identity JSON cannot be merged
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
			String uinHash = super.getUinHash(uin);
			Optional<Uin> liveUinOpt = super.uinRepo.findByUinHash(uinHash);
			if (liveUinOpt.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "UIN NOT FOUND | regId=" + registrationId);
				throw new IdRepoAppException(UIN_NOT_FOUND);
			}
			Uin liveUin = liveUinOpt.get();

			// Only a set uinHash means the draft is already stamped: updateDraftV2 can fill
			// uinData of a LOST draft before any UIN is known.
			if (draft.getUinHash() != null && !draft.getUinHash().equals(uinHash)) {
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
			draft.setUinHash(uinHash);
			// Build a complete draft identity before publish: backfill missing fields from live,
			// without overwriting demographics already set by the LOST packet update.
			backfillDraftIdentityFromLiveUin(draft, liveUin);
			draft.setUpdatedBy(IdRepoSecurityManager.getUser());
			draft.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
			// flush() so the UPDATE runs inside this try-catch instead of at commit.
			uinDraftRepo.flush();
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataIntegrityViolationException e) {
			// Another draft already holds this UIN hash (unique constraint on flush).
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
	 * Fills the draft's identity JSON from the live identity (demographics only). Both are
	 * merged leniently: fields present in the draft keep the draft value, fields only in the
	 * live identity are added. The identity data hash is recomputed. Does nothing when the
	 * live identity has no data.
	 *
	 * @param draft   LOST draft being stamped with a UIN
	 * @param liveUin live identity of that UIN
	 * @throws JSONException      when the JSON cannot be compared
	 * @throws IOException        when the merged JSON cannot be serialized
	 * @throws IdRepoAppException when the merge fails
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

	/**
	 * Merges packet identity data and documents into a V2 draft. When the draft has no
	 * identity data yet (a LOST draft) the request identity is stored as is; otherwise it is
	 * merged by {@link #updateDemographicData}. Files are written to the draft path by
	 * {@link #updateDocumentsV2}.
	 *
	 * @param registrationId registration id of the draft
	 * @param request        identity update payload
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_UIN_DETAILS_NOT_FOUND} when identity data changes on a LOST draft
	 *         not yet stamped; {@code UNKNOWN_ERROR} when the identity JSON cannot be merged;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
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

	/**
	 * Publishes a V2 draft as the live identity. Steps, in order:
	 * <ol>
	 * <li>Load and validate the draft in a short transaction ({@link #loadDraftForPublish}).</li>
	 * <li>Move {@code _draft/{ridHash}} files to the live {@code {uinHash}} path, so a
	 * retry finds them in place.</li>
	 * <li>In a second transaction, update the identity when it already exists, or add it
	 * and activate a draft VID when it is new; then publish the file rows.</li>
	 * <li>Delete the draft rows and save the anonymous profile.</li>
	 * </ol>
	 * Runs with {@code NOT_SUPPORTED}. Queued credential issuance is dropped on failure and
	 * run once the identity transaction has committed.
	 *
	 * @param regId registration id of the draft
	 * @return response with the live identity status and, for a new identity, the draft VID
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_UIN_DETAILS_NOT_FOUND} when the draft has no UIN or identity data;
	 *         a validation error when the draft identity is invalid;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO publishDraftV2(String regId) throws IdRepoAppException {
		anonymousProfileHelper.setRegId(regId);
		try {
			UinDraft draft = transactionTemplate.execute(status -> {
				try {
					return loadDraftForPublish(regId);
				} catch (IdRepoAppException e) {
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			});

			String lastBioFileId = (Objects.nonNull(draft.getBiometrics()) && !draft.getBiometrics().isEmpty())
					? draft.getBiometrics().get(draft.getBiometrics().size() - 1).getBioFileId()
					: null;

			anonymousProfileHelper.setNewCbeff(
					draft.getUinHash().split(SPLITTER)[1],
					!anonymousProfileHelper.isNewCbeffPresent() ? lastBioFileId : null);

			IdRequestDTO idRequest = buildRequest(regId, draft);
			validateRequest(idRequest.getRequest());
			String uin = decryptUin(draft.getUin(), draft.getUinHash());

			// Move objects to the live path first. If the later UIN commit fails, retry
			// finds the files already in place. Reversing this order would commit a live
			// identity with no objects if S3 then fails and nobody retries.
			String srcPrefix = objectStoreHelper.getRidHash(draft.getRegId());
			String destPrefix = draft.getUinHash().split(SPLITTER)[1];
			objectStoreHelper.moveAllDraftBiometricsToLive(srcPrefix, destPrefix);
			objectStoreHelper.moveAllDraftDemographicsToLive(srcPrefix, destPrefix);

			boolean liveIdentityExists = uinRepo.existsByUinHash(draft.getUinHash());
			String draftVid = liveIdentityExists ? null : vidDraftHelper.generateDraftVid(uin);
			Uin liveUin = transactionTemplate.execute(status -> {
				try {
					Uin committed;
					if (liveIdentityExists) {
						committed = updateIdentity(idRequest, uin);
					} else {
						committed = addIdentity(idRequest, uin);
						vidDraftHelper.activateDraftVid(draftVid);
					}
					publishDocuments(draft, committed);
					return committed;
				} catch (IdRepoAppException e) {
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			});

			deleteDraftDbRecords(regId);

			anonymousProfileHelper.buildAndsaveProfile(true);
			return constructIdResponse(null, liveUin.getStatusCode(), null, draftVid);

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			PendingCredentialSync.clear();
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			PendingCredentialSync.clear();
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (IdRepoAppException | RuntimeException e) {
			PendingCredentialSync.clear();
			throw e;
		} finally {
			credentialStatusManager.runPendingAfterIdentityCommit();
		}
	}

	/**
	 * Loads a draft for publishing with its biometric and document collections initialized.
	 * Must run inside a transaction. Does not create or update the live UIN row.
	 *
	 * @param regId registration id of the draft
	 * @return the draft
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_UIN_DETAILS_NOT_FOUND} when the UIN, UIN hash or identity data
	 *         is missing (a LOST draft that was never stamped)
	 */
	private UinDraft loadDraftForPublish(String regId) throws IdRepoAppException {
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
		// Load lazy collections while the session is open so they can be used after the read TX.
		Hibernate.initialize(draft.getBiometrics());
		Hibernate.initialize(draft.getDocuments());
		draft.getUinData();
		return draft;
	}

	/**
	 * Re-extracts biometric templates for a V2 draft and stores them on the draft path.
	 * Does nothing when {@code extractionFormats} is empty. Runs with {@code NOT_SUPPORTED}
	 * so no transaction is held while the SDK runs.
	 *
	 * @param registrationId    registration id of the draft
	 * @param extractionFormats modality key to SDK format
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code BIO_EXTRACTION_ERROR} when extraction fails;
	 *         {@code DATABASE_ACCESS_ERROR} on lookup failure
	 */
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
	 * Validates and normalizes the {@code type} query parameter of {@link #getDraftV2}.
	 *
	 * @param type requested content type; may be {@code null} or blank
	 * @return lower-case {@code demographics}, {@code biometrics}, {@code supportingdocuments}
	 *         or {@code all}; {@code all} when {@code type} is {@code null} or blank
	 * @throws IdRepoAppException {@code INVALID_INPUT_PARAMETER} for any other value
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
	 * Deletes a V2 draft's {@code _draft/{ridHash}} files, then its draft rows. Files go
	 * first so a failed object delete leaves the draft row for a retry. The caller must not
	 * hold a transaction, since object-store calls run here.
	 *
	 * @param regId registration id of the draft
	 * @throws IdRepoAppException when the files cannot be deleted
	 */
	public void cleanupDraft(String regId) throws IdRepoAppException {
		String ridHash = objectStoreHelper.getRidHash(regId);
		objectStoreHelper.deleteAllDraftBiometrics(ridHash);
		objectStoreHelper.deleteAllDraftDemographics(ridHash);
		deleteDraftDbRecords(regId);
	}

	/**
	 * Deletes the biometric draft, document draft and draft rows of a registration in one
	 * transaction, children first. Joins the caller's transaction when one is open.
	 *
	 * @param regId registration id of the draft
	 */
	private void deleteDraftDbRecords(String regId) {
		// TransactionTemplate because @Transactional on a private method is ignored.
		transactionTemplate.executeWithoutResult(status -> {
			uinBiometricDraftRepo.deleteByRegId(regId);
			uinDocumentDraftRepo.deleteByRegId(regId);
			uinDraftRepo.deleteByRegId(regId);
		});
	}

	/**
	 * Wraps {@code uin} in nested maps following the {@code identity.UIN} path, giving
	 * {@code {"identity": {"UIN": uin}}}. The root segment is skipped.
	 *
	 * @param uin value to place at the UIN path
	 * @return nested map holding {@code uin}
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

	/**
	 * Merges the request identity into the draft's identity JSON (V1 and V2). Verified
	 * attributes are merged first; when the JSON differs, fields are updated with the
	 * live-identity merge rules. Does nothing when the request has no identity.
	 *
	 * @param request       update request
	 * @param draftToUpdate draft whose identity data and hash are replaced
	 * @throws JSONException      when the JSON cannot be compared
	 * @throws IdRepoAppException {@code DRAFT_UIN_DETAILS_NOT_FOUND} when the data differs
	 *         but the draft has no UIN hash yet (a V2 LOST draft not yet stamped)
	 * @throws IOException        when the merged JSON cannot be serialized
	 */
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
			// LOST packet: uinData can exist before the UIN is stamped by updateDraftUinData,
			// and updateJsonObject needs the UIN hash.
			if (draftToUpdate.getUinHash() == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						UPDATE_DRAFT, "UIN details not found in draft | regId=" + draftToUpdate.getRegId()
								+ " | uinHashNull=true | hasUinData=" + (draftToUpdate.getUinData() != null));
				throw new IdRepoAppException(DRAFT_UIN_DETAILS_NOT_FOUND);
			}
			super.updateJsonObject(draftToUpdate.getUinHash(), inputData, dbData, diff, false);
		}

		draftToUpdate.setUinData(convertToBytes(
				mapper.readValue(dbData.jsonString().getBytes(), new TypeReference<Map<String, Object>>() {})));
		draftToUpdate.setUinDataHash(securityManager.hash(draftToUpdate.getUinData()));
		draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
		draftToUpdate.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
	}

	/**
	 * V1: writes the request's biometric and document files under the draft's UIN hash path
	 * and updates the draft's file rows. Does nothing when the request has no documents.
	 *
	 * @param requestDTO    update request
	 * @param draftToUpdate draft whose file rows are updated
	 * @throws IdRepoAppException when a file cannot be stored or the CBEFF cannot be merged
	 */
	private void updateDocuments(RequestDTO requestDTO, UinDraft draftToUpdate) throws IdRepoAppException {
		if (Objects.isNull(requestDTO.getDocuments()) || requestDTO.getDocuments().isEmpty()) {
			return;
		}
		Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
		String uinHashWithSalt = draftToUpdate.getUinHash().split(SPLITTER)[1];
		// v1 stores at {uinHash}/Biometrics|Demographics/.
		super.updateDocuments(uinHashWithSalt, uinObject, requestDTO, true, false);
		updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
	}

	/**
	 * V2: writes the request's biometric and document files under {@code _draft/{ridHash}/}
	 * and updates the draft's file rows. Does nothing when the request has no documents.
	 *
	 * @param requestDTO    update request
	 * @param draftToUpdate draft whose file rows are updated
	 * @throws IdRepoAppException when a file cannot be stored or the CBEFF cannot be merged
	 */
	private void updateDocumentsV2(RequestDTO requestDTO, UinDraft draftToUpdate) throws IdRepoAppException {
		if (Objects.isNull(requestDTO.getDocuments()) || requestDTO.getDocuments().isEmpty()) {
			return;
		}

		Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
		String ridHash = objectStoreHelper.getRidHash(draftToUpdate.getRegId());
		uinObject.setUinHash("_" + ridHash);
		// v2 stores at _draft/{ridHash}/Biometrics|Demographics/.
		super.updateDocuments(ridHash, uinObject, requestDTO, true, true);
		updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
	}

	/**
	 * Copies the file rows of {@code uinObject} onto the draft. A draft row of the same
	 * biometric file type or document category is updated in place when its file id
	 * changed; entity rows with no draft counterpart are added. Every draft row is then
	 * tagged with {@code regId}. The entity's own collections are not modified.
	 *
	 * @param regId         registration id of the draft
	 * @param draftToUpdate draft whose biometric and document rows are updated
	 * @param uinObject     identity (or draft mapped to {@link Uin}) holding the new file rows
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
	 * Rebuilds an identity request from a draft so it can go through the normal add or
	 * update identity path. Verified attributes are moved from the identity JSON to their
	 * own request field: claim names to {@code verifiedAttributes}, v2 verification objects
	 * to {@code verifiedAttributesMetadata}.
	 *
	 * @param regId registration id set on the request
	 * @param draft draft whose identity data is used
	 * @return identity request without documents
	 * @throws IdRepoAppUncheckedException {@code UNKNOWN_ERROR} when the identity JSON
	 *         cannot be parsed
	 */
	private IdRequestDTO buildRequest(String regId, UinDraft draft) {
		IdRequestDTO idRequest = new IdRequestDTO();
		RequestDTO request = new RequestDTO();
		request.setRegistrationId(regId);

		try {
			Map<String, Object> identityData = mapper.readValue(draft.getUinData(),
					new TypeReference<Map<String, Object>>() {});
			Object verifiedAttributes = identityData.remove(VERIFIED_ATTRIBUTES);
			if (verifiedAttributes instanceof List<?> list && list.stream().anyMatch(Map.class::isInstance)) {
				request.setVerifiedAttributesMetadata(
						mapper.convertValue(list, new TypeReference<List<VerificationMetadata>>() {}));
			} else {
				request.setVerifiedAttributes(
						mapper.convertValue(verifiedAttributes, new TypeReference<List<String>>() {}));
			}
			request.setIdentity(identityData);
		} catch (IOException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
					PUBLISH_DRAFT, "Failed to parse draft UIN data | error=" + e.getMessage());
			throw new IdRepoAppUncheckedException(UNKNOWN_ERROR, e);
		}

		idRequest.setRequest(request);
		return idRequest;
	}

	/**
	 * Validates a rebuilt draft identity with the same rules as an identity create request.
	 *
	 * @param request identity request rebuilt by {@link #buildRequest}
	 * @throws IdRepoDataValidationException when the identity fails validation
	 */
	private void validateRequest(RequestDTO request) throws IdRepoDataValidationException {
		Errors errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
		validator.validateRequest(request, errors, "create");
		DataValidationUtil.validate(errors);
	}

	/**
	 * Saves the draft's biometric and document rows as live {@code uin_biometric} and
	 * {@code uin_document} rows of the published identity. Rows that already exist (a
	 * retried publish) are logged and skipped.
	 *
	 * @param draft     draft whose file rows are published
	 * @param uinObject live identity the rows are attached to
	 */
	private void publishDocuments(UinDraft draft, Uin uinObject) {
		List<UinBiometric> uinBiometricList = draft.getBiometrics() == null ? Collections.emptyList()
				: draft.getBiometrics().stream().map(bio -> {
					UinBiometric uinBio = mapper.convertValue(bio, UinBiometric.class);
					uinBio.setUinRefId(uinObject.getUinRefId());
					uinBio.setLangCode("");
					return uinBio;
				}).collect(Collectors.toList());
		if (!uinBiometricList.isEmpty()) {
			try {
				uinBiometricRepo.saveAll(uinBiometricList);
			} catch (DataIntegrityViolationException e) {
				// Retry after a committed write may hit uk_uinb. Treat as already published.
				idrepoDraftLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, "Live bio rows already exist (retry) | error=" + e.getMessage());
			}
		}

		List<UinDocument> uinDocumentList = draft.getDocuments() == null ? Collections.emptyList()
				: draft.getDocuments().stream().map(doc -> {
					UinDocument uinDoc = mapper.convertValue(doc, UinDocument.class);
					uinDoc.setUinRefId(uinObject.getUinRefId());
					uinDoc.setLangCode("");
					return uinDoc;
				}).collect(Collectors.toList());
		if (!uinDocumentList.isEmpty()) {
			try {
				uinDocumentRepo.saveAll(uinDocumentList);
			} catch (DataIntegrityViolationException e) {
				// Retry after a committed write may hit uk_uind. Treat as already published.
				idrepoDraftLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
						PUBLISH_DRAFT, "Live doc rows already exist (retry) | error=" + e.getMessage());
			}
		}
	}

	/**
	 * Decrypts a stored UIN and checks it against the stored hash.
	 *
	 * @param encryptedUin stored value {@code {saltId}_{Base64URL ciphertext}}
	 * @param uinHash      stored salted UIN hash
	 * @return the plain UIN
	 * @throws IdRepoAppException when decryption fails
	 * @throws IdRepoAppUncheckedException {@code UIN_HASH_MISMATCH} when the decrypted UIN
	 *         does not hash to {@code uinHash}
	 */
	private String decryptUin(String encryptedUin, String uinHash) throws IdRepoAppException {
		// getOne returns a lazy proxy, which fails here when no session is open (publishDraftV2).
		String salt = uinEncryptSaltRepo.retrieveSaltById(
				Integer.parseInt(encryptedUin.split(SPLITTER)[0]));
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
	 * V1: for each biometric file of the draft, deletes previously extracted files of the
	 * requested formats and extracts them again under the UIN hash path.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param draft             draft whose biometric files are processed
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on persistence failure;
	 *         service errors as raised; {@code BIO_EXTRACTION_ERROR} for any other failure
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

	/**
	 * V2: for each biometric file of the draft, deletes previously extracted files of the
	 * requested formats and extracts them again, storing the results on the draft path.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param draft             draft whose biometric files are processed
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on persistence failure;
	 *         service errors as raised; {@code BIO_EXTRACTION_ERROR} for any other failure
	 */
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

	/**
	 * Deletes the draft-path extracted files of one biometric file for every requested
	 * format. A failed delete is logged and skipped.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param ridHash           rid hash of the draft
	 * @param bioDraft          biometric draft row whose extracted files are deleted
	 */
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
	 * Deletes the live-path extracted files of one biometric file for every requested
	 * format. A failed delete is logged and skipped, so one bad file does not stop the
	 * others from being deleted.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param uinHash           uin hash (without salt id)
	 * @param bioDraft          biometric draft row whose extracted files are deleted
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

	/**
	 * V1: reads a CBEFF from the UIN hash path and returns it with the requested modalities
	 * replaced by their extracted templates.
	 *
	 * @param uinHash           uin hash (without salt id)
	 * @param bioFileId         file id of the CBEFF
	 * @param extractionFormats modality key to SDK format
	 * @return combined CBEFF XML
	 * @throws IdRepoAppException when the file cannot be read or extraction fails
	 */
	private byte[] extractAndGetCombinedCbeff(String uinHash, String bioFileId,
											  Map<String, String> extractionFormats) throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormats(uinHash, bioFileId, extractionFormats,
				super.objectStoreHelper.getBiometricObject(uinHash, bioFileId));
	}

	/**
	 * V2: reads a CBEFF from the draft path and returns it with the requested modalities
	 * replaced by their extracted templates. Extracted files are written to the same draft
	 * path.
	 *
	 * @param ridHash           rid hash of the draft
	 * @param bioFileId         file id of the draft CBEFF
	 * @param extractionFormats modality key to SDK format
	 * @return combined CBEFF XML
	 * @throws IdRepoAppException when the file cannot be read or extraction fails
	 */
	private byte[] extractAndGetCombinedCbeffDraft(String ridHash, String bioFileId,
												   Map<String, String> extractionFormats) throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormatsDraft(ridHash, bioFileId, extractionFormats,
				super.objectStoreHelper.getDraftBiometricObject(ridHash, bioFileId));
	}

	/**
	 * Builds the name of an extracted template file.
	 *
	 * @param extractionFormat modality key (for example {@code fingerExtractionFormat}) and format
	 * @param bioFileId        file id of the source CBEFF
	 * @return {@code {bioFileId without extension}.{modality}.{format}}
	 */
	private String buildExtractionFileName(Entry<String, String> extractionFormat, String bioFileId) {
		return bioFileId.split("\\.")[0]
				+ DOT
				+ getModalityForFormat(extractionFormat.getKey())
				+ DOT
				+ extractionFormat.getValue();
	}

	/**
	 * Strips the {@code ExtractionFormat} suffix from a modality key.
	 *
	 * @param formatQueryParam modality key, for example {@code fingerExtractionFormat}
	 * @return modality name, for example {@code finger}
	 */
	private String getModalityForFormat(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, "");
	}

	/**
	 * Builds a draft API response. Verified attributes are moved from the identity JSON to
	 * their own response field, so they do not appear twice.
	 *
	 * @param uinData   identity JSON to return, or {@code null}
	 * @param status    response status, for example {@code DRAFTED}
	 * @param documents files to return, or {@code null}
	 * @param vid       draft VID to return in the metadata, or {@code null}
	 * @return the response
	 * @throws IdRepoAppUncheckedException {@code UNKNOWN_ERROR} when {@code uinData} cannot
	 *         be parsed
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
			try {
				ObjectNode identityObject = mapper.readValue(uinData, ObjectNode.class);
				// Raw List: stored entries are claim names (v1) or verification objects (v2).
				response.setVerifiedAttributes(
						mapper.convertValue(identityObject.get(VERIFIED_ATTRIBUTES), List.class));
				identityObject.remove(VERIFIED_ATTRIBUTES);
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
	 * Lists the top-level attribute names of an identity JSON, without the attributes in
	 * {@code EXCLUDED_ATTRIBUTE_LIST} (default {@link #DEFAULT_ATTRIBUTE_LIST}).
	 *
	 * @param uinData identity JSON bytes (UTF-8)
	 * @return attribute names
	 * @throws JsonProcessingException when the JSON cannot be parsed
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
