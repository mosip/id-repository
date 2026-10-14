package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.*;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.mosip.idrepository.core.constant.*;
import jakarta.annotation.Resource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdVidMetadataResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.repository.HandleRepo;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.FieldComparisonFailure;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.manager.CredentialStatusManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.dto.HandleDto;
import io.mosip.idrepository.identity.entity.*;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.provider.IdentityUpdateTrackerPolicyProvider;
import io.mosip.idrepository.identity.repository.*;
import io.mosip.idrepository.pipeline.PendingCredentialSync;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.UUIDUtils;

/**
 * Persistence layer of the identity service: writes, merges and reads UIN records in
 * {@code mosip_idrepo}.
 * <p>
 * Primary {@link IdRepoService} implementation. It is called by {@link IdRepoProxyServiceImpl}
 * (validation, caching and the REST contract) and reused by the draft service, which extends it.
 * </p>
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li><b>Add</b> ({@link #addIdentity}): hashes and encrypts the UIN with the salts from
 * {@code uin_hash_salt} / {@code uin_encrypt_salt}, stores the identity JSON and its hash,
 * uploads documents and biometrics to the object store, and writes {@code uin}, {@code uin_h},
 * {@code uin_document(_h)}, {@code uin_biometric(_h)} and {@code handle} rows.</li>
 * <li><b>Update</b> ({@link #updateIdentity}): trims input values, merges the request into
 * the stored JSON ({@link #updateJsonObject}), merges CBEFF files modality by modality,
 * replaces changed handles, enforces per-attribute update limits and writes a history row.</li>
 * <li><b>Verified attributes</b>: keeps the v1 list of claim names or the v2 list of
 * {@link VerificationMetadata} objects on the identity JSON.</li>
 * <li><b>Credentials</b>: updates {@code credential_request_status} and queues synchronous
 * credential issuance to run after the identity transaction ends.</li>
 * <li><b>Anonymous profile</b>: feeds old and new identity data and CBEFF to
 * {@link AnonymousProfileHelper}.</li>
 * </ul>
 * <p>
 * Every public method runs in one transaction that rolls back on {@link IdRepoAppException}
 * and {@link IdRepoAppUncheckedException}, and logs its duration through
 * {@code IdentityServicePerformanceLog}.
 * </p>
 */
@Component
@Primary
@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
public class IdRepoServiceImpl implements IdRepoService<IdRequestDTO, Uin> {

	/**
	 * Identity JSON key holding the verified attributes: a list of claim names (v1) or a list of
	 * {@link VerificationMetadata} objects (v2).
	 */
	private static final String VERIFIED_ATTRIBUTES = "verifiedAttributes";

	/** Method name used in logs of {@code updateIdentity}. */
	private static final String UPDATE_IDENTITY = "updateIdentity";

	/** JSON path root token. */
	private static final String ROOT = "$";

	/** JSON path array-index opening token. */
	private static final String OPEN_SQUARE_BRACE = "[";

	/** Key of the language code in a multi-language identity value. */
	private static final String LANGUAGE = "language";

	/** Key of the text in a multi-language identity value. */
	private static final String VALUE = "value";

	/** Method name used in logs of {@code addIdentity}. */
	private static final String ADD_IDENTITY = "addIdentity";

	/** Method name used in logs of {@link #addIdentityHandle}. */
	private static final String ADD_IDENTITY_HANDLE = "addIdentityHandle";

	/** Separator used when joining or splitting attribute lists. */
	private static final String COMMA = ",";

	/** Logger for identity operations; also receives the per-method performance log. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoServiceImpl.class);

	/** Identity schema key of a document's type. */
	private static final String TYPE = "type";

	/** Separator in file names. */
	private static final String DOT = ".";

	/** Class name used as the logger component id. */
	private static final String ID_REPO_SERVICE_IMPL = "IdRepoServiceImpl";
	
	/** Method name used in logs of {@code retrieveIdentity}. */
	private static final String RETRIEVE_IDENTITY = "retrieveIdentity";

	/** Spring environment for property resolution. */
	@Autowired
	protected EnvUtil env;

	/** Converts between identity JSON, DTOs and entities. */
	@Autowired
	protected ObjectMapper mapper;

	/** Repository of live {@code uin} rows. */
	@Autowired
	protected UinRepo uinRepo;

	/** Repository of {@code uin_document_h}; receives a history row for every document change. */
	@Autowired
	private UinDocumentHistoryRepo uinDocHRepo;

	/** Repository of {@code uin_biometric_h}; receives a history row for every biometric change. */
	@Autowired
	private UinBiometricHistoryRepo uinBioHRepo;

	/** Repository of {@code uin_h}; receives a history row for every identity change. */
	@Autowired
	protected UinHistoryRepo uinHistoryRepo;

	/** Parses, merges and writes CBEFF biometric XML. */
	@Autowired
	protected CbeffUtil cbeffUtil;

	/** Hashes and encrypts the UIN and identity data. */
	@Autowired
	protected IdRepoSecurityManager securityManager;

	/** Identity schema attribute names that hold biometric files. */
	@Resource
	protected List<String> bioAttributes;

	/** Repository of {@code uin_hash_salt}; salts for the UIN hash. */
	@Autowired
	protected UinHashSaltRepo uinHashSaltRepo;

	/** Repository of {@code uin_encrypt_salt}; salts for UIN encryption. */
	@Autowired
	protected UinEncryptSaltRepo uinEncryptSaltRepo;

	/** Reads and writes encrypted biometric and document files. */
	@Autowired
	protected ObjectStoreHelper objectStoreHelper;

	/** Supplies the dummy online-verification partner id used for new credential requests. */
	@Autowired
	private DummyPartnerCheckUtil dummyPartner;

	/**
	 * Runs credential issuance queued during an identity transaction once that transaction
	 * has committed. Lazy to break a bean cycle.
	 */
	@Autowired
	@Lazy
	private CredentialStatusManager credentialStatusManager;

	/** Repository of {@code credential_request_status}; tracks credential issuance per identity. */
	@Autowired
	private CredentialRequestStatusRepo credRequestRepo;
	
	/** Builds and saves the anonymous profile of every identity change. */
	@Autowired
	protected AnonymousProfileHelper anonymousProfileHelper;
	
	/** Identity status that means active (normally {@code ACTIVATED}). */
	@Value("${" + ACTIVE_STATUS + "}")
	private String activeStatus;

	/**
	 * Identity fields that an update replaces as a whole instead of merging values
	 * language by language (a SpEL list from {@code UPDATE_IDENTITY_FIELDS_TO_REPLACE}).
	 */
	@Value("#{${" + UPDATE_IDENTITY_FIELDS_TO_REPLACE + "}}")
	private List<String> fieldsToReplaceOnUpdate;

	/** Repository of {@code identity_update_count_tracker}; counts updates per attribute. */
	@Autowired
	private IdentityUpdateTrackerRepo identityUpdateTracker;

	/**
	 * When {@code true}, a new credential request uses the request id derived by naming
	 * convention instead of a random id.
	 */
	@Value("${" + ENABLE_CONVENTION_BASED_ID + ":false}")
	private boolean enableConventionBasedId;

	/** Repository of {@code handle}; stores the identity handles. */
	@Autowired
	private HandleRepo handleRepo;

	/** Reads handle configuration and builds handle hashes. */
	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	/**
	 * Key reference id used to encrypt and decrypt the UIN with its salt. Not read by this class
	 * ({@link #addIdentity} uses a local variable of the same name for the row id).
	 */
	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	/** When {@code true}, leading and trailing whitespace is trimmed from update values. */
	@Value("${" + UPDATE_IDENTITY_TRIM_WHITESPACES + ":true}")
	private boolean trimWhitespaces;

	/**
	 * Creates a new identity record.
	 * <ol>
	 * <li>Derives the row id ({@code uinRefId}) as a name-based UUID of the UIN and the current
	 * time, the salted UIN hash ({@code saltId_hash}) and the value to encrypt
	 * ({@code saltId_uin_salt}).</li>
	 * <li>Resolves the selected handles, rejects any already owned by another identity, and
	 * writes their hashes back into the identity JSON.</li>
	 * <li>Stores the verified attributes: v2 metadata (only claims present in the identity are
	 * kept) or the v1 list as sent.</li>
	 * <li>Uploads the documents and builds their metadata rows, then saves the {@code uin} row
	 * (cascading the document and biometric rows) and a {@code uin_h} history row.</li>
	 * <li>Saves the handle rows, records the credential request and queues issuance, and saves
	 * the anonymous profile.</li>
	 * </ol>
	 *
	 * @param request identity create request; its identity map is replaced in place with the
	 *                handle-resolved version
	 * @param uin     plaintext UIN, already allocated
	 * @return the saved {@link Uin} entity (UIN field encrypted)
	 * @throws IdRepoAppException          on handle conflict ({@code HANDLE_RECORD_EXISTS}) or
	 *                                     identity serialisation failure
	 * @throws IdRepoAppUncheckedException when a document cannot be decoded or stored
	 */
	@Override
	public Uin addIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			String uinRefId = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, uin + SPLITTER + DateUtils2.getUTCCurrentDateTime())
					.toString();
			String uinHash = getUinHash(uin);
			String uinHashWithSalt = uinHash.split(SPLITTER)[1];
			String uinToEncrypt = getUinToEncrypt(uin);

			Map<String, Object> identityObjectMap = mapper.convertValue(request.getRequest().getIdentity(),
					new TypeReference<Map<String, Object>>() {
					});
			Map<String, List<HandleDto>> selectedUniqueHandlesMap = checkAndGetHandles(request, uinHash, null, CREATE);
			idRepoServiceHelper.updateSelectedHandleFields(identityObjectMap, selectedUniqueHandlesMap);
			request.getRequest().setIdentity(identityObjectMap);

			ObjectNode identityObject = mapper.convertValue(identityObjectMap, ObjectNode.class);
			if (request.getRequest().getVerifiedAttributesMetadata() != null) {
				DocumentContext inputData = jacksonJson(identityObjectMap);
				DocumentContext storedData = jacksonJson(new HashMap<>());
				applyV2VerifiedAttributes(request.getRequest().getVerifiedAttributesMetadata(), inputData, storedData);
				identityObject.putPOJO(VERIFIED_ATTRIBUTES, storedData.read("$." + VERIFIED_ATTRIBUTES));
			} else {
				identityObject.putPOJO(VERIFIED_ATTRIBUTES, request.getRequest().getVerifiedAttributes());
			}
			byte[] identityInfo = convertToBytes(identityObject);

			anonymousProfileHelper
				.setRegId(request.getRequest().getRegistrationId())
				.setNewUinData(identityInfo);

			List<UinDocument> docList = new ArrayList<>();
			List<UinBiometric> bioList = new ArrayList<>();
			Uin uinEntity;
			if (Objects.nonNull(request.getRequest().getDocuments()) && !request.getRequest().getDocuments().isEmpty()) {
				addDocuments(uinHashWithSalt, identityInfo, request.getRequest().getDocuments(), uinRefId, docList, bioList,
						false, false);
				uinEntity = new Uin(uinRefId, uinToEncrypt, uinHash, identityInfo, securityManager.hash(identityInfo),
						request.getRequest().getRegistrationId(), activeStatus, IdRepoSecurityManager.getUser(),
						DateUtils2.getUTCCurrentDateTime(), null, null, false, null, bioList, docList);
				uinEntity = uinRepo.save(uinEntity);
				mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
						"Record successfully saved in db with documents");
			} else {
				uinEntity = new Uin(uinRefId, uinToEncrypt, uinHash, identityInfo, securityManager.hash(identityInfo),
						request.getRequest().getRegistrationId(), activeStatus, IdRepoSecurityManager.getUser(),
						DateUtils2.getUTCCurrentDateTime(), null, null, false, null, null, null);
				uinEntity = uinRepo.save(uinEntity);
				mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
						"Record successfully saved in db without documents");
			}

			uinHistoryRepo.save(new UinHistory(uinRefId, DateUtils2.getUTCCurrentDateTime(), uinEntity.getUin(), uinEntity.getUinHash(),
							uinEntity.getUinData(), uinEntity.getUinDataHash(), uinEntity.getRegId(), activeStatus,
							IdRepoSecurityManager.getUser(), DateUtils2.getUTCCurrentDateTime(), null, null, false, null));

			addIdentityHandle(uinEntity, selectedUniqueHandlesMap);

			issueCredential(uin, uinEntity.getUin(), activeStatus, null, uinEntity.getRegId(), false);

			anonymousProfileHelper.buildAndsaveProfile(false);
			return uinEntity;
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, ID_REPO_SERVICE_IMPL, ADD_IDENTITY, startNanos);
		}
	}

	/**
	 * Builds the value stored (after entity-level encryption) in {@code uin.uin}:
	 * {@code saltId + SPLITTER + uin + SPLITTER + encryptSalt}. The salt id is the UIN modulo the
	 * salt table size, and the salt comes from {@code uin_encrypt_salt}.
	 *
	 * @param uin plaintext UIN
	 * @return the salted value to encrypt
	 */
	protected String getUinToEncrypt(String uin) {
		int modResult = securityManager.getSaltKeyForId(uin);
		String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(modResult);
		return modResult + SPLITTER + uin + SPLITTER + encryptSalt;
	}

	/**
	 * Builds the salted UIN hash used as the lookup key of an identity:
	 * {@code saltId + SPLITTER + hash(uin, hashSalt)}, with the salt id taken from the UIN by the
	 * modulo rule and the salt from {@code uin_hash_salt}.
	 *
	 * @param uin plaintext UIN
	 * @return the value stored in {@code uin.uin_hash}; the part after {@code SPLITTER} is the
	 *         bare hash used as the object-store folder
	 */
	protected String getUinHash(String uin) {
		int modResult = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		return modResult + SPLITTER + securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
	}

	/**
	 * Decodes the request documents, writes them to the object store, and records a
	 * biometric or document metadata row for each.
	 *
	 * @param uinHash             uin hash used as the object path; for V2 drafts, the rid hash
	 * @param identityInfo        identity JSON, used to find each document's category and type
	 * @param documents           Base64URL-encoded documents from the request
	 * @param uinRefId            reference id of the identity row the metadata rows belong to
	 * @param docList             receives the new document metadata rows
	 * @param bioList             receives the new biometric metadata rows
	 * @param isDraft             {@code true} when the metadata rows belong to a draft
	 * @param useDraftObjectStore {@code true} only for V2 drafts, which store files under
	 *                            {@code _draft/{ridHash}/}; {@code false} uses {@code {uinHash}/}
	 * @throws IdRepoAppUncheckedException {@code INVALID_INPUT_PARAMETER} when a document
	 *         cannot be decoded; {@code FILE_STORAGE_ACCESS_ERROR} when a write fails
	 */
	private void addDocuments(String uinHash, byte[] identityInfo, List<DocumentsDTO> documents, String uinRefId,
			List<UinDocument> docList, List<UinBiometric> bioList, boolean isDraft, boolean useDraftObjectStore) {
		ObjectNode identityObject = convertToObject(identityInfo, ObjectNode.class);
		List<PendingObjectStorePut> pendingPuts = new ArrayList<>();
		IntStream.range(0, documents.size()).filter(index -> identityObject.has(documents.get(index).getCategory())).forEach(index -> {
			DocumentsDTO doc = documents.get(index);
			JsonNode docType = identityObject.get(doc.getCategory());
			try {
				byte[] data = CryptoUtil.decodeURLSafeBase64(doc.getValue());
				if (bioAttributes.contains(doc.getCategory())) {
					try {
						cbeffUtil.validateXML(data);
					} catch (Exception e) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "addBiometricDocuments", e.getMessage());
						throw new IdRepoAppUncheckedException(INVALID_INPUT_PARAMETER.getErrorCode(),
								String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + index + "/value"), e);
					}
					pendingPuts.add(new PendingObjectStorePut(true, getFileRefId(docType), data, doc, docType, index));
				} else {
					pendingPuts.add(new PendingObjectStorePut(false, getFileRefId(docType), data, doc, docType, index));
				}
			} catch (IdRepoAppUncheckedException e) {
				throw e;
			} catch (Exception e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getMessage());
				throw new IdRepoAppUncheckedException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + index + "/value"), e);
			}
		});

		storePendingDocuments(uinHash, pendingPuts, useDraftObjectStore);

		for (PendingObjectStorePut pending : pendingPuts) {
			if (pending.bio()) {
				addBiometricMetadata(uinRefId, bioList, pending.doc(), pending.docType(), isDraft, pending.fileRefId(), pending.data());
				anonymousProfileHelper.setNewCbeff(pending.doc().getValue());
			} else {
				addDemographicMetadata(uinRefId, docList, pending.doc(), pending.docType(), isDraft, pending.fileRefId(), pending.data());
			}
		}
	}

	/**
	 * Writes all pending documents to the object store, in parallel when there is more than one.
	 *
	 * @param uinHash             the uin hash (rid hash when {@code useDraftObjectStore})
	 * @param pendingPuts         the pending puts
	 * @param useDraftObjectStore {@code true} for v2 drafts stored under {@code _draft/{ridHash}/...}
	 * @throws IdRepoAppUncheckedException the first write failure, with its original error
	 *         code, or {@code FILE_STORAGE_ACCESS_ERROR} for an unexpected failure
	 */
	private void storePendingDocuments(String uinHash, List<PendingObjectStorePut> pendingPuts,
			boolean useDraftObjectStore) {
		if (pendingPuts.isEmpty()) {
			return;
		}
		if (pendingPuts.size() == 1) {
			putPendingDocument(uinHash, pendingPuts.get(0), useDraftObjectStore);
			return;
		}
		List<CompletableFuture<Void>> puts = pendingPuts.stream()
				.map(pending -> CompletableFuture.runAsync(() -> putPendingDocument(uinHash, pending, useDraftObjectStore)))
				.toList();
		try {
			CompletableFuture.allOf(puts.toArray(CompletableFuture[]::new)).join();
		} catch (CompletionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			if (cause instanceof IdRepoAppUncheckedException unchecked) {
				throw unchecked;
			} else if (cause instanceof IdRepoAppException idRepoAppException) {
				throw new IdRepoAppUncheckedException(idRepoAppException.getErrorCode(),
						idRepoAppException.getErrorText(), idRepoAppException);
			}
			throw new IdRepoAppUncheckedException(FILE_STORAGE_ACCESS_ERROR, cause);
		}
	}

	/**
	 * Writes one pending document to the live or draft object-store path.
	 *
	 * @param uinHash             uin hash, or rid hash when {@code useDraftObjectStore}
	 * @param pending             document to write
	 * @param useDraftObjectStore {@code true} to write under {@code _draft/{ridHash}/}
	 * @throws IdRepoAppUncheckedException when the write fails
	 */
	private void putPendingDocument(String uinHash, PendingObjectStorePut pending, boolean useDraftObjectStore) {
		try {
			if (pending.bio()) {
				if (useDraftObjectStore) {
					objectStoreHelper.putDraftBiometricObject(uinHash, pending.fileRefId(), pending.data());
				} else {
					objectStoreHelper.putBiometricObject(uinHash, pending.fileRefId(), pending.data());
				}
			} else if (useDraftObjectStore) {
				objectStoreHelper.putDraftDemographicObject(uinHash, pending.fileRefId(), pending.data());
			} else {
				objectStoreHelper.putDemographicObject(uinHash, pending.fileRefId(), pending.data());
			}
		} catch (IdRepoAppException e) {
			throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Adds a {@code uin_biometric} row for a CBEFF file and, for a live identity, saves a
	 * {@code uin_biometric_h} history row. The file itself is written separately by
	 * {@link #storePendingDocuments}.
	 *
	 * @param uinRefId  id of the owning {@code uin} or {@code uin_draft} row
	 * @param bioList   biometric rows of the identity; the new row is appended
	 * @param doc       request document; its category (for example {@code individualBiometrics}) is stored
	 * @param docType   the document's node in the identity JSON; supplies the file name
	 * @param isDraft   {@code true} for a draft, which keeps no history
	 * @param fileRefId object-store file name from {@link #getFileRefId}
	 * @param data      decoded CBEFF content; its hash is stored on the row
	 */
	private void addBiometricMetadata(String uinRefId, List<UinBiometric> bioList, DocumentsDTO doc,
			JsonNode docType, boolean isDraft, String fileRefId, byte[] data) {
		bioList.add(new UinBiometric(uinRefId, fileRefId, doc.getCategory(), docType.get(FILE_NAME_ATTRIBUTE).asText(),
				securityManager.hash(data), "", IdRepoSecurityManager.getUser(),
				DateUtils2.getUTCCurrentDateTime(), null, null, false, null));

		if (!isDraft)
			uinBioHRepo.save(new UinBiometricHistory(uinRefId, DateUtils2.getUTCCurrentDateTime(), fileRefId, doc.getCategory(),
					docType.get(FILE_NAME_ATTRIBUTE).asText(), securityManager.hash(doc.getValue().getBytes()),
					"", IdRepoSecurityManager.getUser(), DateUtils2.getUTCCurrentDateTime(),
					null, null, false, null));
	}

	/**
	 * Adds a {@code uin_document} row for a demographic document (proof of address, identity,
	 * and so on) and, for a live identity, saves a {@code uin_document_h} history row. The file
	 * itself is written separately by {@link #storePendingDocuments}.
	 *
	 * @param uinRefId  id of the owning {@code uin} or {@code uin_draft} row
	 * @param docList   document rows of the identity; the new row is appended
	 * @param doc       request document; its category (for example {@code proofOfAddress}) is stored
	 * @param docType   the document's node in the identity JSON; supplies the type, file name and format
	 * @param isDraft   {@code true} for a draft, which keeps no history
	 * @param fileRefId object-store file name from {@link #getFileRefId}
	 * @param data      decoded document content; its hash is stored on the row
	 */
	private void addDemographicMetadata(String uinRefId, List<UinDocument> docList, DocumentsDTO doc,
			JsonNode docType, boolean isDraft, String fileRefId, byte[] data) {
		docList.add(new UinDocument(uinRefId, doc.getCategory(), docType.get(TYPE).asText(), fileRefId,
				docType.get(FILE_NAME_ATTRIBUTE).asText(), docType.get(FILE_FORMAT_ATTRIBUTE).asText(),
				securityManager.hash(data), "", IdRepoSecurityManager.getUser(),
				DateUtils2.getUTCCurrentDateTime(), null, null, false, null));

		if (!isDraft)
			uinDocHRepo.save(new UinDocumentHistory(uinRefId, DateUtils2.getUTCCurrentDateTime(), doc.getCategory(),
					docType.get(TYPE).asText(), fileRefId, docType.get(FILE_NAME_ATTRIBUTE).asText(),
					docType.get(FILE_FORMAT_ATTRIBUTE).asText(), securityManager.hash(data),
					"", IdRepoSecurityManager.getUser(), DateUtils2.getUTCCurrentDateTime(),
					null, null, false, null));
	}

	/**
	 * Loads the identity row by salted UIN hash. Filtering by {@code type} and biometric
	 * extraction are done by the caller ({@link IdRepoProxyServiceImpl}); this method only reads.
	 *
	 * @param uinHash           salted UIN hash, as built by {@link #getUinHash(String)}
	 * @param idType            id type the caller resolved (not used here)
	 * @param type              requested data type, for example {@code bio} or {@code all} (not used here)
	 * @param extractionFormats biometric extraction formats (not used here)
	 * @return the identity row, with its document and biometric rows
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when no row has this hash
	 */
	@Override
	public Uin retrieveIdentity(String uinHash, IdType idType, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			Optional<Uin> uinObjOptional = uinRepo.findByUinHash(uinHash);
			if (uinObjOptional.isPresent()) {
				return uinObjOptional.get();
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
						NO_RECORD_FOUND.getErrorMessage());
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, startNanos);
		}
	}

	/**
	 * Applies an update request to an existing identity.
	 * <ol>
	 * <li>Trims request values (when {@link #trimWhitespaces} is on) and loads the stored row.</li>
	 * <li>Sets the registration id and, when it changes, the status.</li>
	 * <li>When the request has an identity: replaces handles (removed ones are marked
	 * {@code DELETE}, new ones checked for uniqueness), merges verified attributes, replaces the
	 * {@link #fieldsToReplaceOnUpdate} fields, and merges the remaining differences with
	 * {@link #updateJsonObject}, which also enforces update-count limits.</li>
	 * <li>When the request has documents: merges biometrics into the stored CBEFF, uploads the
	 * files and updates the metadata rows.</li>
	 * <li>Saves the row and a {@code uin_h} history row, inserts new handles, records the
	 * credential requests ({@code NEW} when the status is active, {@code DELETED} otherwise) and
	 * saves the anonymous profile.</li>
	 * </ol>
	 *
	 * @param request identity update request; its identity map may be changed in place
	 * @param uin     plaintext UIN of the identity to update
	 * @return the saved {@link Uin} entity
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND}, {@code HANDLE_RECORD_EXISTS},
	 *                            {@code UPDATE_COUNT_LIMIT_EXCEEDED}, {@code UNKNOWN_ERROR} when the
	 *                            merge cannot reconcile the request, {@code ID_OBJECT_PROCESSING_FAILED}
	 *                            on JSON errors, or the code of any unchecked failure
	 */
	@Override
	public Uin updateIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			anonymousProfileHelper.setRegId(request.getRequest().getRegistrationId());
			String uinHash = getUinHash(uin);
			String uinHashWithSalt = uinHash.split(SPLITTER)[1];
			try {
				updateRequestBodyData(request);

				Uin uinObject = retrieveIdentity(uinHash, IdType.UIN, null, null);
				anonymousProfileHelper.setOldCbeff(uinHash,
						!anonymousProfileHelper.isOldCbeffPresent() && Objects.nonNull(uinObject.getBiometrics())
								&& !uinObject.getBiometrics().isEmpty()
										? uinObject.getBiometrics().get(uinObject.getBiometrics().size() - 1).getBioFileId()
										: null);
				uinObject.setRegId(request.getRequest().getRegistrationId());
				if (Objects.nonNull(request.getRequest().getStatus())
						&& !StringUtils.equals(uinObject.getStatusCode(), request.getRequest().getStatus())) {
					uinObject.setStatusCode(request.getRequest().getStatus());
					uinObject.setUpdatedBy(IdRepoSecurityManager.getUser());
					uinObject.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
				}
				Map<String, List<HandleDto>> handlesToAdd = null;
				if (Objects.nonNull(request.getRequest()) && Objects.nonNull(request.getRequest().getIdentity())) {
					handlesToAdd = getNewAndDeleteExistingHandles(request, uinObject, UPDATE);
					RequestDTO requestDTO = request.getRequest();
					Configuration configuration = Configuration.builder().jsonProvider(new JacksonJsonProvider())
							.mappingProvider(new JacksonMappingProvider()).build();
					DocumentContext inputData = JsonPath.using(configuration).parse(requestDTO.getIdentity());
					DocumentContext dbData = JsonPath.using(configuration).parse(new String(uinObject.getUinData()));
					anonymousProfileHelper.setOldUinData(dbData.jsonString().getBytes());
					updateVerifiedAttributes(requestDTO, inputData, dbData);
					replaceConfiguredFieldsOnUpdate(inputData, dbData);
					JSONCompareResult comparisonResult = JSONCompare.compareJSON(inputData.jsonString(),
							dbData.jsonString(), JSONCompareMode.LENIENT);

					if (comparisonResult.failed()) {
						updateJsonObject(uinHash, inputData, dbData, comparisonResult, true);
					}
					uinObject.setUinData(convertToBytes(convertToObject(dbData.jsonString().getBytes(), Map.class)));
					uinObject.setUinDataHash(securityManager.hash(uinObject.getUinData()));
					uinObject.setUpdatedBy(IdRepoSecurityManager.getUser());
					uinObject.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());

					if (Objects.nonNull(requestDTO.getDocuments()) && !requestDTO.getDocuments().isEmpty()) {
						anonymousProfileHelper
								.setNewCbeff(uinObject.getUinHash(),
										!anonymousProfileHelper.isNewCbeffPresent() ?
										uinObject.getBiometrics().get(uinObject.getBiometrics().size() - 1).getBioFileId()
												: null);
						updateDocuments(uinHashWithSalt, uinObject, requestDTO, false);
						uinObject.setUpdatedBy(IdRepoSecurityManager.getUser());
						uinObject.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
					}
				}

				uinObject = uinRepo.save(uinObject);
				anonymousProfileHelper.setNewUinData(uinObject.getUinData());
				uinHistoryRepo.save(new UinHistory(uinObject.getUinRefId(), DateUtils2.getUTCCurrentDateTime(),
						uinObject.getUin(), uinObject.getUinHash(), uinObject.getUinData(), uinObject.getUinDataHash(),
						uinObject.getRegId(), uinObject.getStatusCode(), IdRepoSecurityManager.getUser(),
						DateUtils2.getUTCCurrentDateTime(), IdRepoSecurityManager.getUser(),
						DateUtils2.getUTCCurrentDateTime(), false, null));

				addIdentityHandle(uinObject, handlesToAdd);

				issueCredential(uin, uinObject.getUin(), uinObject.getStatusCode(),
						DateUtils2.getUTCCurrentDateTime(), uinObject.getRegId(), true);

				anonymousProfileHelper.buildAndsaveProfile(false);
				return uinObject;
			} catch (JSONException | InvalidJsonException | IOException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, UPDATE_IDENTITY, e.getMessage());
				throw new IdRepoAppException(ID_OBJECT_PROCESSING_FAILED, e);
			} catch (IdRepoAppUncheckedException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, UPDATE_IDENTITY,
						"\n" + e.getErrorText());
				throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
			}
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, ID_REPO_SERVICE_IMPL, UPDATE_IDENTITY, startNanos);
		}
	}

	/**
	 * When {@link #trimWhitespaces} is on, trims leading and trailing whitespace from the top-level
	 * identity values: plain strings, strings inside lists, and the {@code value} of language or
	 * tagged entries ({@code {language, value}}, {@code {value, tags}}). Nested objects other than
	 * that {@code value} key are left as they are.
	 *
	 * @param request identity request; its identity is replaced with the trimmed map
	 * @throws IdRepoAppException if the identity cannot be converted to a map
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateRequestBodyData(IdRequestDTO request) throws IdRepoAppException {
		if (trimWhitespaces && Objects.nonNull(request.getRequest().getIdentity())) {
			Map<String, Object> identityData = idRepoServiceHelper.convertToMap(request.getRequest().getIdentity());
			Map<String, Object> updatedIdentityData = identityData.entrySet().stream().map(attributeData -> {
				if (attributeData.getValue() instanceof String) {
					attributeData.setValue(((String) attributeData.getValue()).trim());
				} else if (attributeData.getValue() instanceof List) {
					List<Object> list = (List<Object>) attributeData.getValue();
					for (int i = 0; i < list.size(); i++) {
						Object item = list.get(i);
						if (item instanceof String) {
							list.set(i, ((String) item).trim());
						} else if (item instanceof Map) {
							Map<String, Object> map = (Map<String, Object>) item;
							Object v = map.get(VALUE);
							if (v instanceof String) {
								map.put(VALUE, ((String) v).trim());
							}
						}
					}
				} else if (attributeData.getValue() instanceof Map) {
					Map map = (Map) attributeData.getValue();
					Object v = map.get(VALUE);
					if (v instanceof String) {
						map.put(VALUE, ((String) v).trim());
					}
				}
				return attributeData;
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			request.getRequest().setIdentity(updatedIdentityData);
		}
	}

	/**
	 * Merges the request's verified attributes into the stored identity and writes the result
	 * to both documents, so the later JSON comparison sees no difference for this key.
	 * <ul>
	 * <li>v2 ({@code verifiedAttributesMetadata} set): delegates to
	 * {@link #applyV2VerifiedAttributes}.</li>
	 * <li>v1: the stored list of claim names and the request list are combined into a set
	 * (duplicates removed; order not kept).</li>
	 * </ul>
	 *
	 * @param requestDTO update request body
	 * @param inputData  request identity JSON; changed in place
	 * @param dbData     stored identity JSON; changed in place
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void updateVerifiedAttributes(RequestDTO requestDTO, DocumentContext inputData, DocumentContext dbData) {
		if (requestDTO.getVerifiedAttributesMetadata() != null) {
			applyV2VerifiedAttributes(requestDTO.getVerifiedAttributesMetadata(), inputData, dbData);
			return;
		}
		List dbVerifiedAttributes = (List) dbData.read(".verifiedAttributes");
		dbVerifiedAttributes.remove(null);
		if (dbVerifiedAttributes.isEmpty()) {
			dbVerifiedAttributes.add(new ArrayList<>());
		}
		List verifiedAttributeList = (List) dbVerifiedAttributes.get(0);
		if (Objects.nonNull(requestDTO.getVerifiedAttributes())) {
			verifiedAttributeList.addAll(requestDTO.getVerifiedAttributes());
		}
		HashSet<String> verifiedAttributesSet = new HashSet<>(verifiedAttributeList);
		inputData.put("$", VERIFIED_ATTRIBUTES, verifiedAttributesSet);
		dbData.put("$", VERIFIED_ATTRIBUTES, verifiedAttributesSet);
	}

	/**
	 * Merges v2 {@link VerificationMetadata} into the stored identity.
	 * <ol>
	 * <li>Copies the request entries and drops claims whose field has no value in either the
	 * request or the stored identity; entries left with no claims are dropped.</li>
	 * <li>Reads the stored entries (a legacy v1 list of names becomes one entry with framework
	 * and process {@code NA}).</li>
	 * <li>A stored entry whose trust framework and verification process (case-insensitive) are
	 * not in the request is kept, but only with the claims whose field is absent from the request
	 * identity; it is dropped if none remain. A stored entry with a matching framework and process
	 * is replaced by the request entry.</li>
	 * </ol>
	 * With no stored entries, the filtered request entries are used as they are. The merged list
	 * is written to both {@code inputData} and {@code dbData}.
	 *
	 * @param incoming  v2 entries from the request; not modified
	 * @param inputData request identity JSON; changed in place
	 * @param dbData    stored identity JSON (empty on add); changed in place
	 */
	private void applyV2VerifiedAttributes(List<VerificationMetadata> incoming, DocumentContext inputData,
			DocumentContext dbData) {
		List<VerificationMetadata> inputVerifiedAttributes = new ArrayList<>();
		for (VerificationMetadata item : incoming) {
			VerificationMetadata copy = new VerificationMetadata();
			copy.setTrustFramework(item.getTrustFramework());
			copy.setVerificationProcess(item.getVerificationProcess());
			copy.setClaims(item.getClaims() == null ? new ArrayList<>() : new ArrayList<>(item.getClaims()));
			copy.setMetadata(item.getMetadata());
			inputVerifiedAttributes.add(copy);
		}
		retainClaimsThatExist(inputData, dbData, inputVerifiedAttributes);

		List<VerificationMetadata> stored = readStoredVerificationMetadata(dbData);
		if (stored.isEmpty()) {
			inputData.put("$", VERIFIED_ATTRIBUTES, inputVerifiedAttributes);
			dbData.put("$", VERIFIED_ATTRIBUTES, inputVerifiedAttributes);
			return;
		}
		for (VerificationMetadata saved : stored) {
			boolean updated = inputVerifiedAttributes.stream()
					.anyMatch(vm -> StringUtils.equalsIgnoreCase(saved.getTrustFramework(), vm.getTrustFramework())
							&& StringUtils.equalsIgnoreCase(saved.getVerificationProcess(), vm.getVerificationProcess()));
			if (!updated) {
				List<String> claims = saved.getClaims() == null ? List.of() : saved.getClaims();
				List<String> claimsNotUpdated = claims.stream()
						.filter(claim -> !fieldHasValue(inputData, claim))
						.collect(Collectors.toList());
				if (!claimsNotUpdated.isEmpty()) {
					saved.setClaims(claimsNotUpdated);
					inputVerifiedAttributes.add(saved);
				}
			}
		}
		inputData.put("$", VERIFIED_ATTRIBUTES, inputVerifiedAttributes);
		dbData.put("$", VERIFIED_ATTRIBUTES, inputVerifiedAttributes);
	}

	/**
	 * Removes, from each entry, claims whose field has no value in either document, then removes
	 * entries left without claims.
	 *
	 * @param inputData               request identity JSON
	 * @param dbData                  stored identity JSON
	 * @param inputVerifiedAttributes entries to filter; changed in place
	 */
	private void retainClaimsThatExist(DocumentContext inputData, DocumentContext dbData,
			List<VerificationMetadata> inputVerifiedAttributes) {
		for (VerificationMetadata metadata : inputVerifiedAttributes) {
			List<String> claims = metadata.getClaims() == null ? List.of() : metadata.getClaims();
			metadata.setClaims(claims.stream()
					.filter(claim -> fieldHasValue(inputData, claim) || fieldHasValue(dbData, claim))
					.collect(Collectors.toList()));
		}
		inputVerifiedAttributes.removeIf(item -> item.getClaims() == null || item.getClaims().isEmpty());
	}

	/**
	 * @param data    identity JSON
	 * @param fieldId top-level field name
	 * @return true when {@code $.fieldId} exists and is not null
	 */
	private boolean fieldHasValue(DocumentContext data, String fieldId) {
		try {
			return data.read("$." + fieldId) != null;
		} catch (PathNotFoundException ignored) {
			return false;
		}
	}

	/**
	 * Reads the stored verified attributes as v2 entries.
	 * <p>
	 * A legacy v1 list of claim names is converted to a single entry with trust framework and
	 * verification process {@code NA} and the current time, so it can be merged with v2 data.
	 * </p>
	 *
	 * @param dbData stored identity JSON
	 * @return mutable list of entries; empty when the key is absent, null or empty
	 */
	private List<VerificationMetadata> readStoredVerificationMetadata(DocumentContext dbData) {
		List<?> matches;
		try {
			matches = dbData.read(".verifiedAttributes");
		} catch (PathNotFoundException ex) {
			return new ArrayList<>();
		}
		if (matches == null || matches.isEmpty() || matches.get(0) == null) {
			return new ArrayList<>();
		}
		Object stored = matches.get(0);
		if (!(stored instanceof List<?> storedList) || storedList.isEmpty()) {
			return new ArrayList<>();
		}
		if (storedList.stream().allMatch(String.class::isInstance)) {
			VerificationMetadata converted = new VerificationMetadata();
			converted.setClaims(storedList.stream().map(String.class::cast).collect(Collectors.toList()));
			converted.setTrustFramework("NA");
			converted.setVerificationProcess("NA");
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("trust_framework", "NA");
			metadata.put("verification_process", "NA");
			metadata.put("time", DateUtils2.formatToISOString(DateUtils2.getUTCCurrentDateTime()));
			converted.setMetadata(metadata);
			return new ArrayList<>(List.of(converted));
		}
		return mapper.convertValue(storedList, new TypeReference<List<VerificationMetadata>>() {
		});
	}

	/**
	 * Wraps an object (map, list or JSON string) in a JsonPath document backed by Jackson, so
	 * POJOs such as {@link VerificationMetadata} can be written into it.
	 *
	 * @param source value to parse
	 * @return a writable document
	 */
	private static DocumentContext jacksonJson(Object source) {
		Configuration configuration = Configuration.builder().jsonProvider(new JacksonJsonProvider())
				.mappingProvider(new JacksonMappingProvider()).build();
		return JsonPath.using(configuration).parse(source);
	}

	/**
	 * Copies each field listed in {@link #fieldsToReplaceOnUpdate} from the request to the
	 * stored identity, replacing the stored value whole. Fields missing or null in the request
	 * are left as they are.
	 *
	 * @param inputData identity JSON from the update request
	 * @param dbData    stored identity JSON; changed in place
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void replaceConfiguredFieldsOnUpdate(DocumentContext inputData, DocumentContext dbData) {
		for(String fieldId : fieldsToReplaceOnUpdate) {
			List readValue = (List) inputData.read(DOT + fieldId);
			if (!readValue.isEmpty()) {
				Object value = readValue.get(0);
				if (Objects.nonNull(value)) {
					dbData.put("$", fieldId, value);
				}
			}
		}
	}

	/**
	 * Merges the request identity into the stored identity and enforces update-count limits.
	 * <ol>
	 * <li>Replaces array-handle fields ({@code [{value, tags}]}) whole.</li>
	 * <li>Adds fields missing from the stored identity.</li>
	 * <li>Overwrites fields whose values differ.</li>
	 * <li>Merges language lists entry by entry.</li>
	 * <li>When {@code canPersistUpdateCount} is true, adds one to the count of every changed
	 * attribute.</li>
	 * </ol>
	 * The JSON is compared (LENIENT) again before each step. If it still differs after the last
	 * step, the remaining diff is logged and the update fails; calling this method again would
	 * never converge and would overflow the stack. Otherwise the count map is saved to
	 * {@code identity_update_count_tracker}, even when counting is off.
	 *
	 * @param uinHash               salted UIN hash; id of the update-count tracker row
	 * @param inputData             request identity JSON
	 * @param dbData                stored identity JSON; changed in place
	 * @param comparisonResult      first comparison; recomputed before each step
	 * @param canPersistUpdateCount whether changed attributes count against their limits
	 * @throws JSONException      if a document cannot be compared
	 * @throws IOException        if the stored count map cannot be read or written
	 * @throws IdRepoAppException {@code UPDATE_COUNT_LIMIT_EXCEEDED}, or {@code UNKNOWN_ERROR}
	 *                            when a diff remains after the merge
	 */
	protected void updateJsonObject(String uinHash, DocumentContext inputData, DocumentContext dbData,
			JSONCompareResult comparisonResult, boolean canPersistUpdateCount) throws JSONException, IOException, IdRepoAppException {
		Entry<String, Map<String, Integer>> updateCountTracker = getUpdateCountTracker(uinHash, dbData);
		Map<String, Integer> updateCountTrackerMap = updateCountTracker.getValue();
		Set<String> attribute = new HashSet<>();

		replaceArrayHandleFields(inputData, dbData, attribute);
		comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(), JSONCompareMode.LENIENT);

		if (comparisonResult.isMissingOnField()) {
			updateMissingFields(dbData, comparisonResult, attribute);
		}

		comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(), JSONCompareMode.LENIENT);
		if (comparisonResult.isFailureOnField()) {
			updateFailingFields(inputData, dbData, comparisonResult, attribute);
		}

		comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(), JSONCompareMode.LENIENT);
		if (!comparisonResult.getMessage().isEmpty()) {
			updateMissingValues(inputData, dbData, comparisonResult, attribute);
		}
		if(canPersistUpdateCount) {
			updateCount(updateCountTrackerMap, attribute);
		}
		comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(), JSONCompareMode.LENIENT);
		if (comparisonResult.failed()) {
			// Code should never reach here
			//updateJsonObject(uinHash, inputData, dbData, comparisonResult, true);
			logRemainingDiff(uinHash, inputData, dbData, comparisonResult, attribute);
			throw new IdRepoAppException(UNKNOWN_ERROR);
		}
		identityUpdateTracker.save(new IdentityUpdateTracker(updateCountTracker.getKey(), CryptoUtil
				.encodeToURLSafeBase64(mapper.writeValueAsString(updateCountTrackerMap).getBytes()).getBytes()));
	}

	/**
	 * Copies array-handle fields from the request to the stored identity, replacing the stored
	 * list whole. The LENIENT comparison cannot match such lists by position, so they are not
	 * merged entry by entry. Fields equal to the stored value are skipped.
	 *
	 * @param inputData request identity JSON
	 * @param dbData    stored identity JSON; changed in place
	 * @param attribute names of changed attributes; replaced fields are added
	 */
	@SuppressWarnings("unchecked")
	private void replaceArrayHandleFields(DocumentContext inputData, DocumentContext dbData, Set<String> attribute) {
		Map<String, Object> inputMap = inputData.read(ROOT, Map.class);
		Map<String, Object> dbMap = dbData.read(ROOT, Map.class);
		for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
			if (!isArrayHandleList(entry.getValue()) || Objects.equals(dbMap.get(entry.getKey()), entry.getValue())) {
				continue;
			}
			dbData.put(ROOT, entry.getKey(), new ArrayList<>((List<Object>) entry.getValue()));
			attribute.add(entry.getKey());
		}
	}

	/**
	 * @param value an identity field value
	 * @return true when the value is a non-empty list of objects that each have {@code value},
	 *         optionally {@code tags}, no {@code language}, and no other keys
	 */
	private boolean isArrayHandleList(Object value) {
		if (!(value instanceof List<?> list) || list.isEmpty()) {
			return false;
		}
		for (Object item : list) {
			if (!(item instanceof Map<?, ?> map) || !map.containsKey(VALUE) || map.containsKey(LANGUAGE)) {
				return false;
			}
			for (Object key : map.keySet()) {
				if (!VALUE.equals(key) && !"tags".equals(key)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Logs why the merge in {@link #updateJsonObject} did not converge.
	 * <p>
	 * Paths and counts go to ERROR. Request and merged stored values of the attributes named in
	 * the diff go to DEBUG only, because they can hold personal data.
	 * </p>
	 *
	 * @param uinHash          salted UIN hash of the identity being updated
	 * @param inputData        request identity after the merge passes
	 * @param dbData           stored identity after the merge passes
	 * @param comparisonResult final LENIENT comparison that still fails
	 * @param attribute        attributes already changed by the merge passes
	 */
	@SuppressWarnings("unchecked")
	private void logRemainingDiff(String uinHash, DocumentContext inputData, DocumentContext dbData,
			JSONCompareResult comparisonResult, Set<String> attribute) {
		String message = comparisonResult.getMessage();
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "updateJsonObject",
				"Comparison still failing after all update passes for uinHash: " + uinHash
						+ " - remaining diff: " + message
						+ " - fieldFailures: " + describeFailures(comparisonResult.getFieldFailures())
						+ " - fieldMissing: " + describeFailures(comparisonResult.getFieldMissing())
						+ " - fieldUnexpected: " + describeFailures(comparisonResult.getFieldUnexpected())
						+ " - attributesMergedSoFar: " + attribute);

		Map<String, Object> inputMap = inputData.read(ROOT, Map.class);
		Map<String, Object> dbMap = dbData.read(ROOT, Map.class);
		Set<String> keys = new LinkedHashSet<>(inputMap.keySet());
		keys.addAll(dbMap.keySet());
		keys.stream()
				.filter(key -> Pattern.compile("(^|[\\s.;])" + Pattern.quote(key) + "[\\[.:]").matcher(message).find())
				.forEach(key -> {
					Object requestValue = inputMap.get(key);
					Object storedValue = dbMap.get(key);
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "updateJsonObject",
							"Diff attribute '" + key + "' - request: " + describeShape(requestValue)
									+ ", merged stored: " + describeShape(storedValue));
					mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "updateJsonObject",
							"Diff attribute '" + key + "' - request value: " + requestValue
									+ " | merged stored value: " + storedValue);
				});
	}

	/**
	 * Lists comparison failures by path and value shape, without values, so the result is safe
	 * to log at ERROR.
	 *
	 * @param failures field failures from a {@link JSONCompareResult}
	 * @return text such as {@code [email (expected list of 2, actual list of 1)]}
	 */
	private String describeFailures(List<FieldComparisonFailure> failures) {
		return failures.stream()
				.map(failure -> failure.getField() + " (expected " + describeShape(failure.getExpected())
						+ ", actual " + describeShape(failure.getActual()) + ")")
				.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Describes a JSON value without exposing it.
	 *
	 * @param value any JSON value, or null
	 * @return {@code absent}, {@code list of N}, {@code object}, or the simple class name
	 */
	private String describeShape(Object value) {
		if (value == null) {
			return "absent";
		}
		if (value instanceof List) {
			return "list of " + ((List<?>) value).size();
		}
		if (value instanceof JSONArray) {
			return "list of " + ((JSONArray) value).length();
		}
		if (value instanceof Map || value instanceof JSONObject) {
			return "object";
		}
		return value.getClass().getSimpleName();
	}

	/**
	 * Adds one to the update count of every changed attribute that has a configured limit.
	 * <p>
	 * A count never goes above the limit. All attributes are processed first; then, if any of
	 * them had already reached its limit before this update, the update is rejected.
	 * </p>
	 *
	 * @param updateCountTrackerMap counts by attribute; changed in place
	 * @param attributeSet          attributes changed by this update
	 * @throws IdRepoAppException {@code UPDATE_COUNT_LIMIT_EXCEEDED}, listing the attributes at their limit
	 */
	private void updateCount(Map<String, Integer> updateCountTrackerMap, Set<String> attributeSet) throws IdRepoAppException {
		mosipLogger.debug("Entering updateCount");
		List<String> attributesHavingLimitExceeded = new ArrayList<>();
		attributeSet.forEach( attribute -> {
			mosipLogger.debug("Processing attribute: {}", attribute);
					if (IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap().containsKey(attribute)) {
						Integer currentUpdateCount = updateCountTrackerMap.get(attribute);
						mosipLogger.debug("Current Update Count for {}: {}", attribute, currentUpdateCount);
						if (currentUpdateCount != null) {
							int maxUpdateCountLimit = IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit(attribute);
							mosipLogger.debug("Max Update Count Limit for {}: {}", attribute, maxUpdateCountLimit);
							if (maxUpdateCountLimit - currentUpdateCount <= 0) {
								attributesHavingLimitExceeded.add(attribute);
								mosipLogger.debug("Limit exceeded for {}: {}", attribute, currentUpdateCount);
							}
						}
						updateCountTrackerMap.compute(attribute,
								(k, v) -> (Objects.nonNull(v) ? v + 1 : 1) < IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit(k)
										? (Objects.nonNull(v) ? v + 1 : 1)
										: IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit(k));
						mosipLogger.debug("Updated count for {}: {}", attribute, updateCountTrackerMap.get(attribute));
					}
		}
		);
		if (!attributesHavingLimitExceeded.isEmpty()) {
			String exceededAttributes = String.join(COMMA, attributesHavingLimitExceeded);
			mosipLogger.debug("Limit exceeded for attributes: {}", exceededAttributes);
			throw new IdRepoAppException(UPDATE_COUNT_LIMIT_EXCEEDED.getErrorCode(),
					String.format(UPDATE_COUNT_LIMIT_EXCEEDED.getErrorMessage(), exceededAttributes));
		}
		mosipLogger.debug("Exiting updateCount");
	}

	/**
	 * Loads how many times each attribute of the identity has been updated.
	 * <p>
	 * The count map is stored in {@code identity_update_count_tracker} as URL-safe Base64 JSON.
	 * </p>
	 *
	 * @param uinHash salted UIN hash; the tracker's id
	 * @param dbData  stored identity JSON (not used)
	 * @return entry of the UIN hash and a mutable count map; the map is empty when nothing is stored
	 * @throws IOException          if the stored value cannot be decoded
	 * @throws JsonParseException   if the stored JSON is malformed
	 * @throws JsonMappingException if the stored JSON is not a map of counts
	 */
	private Entry<String, Map<String, Integer>> getUpdateCountTracker(String uinHash, DocumentContext dbData)
			throws IOException, JsonParseException, JsonMappingException {
		Optional<IdentityUpdateTracker> updateTrackerOptional = identityUpdateTracker.findById(uinHash);
		Map<String, Integer> updateCountTrackerMap = new HashMap<>();
		if (updateTrackerOptional.isPresent()) {
			updateCountTrackerMap = new HashMap<>(mapper.readValue(
					CryptoUtil.decodeURLSafeBase64(new String(updateTrackerOptional.get().getIdentityUpdateCount())),
					new TypeReference<Map<String, Integer>>() {
					}));
		}
		return Map.entry(uinHash, updateCountTrackerMap);
	}
	
	/**
	 * First merge step of an update: adds fields that are in the request but not in the stored
	 * identity.
	 * <p>
	 * The comparison treats the request as expected and the stored JSON as actual, so a
	 * "missing" field exists only in the request. For an array path (a multi-language value),
	 * the request entry is appended to the stored list. Otherwise the field is created.
	 * </p>
	 *
	 * @param dbData           stored identity JSON; changed in place
	 * @param comparisonResult LENIENT comparison of the request against {@code dbData}
	 * @param attribute        collects the names of changed top-level attributes for update counting
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateMissingFields(DocumentContext dbData, JSONCompareResult comparisonResult,
									 Set<String> attribute) {
		for (FieldComparisonFailure failure : comparisonResult.getFieldMissing()) {
			if (StringUtils.contains(failure.getField(), OPEN_SQUARE_BRACE)) {
				String path = StringUtils.substringBefore(failure.getField(), OPEN_SQUARE_BRACE);
				String key = StringUtils.substringAfterLast(path, DOT);
				path = StringUtils.substringBeforeLast(path, DOT);

				if (StringUtils.isEmpty(key)) {
					key = path;
					path = ROOT;
				}
				attribute.add(key);
				List value = dbData.read(path + DOT + key, List.class);
				value.addAll(Collections
						.singletonList(convertToObject(failure.getExpected().toString().getBytes(), Map.class)));

				dbData.put(path, key, value);
			} else {
				String path = StringUtils.substringBeforeLast(failure.getField(), DOT);
				if (StringUtils.isEmpty(path)) {
					path = ROOT;
				}
				String key = StringUtils.substringAfterLast(failure.getField(), DOT);
				attribute.add(key);
				dbData.put(path, (String) failure.getExpected(), key);
			}
		}
	}

	/**
	 * Second merge step of an update: overwrites stored values that differ from the request.
	 * <p>
	 * An array-index path from the comparison (for example {@code fullName[language=eng]}) is
	 * turned into a JsonPath filter ({@code fullName[?(@.language=='eng')]}) so the matching
	 * entry is updated. Arrays and objects are written whole. A scalar is written to both
	 * documents, except that a {@code null} in the request keeps the stored value (copied into
	 * {@code inputData} so the documents match).
	 * </p>
	 *
	 * @param inputData        identity JSON from the request; changed in place to match
	 * @param dbData           stored identity JSON; changed in place
	 * @param comparisonResult LENIENT comparison of the request against {@code dbData}
	 * @param attribute        collects the names of changed top-level attributes for update counting
	 */
	private void updateFailingFields(DocumentContext inputData, DocumentContext dbData,
									 JSONCompareResult comparisonResult, Set<String> attribute) {
		for (FieldComparisonFailure failure : comparisonResult.getFieldFailures()) {

			String path = StringUtils.substringBeforeLast(failure.getField(), DOT);
			if (StringUtils.contains(path, OPEN_SQUARE_BRACE)) {
				path = RegExUtils.replaceAll(path, "\\[", "\\[\\?\\(\\@\\.");
				path = RegExUtils.replaceAll(path, "=", "=='");
				path = RegExUtils.replaceAll(path, "\\]", "'\\)\\]");
			}

			String key = StringUtils.substringAfterLast(failure.getField(), DOT);
			if (StringUtils.isEmpty(key)) {
				key = failure.getField();
				path = ROOT;
			}
			attribute.add(StringUtils.substringBefore(failure.getField(), OPEN_SQUARE_BRACE));
			if (failure.getExpected() instanceof JSONArray) {
				dbData.put(path, key, convertToObject(failure.getExpected().toString().getBytes(), List.class));
				inputData.put(path, key, convertToObject(failure.getExpected().toString().getBytes(), List.class));
			} else if (failure.getExpected() instanceof JSONObject) {
				Object object = convertToObject(failure.getExpected().toString().getBytes(), ObjectNode.class);
				dbData.put(path, key, object);
				inputData.put(path, key, object);
			} else {
				if (!failure.getExpected().toString().contentEquals("null")) {
					dbData.put(path, key, failure.getExpected());
					inputData.put(path, key, failure.getExpected());
				} else {
					inputData.put(path, key, failure.getActual());
				}
			}
		}
	}

	/**
	 * Last merge step of an update: lines up the language entries of a multi-language field.
	 * <p>
	 * The field is taken from the comparison message. Request entries whose language is not
	 * stored are added to {@code dbData}; stored entries whose language is not in the request
	 * are added to {@code inputData}. Afterwards both lists cover the same languages, so
	 * languages not sent in the update are kept.
	 * </p>
	 * <p>
	 * A list holding plain values (for example a list of phone numbers or handle field ids)
	 * has no language entries to line up, so the stored list is replaced by the request list.
	 * </p>
	 *
	 * @param inputData        identity JSON from the request; changed in place
	 * @param dbData           stored identity JSON; changed in place
	 * @param comparisonResult LENIENT comparison whose message names the field to fix
	 * @param attribute        collects the name of the changed attribute for update counting
	 */
	@SuppressWarnings("unchecked")
	private void updateMissingValues(DocumentContext inputData, DocumentContext dbData,
									 JSONCompareResult comparisonResult, Set<String> attribute) {
		String path = StringUtils.substringBefore(comparisonResult.getMessage(), OPEN_SQUARE_BRACE);
		String key = StringUtils.substringAfterLast(path, DOT);
		path = StringUtils.substringBeforeLast(path, DOT);

		if (StringUtils.isEmpty(key)) {
			key = path;
			path = ROOT;
		}
		attribute.add(key);
		JsonPath jsonPath = JsonPath.compile(path + DOT + key);
		List<Object> dbValues = dbData.read(path + DOT + key, List.class);
		List<Object> inputValues = inputData.read(path + DOT + key, List.class);
		if (Stream.concat(dbValues.stream(), inputValues.stream()).anyMatch(value -> !(value instanceof Map))) {
			dbData.set(jsonPath, inputValues);
			return;
		}
		List<Map<String, String>> dbDataList = (List<Map<String, String>>) (List<?>) dbValues;
		List<Map<String, String>> inputDataList = (List<Map<String, String>>) (List<?>) inputValues;
		inputDataList.stream()
				.filter(map -> map.containsKey(LANGUAGE) && dbDataList.stream().filter(dbMap -> dbMap.containsKey(LANGUAGE))
						.allMatch(dbMap -> !StringUtils.equalsIgnoreCase(dbMap.get(LANGUAGE), map.get(LANGUAGE))))
				.forEach(value -> dbData.add(jsonPath, value));
		dbDataList.stream()
				.filter(map -> map.containsKey(LANGUAGE)
						&& inputDataList.stream().filter(inputDataMap -> inputDataMap.containsKey(LANGUAGE)).allMatch(
						inputDataMap -> !StringUtils.equalsIgnoreCase(inputDataMap.get(LANGUAGE), map.get(LANGUAGE))))
				.forEach(value -> inputData.add(jsonPath, value));
	}

	/**
	 * Same as {@link #updateDocuments(String, Uin, RequestDTO, boolean, boolean)} with the UIN-hash
	 * object path ({@code useDraftObjectStore = false}); used by live updates and v1 drafts.
	 *
	 * @param uinHashwithSalt UIN hash without the salt-id prefix; the object-store folder
	 * @param uinObject       identity (or draft mapped to {@link Uin}) being updated
	 * @param requestDTO      update request with the documents
	 * @param isDraft         {@code true} when the metadata rows belong to a draft
	 * @throws IdRepoAppException when a file cannot be read, merged or written
	 */
	protected void updateDocuments(String uinHashwithSalt, Uin uinObject, RequestDTO requestDTO, boolean isDraft)
			throws IdRepoAppException {
		updateDocuments(uinHashwithSalt, uinObject, requestDTO, isDraft, false);
	}

	/**
	 * Merges the request's biometrics into the stored CBEFF, writes the request documents,
	 * and adds or replaces the identity's biometric and document metadata rows.
	 *
	 * @param uinHashwithSalt     uin hash used as the object path; for V2 drafts, the rid hash
	 * @param uinObject           identity (or draft mapped to {@link Uin}) being updated
	 * @param requestDTO          update request with the documents
	 * @param isDraft             {@code true} when the metadata rows belong to a draft
	 * @param useDraftObjectStore {@code true} only for v2 draft APIs, which store
	 *                            files under {@code _draft/{ridHash}/...}. v1 drafts
	 *                            and live identity updates use the UIN-hash path
	 *                            {@code {uinHash}/Biometrics|Demographics/...}.
	 * @throws IdRepoAppException when a file cannot be read, merged or written
	 */
	protected void updateDocuments(String uinHashwithSalt, Uin uinObject, RequestDTO requestDTO, boolean isDraft,
			boolean useDraftObjectStore) throws IdRepoAppException {
		List<UinDocument> docList = new ArrayList<>();
		List<UinBiometric> bioList = new ArrayList<>();

		if (Objects.nonNull(uinObject.getBiometrics())) {
			updateCbeff(uinObject, requestDTO, useDraftObjectStore);
		}

		addDocuments(uinHashwithSalt, convertToBytes(requestDTO.getIdentity()), requestDTO.getDocuments(),
				uinObject.getUinRefId(), docList, bioList, isDraft, useDraftObjectStore);

		docList.stream().forEach(doc -> uinObject.getDocuments().stream()
				.filter(docObj -> StringUtils.equals(doc.getDoccatCode(), docObj.getDoccatCode())).forEach(docObj -> {
					docObj.setDocId(doc.getDocId());
					docObj.setDocName(doc.getDocName());
					docObj.setDoctypCode(doc.getDoctypCode());
					docObj.setDocfmtCode(doc.getDocfmtCode());
					docObj.setDocHash(doc.getDocHash());
					docObj.setUpdatedBy(IdRepoSecurityManager.getUser());
					docObj.setUpdatedDateTime(doc.getUpdatedDateTime());
				}));
		docList.stream()
				.filter(doc -> uinObject.getDocuments().stream()
						.allMatch(docObj -> !StringUtils.equals(doc.getDoccatCode(), docObj.getDoccatCode())))
				.forEach(doc -> uinObject.getDocuments().add(doc));
		bioList.stream()
				.forEach(bio -> uinObject.getBiometrics().stream()
						.filter(bioObj -> StringUtils.equals(bio.getBiometricFileType(), bioObj.getBiometricFileType()))
						.forEach(bioObj -> {
							bioObj.setBioFileId(bio.getBioFileId());
							bioObj.setBiometricFileName(bio.getBiometricFileName());
							bioObj.setBiometricFileHash(bio.getBiometricFileHash());
							bioObj.setUpdatedBy(IdRepoSecurityManager.getUser());
							bioObj.setUpdatedDateTime(bio.getUpdatedDateTime());
						}));
		bioList.stream()
				.filter(bio -> uinObject.getBiometrics().stream()
						.allMatch(bioObj -> !StringUtils.equals(bio.getBioFileId(), bioObj.getBioFileId())))
				.forEach(bio -> uinObject.getBiometrics().add(bio));
	}

	/**
	 * Merges each request CBEFF with the stored CBEFF of the same biometric file type, so
	 * modalities missing from the request are kept. The stored CBEFF is read from the
	 * live path, or from the draft path when {@code useDraftObjectStore} is set.
	 *
	 * @param uinObject           identity whose stored biometric files are read; for V2 drafts
	 *                            its uin hash is {@code _{ridHash}}
	 * @param requestDTO          request whose biometric documents are replaced by the merged CBEFF
	 * @param useDraftObjectStore {@code true} to read the stored CBEFF from {@code _draft/{ridHash}/}
	 * @throws IdRepoAppUncheckedException the read error when a stored CBEFF cannot be read;
	 *         {@code INVALID_INPUT_PARAMETER} when the request CBEFF cannot be merged
	 */
	private void updateCbeff(Uin uinObject, RequestDTO requestDTO, boolean useDraftObjectStore) {
		ObjectNode identityMap = convertToObject(uinObject.getUinData(), ObjectNode.class);
		IntStream.range(0, uinObject.getBiometrics().size()).forEach(index -> {
			UinBiometric bio = uinObject.getBiometrics().get(index);
			requestDTO.getDocuments().stream()
					.filter(doc -> StringUtils.equals(bio.getBiometricFileType(), doc.getCategory())).forEach(doc -> {
						try {
							String uinHash = uinObject.getUinHash().split("_")[1];
							String bioFileId = bio.getBioFileId();
							byte[] data = useDraftObjectStore
									? objectStoreHelper.getDraftBiometricObject(uinHash, bioFileId)
									: objectStoreHelper.getBiometricObject(uinHash, bioFileId);
								if (StringUtils.equalsIgnoreCase(
										identityMap.get(bio.getBiometricFileType()).get(FILE_FORMAT_ATTRIBUTE).asText(), CBEFF_FORMAT)
										&& bioFileId.endsWith(CBEFF_FORMAT)) {
									byte[] decodedBioData = CryptoUtil.decodeURLSafeBase64(doc.getValue());
									anonymousProfileHelper.setOldCbeff(CryptoUtil.encodeToURLSafeBase64(data));
									doc.setValue(CryptoUtil.encodeToURLSafeBase64(this.updateXML(decodedBioData, data)));
								}
						} catch (IdRepoAppUncheckedException e) {
							mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "updateCbeff",
									ExceptionUtils.getStackTrace(e));
							throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
						} catch (Exception e) {
							mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "updateCbeff",
									ExceptionUtils.getStackTrace(e));
							throw new IdRepoAppUncheckedException(INVALID_INPUT_PARAMETER.getErrorCode(),
									String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + index + "/value"));
						}
					});
		});
	}

	/**
	 * Merges a new CBEFF into the stored one, modality by modality.
	 * <p>
	 * Each BIR is keyed by its biometric type plus subtype (for example {@code FingerLeftIndexFinger}).
	 * A BIR in the input replaces the stored BIR with the same key or is added if there is none;
	 * stored BIRs not in the input are kept. The merged CBEFF is also given to the anonymous
	 * profile helper.
	 * </p>
	 *
	 * @param inputBioData    CBEFF XML from the update request
	 * @param existingBioData CBEFF XML currently stored
	 * @return the merged CBEFF XML
	 * @throws Exception if either CBEFF cannot be parsed or the result cannot be written
	 */
	private byte[] updateXML(byte[] inputBioData, byte[] existingBioData) throws Exception {
		List<BIR> existingBIRData = cbeffUtil.getBIRDataFromXML(existingBioData);
		List<BIR> inputBIRData = cbeffUtil.getBIRDataFromXML(inputBioData);
		Map<String, BIR> inputBIRDataMap = inputBIRData.stream()
				.collect(Collectors.toMap(bir -> bir.getBdbInfo().getType().stream().map(BiometricType::value)
						.collect(Collectors.joining())
						.concat(bir.getBdbInfo().getSubtype().stream().collect(Collectors.joining())), bir -> bir));
		Map<String, BIR> existingBIRDataMap = existingBIRData.stream()
				.collect(Collectors.toMap(bir -> bir.getBdbInfo().getType().stream().map(BiometricType::value)
						.collect(Collectors.joining())
						.concat(bir.getBdbInfo().getSubtype().stream().collect(Collectors.joining())), bir -> bir));
		inputBIRDataMap.entrySet().forEach(entry -> {
			if (existingBIRDataMap.containsKey(entry.getKey())) {
				existingBIRDataMap.replace(entry.getKey(), entry.getValue());
			} else {
				existingBIRDataMap.put(entry.getKey(), entry.getValue());
			}
		});
		byte[] updatedCbeff = cbeffUtil.createXML(new ArrayList<>(existingBIRDataMap.values()));
		anonymousProfileHelper.setNewCbeff(CryptoUtil.encodeToURLSafeBase64(updatedCbeff));
		return updatedCbeff;
	}

	/**
	 * Not supported by this layer; RID lookup is done by {@link IdRepoProxyServiceImpl}.
	 *
	 * @param individualId UIN, VID or RID (not used)
	 * @param idType       type of {@code individualId} (not used)
	 * @return always {@code null}
	 * @throws IdRepoAppException never thrown here
	 */
	@Override
	public String getRidByIndividualId(String individualId, IdType idType) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			return null;
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, ID_REPO_SERVICE_IMPL, "getRidByIndividualId", startNanos);
		}
	}

	/**
	 * Not supported by this layer; metadata lookup is done by {@link IdRepoProxyServiceImpl}.
	 *
	 * @param individualId UIN or VID (not used)
	 * @param idType       type of {@code individualId} (not used)
	 * @return never returns
	 * @throws IdRepoAppException always, {@code UNKNOWN_ERROR} ("not implemented")
	 */
	@Override
	public IdVidMetadataResponseDTO getIdVidMetadata(String individualId, IdType idType) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			throw new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(),
					"getIdVidMetadata not implemented in IdRepoServiceImpl");
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, ID_REPO_SERVICE_IMPL, "getIdVidMetadata", startNanos);
		}
	}

	/**
	 * Returns how many more times each limited attribute of the identity may be updated.
	 * <ul>
	 * <li>With a tracker row: for each tracked attribute that has a limit (and is in
	 * {@code attributeList}, when given), remaining = max(0, limit − used). Then every limited
	 * attribute not yet in the result is added with its full limit; this step ignores
	 * {@code attributeList}.</li>
	 * <li>Without a tracker row: the configured limits, filtered by {@code attributeList}.</li>
	 * </ul>
	 *
	 * @param uinHash       salted UIN hash; id of the tracker row
	 * @param idType        type of the id the caller resolved (not used)
	 * @param attributeList attributes to report; empty means all limited attributes
	 * @return remaining update count by attribute
	 * @throws IdRepoAppException {@code UNKNOWN_ERROR} if the stored count map cannot be read
	 */
	@Override
	public Map<String, Integer> getRemainingUpdateCountByIndividualId(String uinHash, IdType idType,
			List<String> attributeList) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				Optional<IdentityUpdateTracker> updateTrackerOptional = identityUpdateTracker.findById(uinHash);
				if (updateTrackerOptional.isPresent()) {
					IdentityUpdateTracker trackRecord = updateTrackerOptional.get();
					Map<String, Integer> updateCountMap = mapper.readValue(
							CryptoUtil.decodeURLSafeBase64(new String(trackRecord.getIdentityUpdateCount())),
							new TypeReference<Map<String, Integer>>() {
							});
					return addMissingAttributes(
							updateCountMap.entrySet().stream()
									.filter(entry -> attributeList.isEmpty() || attributeList.contains(entry.getKey()))
									.filter(entry -> IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap().containsKey(entry.getKey()))
									.collect(Collectors.toMap(
											Map.Entry::getKey,
											entry -> Math.max(0, IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit(entry.getKey()) - entry.getValue())
									))
					);
				} else {
					return getRemainingUpdateCountFromConfig(attributeList);
				}
			} catch (IOException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL,
						"getMaxAllowedUpdateCountForIndividualId", ExceptionUtils.getStackTrace(e));
				throw new IdRepoAppException(UNKNOWN_ERROR);
			}
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, ID_REPO_SERVICE_IMPL,
					"getRemainingUpdateCountByIndividualId", startNanos);
		}
	}

	/**
	 * Adds every attribute that has an update limit but no entry yet, using the configured
	 * limit as its remaining count (the attribute has never been updated).
	 *
	 * @param updateCountTracker remaining counts by attribute; changed in place
	 * @return the same map, now covering all limited attributes
	 */
	private Map<String, Integer> addMissingAttributes(Map<String, Integer> updateCountTracker) {
		IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap().entrySet().stream()
				.filter(entry -> !updateCountTracker.containsKey(entry.getKey()))
				.forEach(entry -> updateCountTracker.put(entry.getKey(), entry.getValue()));
		return updateCountTracker;
	}


	/**
	 * Returns remaining update counts for an identity that has never been updated, which are
	 * simply the configured limits.
	 *
	 * @param attributeList attributes to include; {@code null} or empty means all limited attributes.
	 *                      Attributes without a limit are left out.
	 * @return configured limit by attribute
	 */
	private Map<String, Integer> getRemainingUpdateCountFromConfig(List<String> attributeList) {
		if(attributeList == null || attributeList.isEmpty()){
			return IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap();
		} else {
			Map<String, Integer> updateCountMapFromPolicy = IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap();
			return attributeList.stream()
					.filter(updateCountMapFromPolicy::containsKey)
					.collect(Collectors.toMap(attribute -> attribute, updateCountMapFromPolicy::get));
		}
	}

	/**
	 * Records the credential work needed after an identity is added or updated, then queues
	 * credential issuance to run once the identity transaction has committed.
	 * <p>
	 * Credential request rows for the identity are changed as follows:
	 * </p>
	 * <ul>
	 * <li>Rows exist and the status is active: each row goes back to {@code NEW}.</li>
	 * <li>Rows exist and the status is not active: each row becomes {@code DELETED}.</li>
	 * <li>No rows exist: one row is created for the dummy online-verification partner, either
	 * {@code NEW} or {@code DELETED} (with {@code expiryTimestamp}) depending on the status.
	 * When convention-based ids are enabled, {@code requestId} becomes its request id.</li>
	 * </ul>
	 *
	 * @param uin             plaintext UIN; hashed to find the rows
	 * @param encryptedUin    encrypted UIN stored on a new row
	 * @param uinStatus       identity status after the change
	 * @param expiryTimestamp expiry for a deactivated identity; ignored when active
	 * @param requestId       request id to use for a new row; may be {@code null}
	 * @param isUpdate        {@code true} sets the trigger action to {@code UPDATE}, otherwise {@code CREATE}
	 */
	private void issueCredential(String uin, String encryptedUin, String uinStatus, LocalDateTime expiryTimestamp, String requestId, boolean isUpdate) {

		String uinHash = securityManager.getIdHashWithSaltModuloByPlainIdHash(uin, uinHashSaltRepo::retrieveSaltById);

		List<CredentialRequestStatus> credStatusList = credRequestRepo.findByIndividualIdHash(uinHash);
		String triggerAction = isUpdate ? CredentialTriggerAction.UPDATE.toString() : CredentialTriggerAction.CREATE.toString();

		if (!credStatusList.isEmpty() && uinStatus.contentEquals(activeStatus)) {
			credStatusList.forEach(credStatus -> {
				credStatus.setStatus(CredentialRequestStatusLifecycle.NEW.toString());
				credStatus.setUpdatedBy(IdRepoSecurityManager.getUser());
				credStatus.setTriggerAction(triggerAction);
				credStatus.setUpdDTimes(DateUtils2.getUTCCurrentDateTime());
				credRequestRepo.save(credStatus);
			});
		} else if (!credStatusList.isEmpty() && !uinStatus.contentEquals(activeStatus)) {
			credStatusList.forEach(credStatus -> {
				credStatus.setTriggerAction(triggerAction);
				credStatus.setStatus(CredentialRequestStatusLifecycle.DELETED.toString());
				credStatus.setUpdatedBy(IdRepoSecurityManager.getUser());
				credStatus.setUpdDTimes(DateUtils2.getUTCCurrentDateTime());
				credRequestRepo.save(credStatus);
			});
		} else if (credStatusList.isEmpty()) {
			CredentialRequestStatus credStatus = new CredentialRequestStatus();
			credStatus.setIndividualId(encryptedUin);
			credStatus.setIndividualIdHash(uinHash);
			credStatus.setPartnerId(dummyPartner.getDummyOLVPartnerId());
			credStatus.setStatus(uinStatus.contentEquals(activeStatus) ? CredentialRequestStatusLifecycle.NEW.toString() :
					CredentialRequestStatusLifecycle.DELETED.toString());
			credStatus.setTriggerAction(triggerAction);
			credStatus.setIdExpiryTimestamp(uinStatus.contentEquals(activeStatus) ? null : expiryTimestamp);
			credStatus.setCreatedBy(IdRepoSecurityManager.getUser());
			credStatus.setCrDTimes(DateUtils2.getUTCCurrentDateTime());
			if(enableConventionBasedId && (requestId != null)) {
				credStatus.setRequestId(requestId);
			} 
			credRequestRepo.save(credStatus);
		}

		scheduleSynchronizedCredentialPipeline(uin, encryptedUin, uinStatus, expiryTimestamp, isUpdate, requestId);
	}

	/**
	 * Enqueues sync credential issuance for after the identity TX returns (connection released).
	 * <p>
	 * Do not run credentials in {@code afterCommit}: Spring still holds the JDBC connection then.
	 * {@link IdRepoProxyServiceImpl} / draft publish drain via
	 * {@link CredentialStatusManager#runPendingAfterIdentityCommit()}.
	 * </p>
	 *
	 * @param uin             plaintext UIN
	 * @param encryptedUin    encrypted UIN as stored on the identity row
	 * @param uinStatus       identity status after the change
	 * @param expiryTimestamp expiry for a deactivated identity; may be {@code null}
	 * @param isUpdate        {@code true} for an update, {@code false} for a create
	 * @param requestId       credential request id to reuse; may be {@code null}
	 */
	private void scheduleSynchronizedCredentialPipeline(String uin, String encryptedUin, String uinStatus,
			LocalDateTime expiryTimestamp, boolean isUpdate, String requestId) {
		PendingCredentialSync.enqueue(uin, encryptedUin, uinStatus, expiryTimestamp, isUpdate, requestId);
	}

	/**
	 * Parses JSON bytes with the shared {@link ObjectMapper}. Unchecked so it can be used
	 * inside lambdas and streams.
	 *
	 * @param <T>      target type
	 * @param identity JSON bytes, for example stored identity data or a CBEFF diff value
	 * @param clazz    target class, for example {@link Map}, {@link List} or {@link ObjectNode}
	 * @return the parsed value
	 * @throws IdRepoAppUncheckedException {@code ID_OBJECT_PROCESSING_FAILED} if the bytes are not
	 *                                     valid JSON for {@code clazz}
	 */
	protected <T> T convertToObject(byte[] identity, Class<T> clazz) {
		try {
			return mapper.readValue(identity, clazz);
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "convertToObject", e.getMessage());
			throw new IdRepoAppUncheckedException(ID_OBJECT_PROCESSING_FAILED, e);
		}
	}

	/**
	 * Serializes a value to JSON bytes with the shared {@link ObjectMapper}; used for the stored
	 * identity data and its hash.
	 *
	 * @param identity value to serialize, usually the identity map
	 * @return UTF-8 JSON bytes
	 * @throws IdRepoAppException {@code ID_OBJECT_PROCESSING_FAILED} if the value cannot be serialized
	 */
	protected byte[] convertToBytes(Object identity) throws IdRepoAppException {
		try {
			return mapper.writeValueAsBytes(identity);
		} catch (JsonProcessingException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "convertToBytes", e.getMessage());
			throw new IdRepoAppException(ID_OBJECT_PROCESSING_FAILED, e);
		}
	}

	/**
	 * Gets the handles selected in the request and checks that none of them is already taken
	 * by a different UIN. On update, hashes already owned by {@code uinHash} are not duplicates.
	 *
	 * @param request                    identity create/update request
	 * @param uinHash                    salted UIN hash of the identity being written
	 * @param existingSelectedHandlesMap currently stored handles; {@code null} on create
	 * @param method                     {@link IdRepoConstants#CREATE} or {@link IdRepoConstants#UPDATE}
	 * @return handles keyed by field id; empty when the request selects none
	 * @throws IdRepoAppException {@code HANDLE_RECORD_EXISTS} when a handle hash is already stored
	 */
	private Map<String, List<HandleDto>> checkAndGetHandles(IdRequestDTO request, String uinHash,
			Map<String, List<HandleDto>> existingSelectedHandlesMap, String method) throws IdRepoAppException {
		Map<String, List<HandleDto>> handles = idRepoServiceHelper.getSelectedHandles(request.getRequest(),
				existingSelectedHandlesMap);
		if (handles != null && !handles.isEmpty()) {
			List<String> hashes = handles.values().stream()
					.filter(Objects::nonNull)
					.flatMap(List::stream)
					.map(HandleDto::getHandleHash)
					.collect(Collectors.toList());
			if (!hashes.isEmpty()) {
				List<String> uinHashFromDB = handleRepo.findUinHashByHandleHashes(hashes);
				boolean isDuplicate = !uinHashFromDB.isEmpty()
						&& !(UPDATE.equals(method) && uinHashFromDB.contains(uinHash));
				if (isDuplicate) {
					throw new IdRepoAppException(HANDLE_RECORD_EXISTS.getErrorCode(),
							String.format(HANDLE_RECORD_EXISTS.getErrorMessage(), handles.keySet()));
				}
			}
		}
		return handles;
	}

	/**
	 * Compares requested handles with those already stored for the UIN: leftover hashes are
	 * marked {@code DELETE}, matching hashes are dropped from the returned map so they are
	 * not re-inserted, and {@code selectedHandles} on the request identity is rewritten.
	 *
	 * @param request identity update request
	 * @param uinObject persisted identity
	 * @param method {@link IdRepoConstants#UPDATE}
	 * @return new handles to insert (existing matching hashes removed)
	 * @throws IdRepoAppException on uniqueness failure or identity conversion errors
	 */
	private Map<String, List<HandleDto>> getNewAndDeleteExistingHandles(IdRequestDTO request, Uin uinObject,
			String method) throws IdRepoAppException {
		RequestDTO existingIdentityRequestDto = new RequestDTO();
		existingIdentityRequestDto.setIdentity(convertToObject(uinObject.getUinData(), ObjectNode.class));
		Map<String, List<HandleDto>> existingSelectedHandlesMap = idRepoServiceHelper
				.getSelectedHandles(existingIdentityRequestDto, null);
		Map<String, List<HandleDto>> newSelectedHandlesMap = checkAndGetHandles(request, uinObject.getUinHash(),
				existingSelectedHandlesMap, method);
		if (newSelectedHandlesMap == null) {
			newSelectedHandlesMap = new HashMap<>();
		}

		if (existingSelectedHandlesMap != null && !existingSelectedHandlesMap.isEmpty()) {
			for (Map.Entry<String, List<HandleDto>> existingEntry : existingSelectedHandlesMap.entrySet()) {
				List<HandleDto> requested = newSelectedHandlesMap.get(existingEntry.getKey());
				List<HandleDto> remainingNew = new ArrayList<>(requested == null ? List.of() : requested);
				for (HandleDto existingDto : existingEntry.getValue()) {
					Optional<HandleDto> matching = remainingNew.stream()
							.filter(h -> Objects.equals(h.getHandleHash(), existingDto.getHandleHash()))
							.findFirst();
					if (matching.isPresent()) {
						remainingNew.remove(matching.get());
					} else {
						handleRepo.updateStatusByHandleHash(existingDto.getHandleHash(),
								HandleStatusLifecycle.DELETE.name());
					}
				}
				if (remainingNew.isEmpty()) {
					newSelectedHandlesMap.remove(existingEntry.getKey());
				} else {
					newSelectedHandlesMap.put(existingEntry.getKey(), remainingNew);
				}
			}
		}

		Map<String, Object> identityObjectMap = mapper.convertValue(request.getRequest().getIdentity(),
				new TypeReference<Map<String, Object>>() {
				});
		idRepoServiceHelper.updateSelectedHandleFields(identityObjectMap, newSelectedHandlesMap);
		request.getRequest().setIdentity(identityObjectMap);
		return newSelectedHandlesMap;
	}

	/**
	 * Saves one {@code handle} row per handle DTO, linked to the identity by UIN hash.
	 * <p>
	 * The stored value is {@code saltId + SPLITTER + base64(handle) + SPLITTER + encryptSalt},
	 * with the salt chosen from the handle by the same modulo rule as the UIN. The row id is a
	 * UUID derived from the handle and the current time. Status is {@code ACTIVATED}.
	 * </p>
	 *
	 * @param uinEntity identity the handles belong to
	 * @param handles   handles from {@link #checkAndGetHandles}; nothing is saved when null or empty
	 */
	private void addIdentityHandle(Uin uinEntity, Map<String, List<HandleDto>> handles) {
		if (handles == null || handles.isEmpty()) {
			return;
		}
		for (List<HandleDto> handleDtos : handles.values()) {
			if (handleDtos == null) {
				continue;
			}
			for (HandleDto handleDto : handleDtos) {
				if (handleRepo.findByHandleHash(handleDto.getHandleHash()).isPresent()) {
					throw new IdRepoAppUncheckedException(HANDLE_RECORD_EXISTS.getErrorCode(),
							String.format(HANDLE_RECORD_EXISTS.getErrorMessage(), handleDto.getHandle()));
				}
				int saltId = securityManager.getSaltKeyForHashOfId(handleDto.getHandle());
				String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);

				String encodedHandleValue = CryptoUtil.encodeToPlainBase64(handleDto.getHandle().getBytes());
				String handleToEncrypt = saltId + SPLITTER + encodedHandleValue + SPLITTER + encryptSalt;

				Handle handleEntity = new Handle();
				handleEntity.setHandleHash(handleDto.getHandleHash());
				handleEntity.setId(UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID,
						handleDto.getHandle() + SPLITTER + DateUtils2.getUTCCurrentDateTime()).toString());
				handleEntity.setHandle(handleToEncrypt);
				handleEntity.setUinHash(uinEntity.getUinHash());
				handleEntity.setStatus(HandleStatusLifecycle.ACTIVATED.name());
				handleEntity.setCreatedBy(IdRepoSecurityManager.getUser());
				handleEntity.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
				handleRepo.save(handleEntity);
				mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY_HANDLE,
						"Record successfully saved in db");
			}
		}
	}

	/**
	 * Builds a new object-store file name for a document: a random UUID plus the file format
	 * from the identity JSON (for example {@code 3f2a....cbeff}).
	 *
	 * @param docType the document's node in the identity JSON; must contain {@code format}
	 * @return the new file reference id
	 */
	private String getFileRefId(JsonNode docType) {
		return UUID.randomUUID().toString() + DOT + docType.get(FILE_FORMAT_ATTRIBUTE).asText();
	}

	/**
	 * A document file waiting to be written to the object store. Files are collected while
	 * metadata rows are built and written afterwards, so a bad document fails before any upload.
	 *
	 * @param bio       {@code true} for a biometric (CBEFF) file, {@code false} for a demographic one
	 * @param fileRefId object name under the identity's folder
	 * @param data      decoded file content
	 * @param doc       the request document, used to report which document failed
	 * @param docType   the document's node in the identity JSON
	 * @param index     position of the document in the request, used in error messages
	 */
	private record PendingObjectStorePut(boolean bio, String fileRefId, byte[] data, DocumentsDTO doc,
			JsonNode docType, int index) {
	}
}