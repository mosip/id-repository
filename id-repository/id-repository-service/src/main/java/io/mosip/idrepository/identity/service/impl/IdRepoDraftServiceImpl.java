package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CREATE_DRAFT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREATE_IDENTITY_ENABLE_FORCE_MERGE;
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
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.dto.DraftUinResponseDto;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
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
import io.mosip.idrepository.identity.helper.VidDraftHelper;
import io.mosip.idrepository.identity.repository.UinBiometricDraftRepo;
import io.mosip.idrepository.identity.repository.UinBiometricRepo;
import io.mosip.idrepository.identity.repository.UinDocumentDraftRepo;
import io.mosip.idrepository.identity.repository.UinDocumentRepo;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.idrepository.manager.CredentialStatusManager;
import io.mosip.idrepository.pipeline.PendingCredentialSync;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.StringUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
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
 * {@code discardDraftV2}): draft files are written under {@code _draft/{ridHash}} by
 * {@link ObjectStoreHelper} and moved to the live path only on publish. V2 also
 * supports LOST packets, whose UIN is stamped after ABIS deduplication.</li>
 * </ul>
 * V2 publish, create and discard run without a surrounding transaction
 * ({@code NOT_SUPPORTED}) and open short {@link TransactionTemplate} transactions only
 * around database work, so no database connection is held during object-store calls.
 */
@Service
@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
public class IdRepoDraftServiceImpl extends IdRepoServiceImpl implements IdRepoDraftService<IdRequestDTO, IdResponseDTO> {

	/** Logger for draft operations; also receives the per-method performance log. */
	private static final Logger idrepoDraftLogger = IdRepoLogger.getLogger(IdRepoDraftServiceImpl.class);

	/** Separator of the excluded-attribute list property. */
	private static final String COMMA = ",";

	/**
	 * Identity attributes left out of {@link #getDraftUin} attribute lists when
	 * {@code EXCLUDED_ATTRIBUTE_LIST} is not configured.
	 */
	private static final String DEFAULT_ATTRIBUTE_LIST = "UIN,verifiedAttributes,IDSchemaVersion";

	@Value("${" + MOSIP_KERNEL_IDREPO_JSON_PATH + "}")
	/**
	 * JSON path of the UIN inside the identity object (for example {@code identity.UIN});
	 * used to build the minimal identity object of a NEW draft.
	 */
	private String uinPath;

	@Value("${" + UIN_REFID + "}")
	/** Key reference id used to encrypt and decrypt the UIN with its salt. */
	private String uinRefId;

	@Autowired
	/** Repository of {@code uin_draft}. */
	private UinDraftRepo uinDraftRepo;

	/** Transaction manager behind the programmatic transactions of publish and the V2 flows. */
	@Autowired
	private PlatformTransactionManager transactionManager;

	/**
	 * Runs credential issuance queued during an identity transaction once that transaction
	 * has committed. Lazy to break a bean cycle.
	 */
	@Autowired
	@Lazy
	private CredentialStatusManager credentialStatusManager;

	@Autowired
	/** Validates the identity rebuilt from a draft before it is published. */
	private IdRequestValidator validator;

	@Autowired
	/** Builds the REST request to the UIN generator service. */
	private RestRequestBuilder restBuilder;

	@Autowired
	/** Sends the REST request to the UIN generator service. */
	private RestHelper restHelper;

	@Autowired
	/** Repository of live {@code uin_biometric} rows, written when a draft is published. */
	private UinBiometricRepo uinBiometricRepo;

	@Autowired
	/** Repository of live {@code uin_document} rows, written when a draft is published. */
	private UinDocumentRepo uinDocumentRepo;

	@Autowired
	/** Repository of {@code uin_biometric_draft}; used to delete a V2 draft's biometric rows. */
	private UinBiometricDraftRepo uinBiometricDraftRepo;

	@Autowired
	/** Repository of {@code uin_document_draft}; used to delete a V2 draft's document rows. */
	private UinDocumentDraftRepo uinDocumentDraftRepo;
	
	@Autowired
	/**
	 * Proxy service used to read an identity by registration id and to build combined
	 * CBEFFs with extracted templates.
	 */
	private IdRepoProxyServiceImpl proxyService;
	
	@Autowired
	/** Creates and activates the draft VID issued when a draft publishes a new identity. */
	private VidDraftHelper vidDraftHelper;

	@Autowired
	/** Source of the excluded-attribute list of {@link #getDraftUin}. */
	private Environment environment;
	
	@Value("${" + CREATE_IDENTITY_ENABLE_FORCE_MERGE + ":false}")
	/**
	 * When {@code true}, a V1 draft is created even if the registration id was already
	 * processed, and its UIN is taken from the identity of that registration id.
	 */
	private boolean isForceMergeEnabled;
	
	@Override
	/**
	 * Creates a V1 draft. With {@code uin}, the draft is copied from that live identity;
	 * without, a new UIN is generated and a minimal identity object is stored. Draft files
	 * use the UIN hash path.
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN, or {@code null} for a new identity
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code RECORD_EXISTS} when the registration id already has a
	 *         draft or was already processed (unless force merge is enabled);
	 *         {@code NO_RECORD_FOUND} when {@code uin} does not exist;
	 *         {@code UIN_GENERATION_FAILED} when a UIN cannot be generated;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public IdResponseDTO createDraft(String registrationId, String uin) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				UinDraft newDraft;

				if (isForceMergeEnabled || (!super.uinHistoryRepo.existsByRegId(registrationId)
						&& !uinDraftRepo.existsByRegId(registrationId))) {
					if (isForceMergeEnabled) {
						IdResponseDTO response = proxyService.retrieveIdentityByRid(registrationId, uin, null);
						Object res = response.getResponse().getIdentity();
						LinkedHashMap<String, Object> map = mapper.convertValue(res, LinkedHashMap.class);
						uin = String.valueOf(map.get("UIN"));
					}
					if (Objects.nonNull(uin)) {
						Optional<Uin> uinObjectOptional = super.uinRepo.findByUinHash(super.getUinHash(uin));
						if (uinObjectOptional.isPresent()) {
							Uin uinObject = uinObjectOptional.get();
							newDraft = mapper.convertValue(uinObject, UinDraft.class);
							updateBiometricAndDocumentDrafts(registrationId, newDraft, uinObject);
							newDraft.setRegId(registrationId);
							newDraft.setUin(super.getUinToEncrypt(uin));
						} else {
							idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL,
									CREATE_DRAFT, "UIN NOT EXIST");
							throw new IdRepoAppException(NO_RECORD_FOUND);
						}
					} else {
						newDraft = new UinDraft();
						uin = generateUin();
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
				} else {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
							"RID ALREADY EXIST");
					throw new IdRepoAppException(RECORD_EXISTS);
				}
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT, e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			}
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT, startNanos);
		}
	}

	/**
	 * Wraps {@code uin} in nested maps following {@link #uinPath}, so a path of
	 * {@code identity.UIN} gives {@code {"identity": {"UIN": uin}}}. The root segment is skipped.
	 *
	 * @param uin value to place at the UIN path
	 * @return nested map holding {@code uin}
	 */
	private Object generateIdentityObject(Object uin) {
		List<String> pathList = new ArrayList<>(Arrays.asList(uinPath.split("\\.")));
		pathList.remove(ROOT_PATH);
		Collections.reverse(pathList);
		for (String string : pathList) {
			uin = new HashMap<>(Map.of(string, uin));
		}
		return uin;
	}

	/**
	 * Allocates a new UIN from the kernel UIN generator service.
	 *
	 * @return the allocated UIN
	 * @throws IdRepoAppException {@code UNKNOWN_ERROR} when the request cannot be built;
	 *         {@code UIN_GENERATION_FAILED} when the service call fails
	 */
	private String generateUin() throws IdRepoAppException {
		try {
			RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.UIN_GENERATOR_SERVICE, null,
					ResponseWrapper.class);
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(restRequest);
			return response.getResponse().get("uin");
		} catch (IdRepoDataValidationException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GENERATE_UIN, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (RestServiceException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GENERATE_UIN, e.getMessage());
			throw new IdRepoAppException(UIN_GENERATION_FAILED, e);
		}
	}

	@Override
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
	public IdResponseDTO updateDraft(String registrationId, IdRequestDTO request) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(registrationId);
				if (uinDraft.isPresent()) {
					UinDraft draftToUpdate = uinDraft.get();
					if (Objects.isNull(draftToUpdate.getUinData())) {
						ObjectNode identityObject = mapper.convertValue(request.getRequest().getIdentity(), ObjectNode.class);
						identityObject.putPOJO(VERIFIED_ATTRIBUTES, request.getRequest().getVerifiedAttributes());
						byte[] uinData = super.convertToBytes(request.getRequest().getIdentity());
						draftToUpdate.setUinData(uinData);
						draftToUpdate.setUinDataHash(securityManager.hash(uinData));
						updateDocuments(request.getRequest(), draftToUpdate);
						draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
						draftToUpdate.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
						uinDraftRepo.save(draftToUpdate);
					} else {
						updateDemographicData(request, draftToUpdate);
						updateDocuments(request.getRequest(), draftToUpdate);

						uinDraftRepo.save(draftToUpdate);
					}
				} else {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
							"RID NOT FOUND IN DB");
					throw new IdRepoAppException(NO_RECORD_FOUND);
				}
			} catch (JSONException | IOException | InvalidJsonException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
				throw new IdRepoAppException(UNKNOWN_ERROR, e);
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			}
			return constructIdResponse(null, DRAFTED, null, null);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, startNanos);
		}
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
	private void updateDemographicData(IdRequestDTO request, UinDraft draftToUpdate) throws JSONException, IdRepoAppException, IOException {
		if (Objects.nonNull(request.getRequest()) && Objects.nonNull(request.getRequest().getIdentity())) {
			RequestDTO requestDTO = request.getRequest();
			Configuration configuration = Configuration.builder().jsonProvider(new JacksonJsonProvider()).mappingProvider(new JacksonMappingProvider()).build();
			DocumentContext inputData = JsonPath.using(configuration).parse(requestDTO.getIdentity());
			DocumentContext dbData = JsonPath.using(configuration).parse(new String(draftToUpdate.getUinData()));
			JsonPath uinJsonPath = JsonPath.compile(uinPath.replace(ROOT_PATH, "$"));
			super.updateVerifiedAttributes(requestDTO, inputData, dbData);
			JSONCompareResult comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(),
					JSONCompareMode.LENIENT);
			if (comparisonResult.failed()) {
				// LOST packet (v2): uinData may exist before the UIN is stamped via updateDraftUinData.
				if (draftToUpdate.getUinHash() == null) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
							"UIN details not found in draft | regId=" + draftToUpdate.getRegId());
					throw new IdRepoAppException(DRAFT_UIN_DETAILS_NOT_FOUND);
				}
				super.updateJsonObject(draftToUpdate.getUinHash(), inputData, dbData, comparisonResult, false);
			}
			draftToUpdate.setUinData(convertToBytes(convertToObject(dbData.jsonString().getBytes(), Map.class)));
			draftToUpdate.setUinDataHash(securityManager.hash(draftToUpdate.getUinData()));
			draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
			draftToUpdate.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
		}
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
		if (Objects.nonNull(requestDTO.getDocuments()) && !requestDTO.getDocuments().isEmpty()) {
			Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
			String uinHashWithSalt = draftToUpdate.getUinHash().split(SPLITTER)[1];
			super.updateDocuments(uinHashWithSalt, uinObject, requestDTO, true);
			updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
		}
	}

	/**
	 * Copies the file rows of {@code uinObject} onto the draft. A draft row of the same
	 * biometric file type or document category is updated in place when its file id
	 * changed; rows with no match are added. Every draft row is then tagged with
	 * {@code regId}. Matched rows are removed from {@code uinObject}'s lists as a side effect.
	 *
	 * @param regId         registration id of the draft
	 * @param draftToUpdate draft whose biometric and document rows are updated
	 * @param uinObject     identity (or draft mapped to {@link Uin}) holding the new file rows
	 */
	private void updateBiometricAndDocumentDrafts(String regId, UinDraft draftToUpdate, Uin uinObject) {
		List<UinBiometric> uinBiometrics = new ArrayList<>(uinObject.getBiometrics());
		IntStream.range(0, uinBiometrics.size()).forEach(index -> {
			UinBiometric uinBio = uinBiometrics.get(index);
			Optional<UinBiometricDraft> draftBioRecord = draftToUpdate.getBiometrics().stream()
					.filter(draftBio -> uinBio.getBiometricFileType().contentEquals(draftBio.getBiometricFileType())).findFirst();
			if (draftBioRecord.isPresent()) {
				UinBiometricDraft draftBio = draftBioRecord.get();
				if (!uinBio.getBioFileId().contentEquals(draftBio.getBioFileId())) {
					draftBio.setRegId(regId);
					draftBio.setBioFileId(uinBio.getBioFileId());
					draftBio.setBiometricFileName(uinBio.getBiometricFileName());
					draftBio.setBiometricFileHash(uinBio.getBiometricFileHash());
					draftBio.setUpdatedBy(IdRepoSecurityManager.getUser());
					draftBio.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
				}
				ListIterator<UinBiometric> listIterator = uinObject.getBiometrics().listIterator();
				while (listIterator.hasNext()) {
					if (listIterator.next().getBioFileId().contentEquals(draftBio.getBioFileId()))
						listIterator.remove();
				}
			}
		});

		List<UinDocument> uinDocuments = new ArrayList<>(uinObject.getDocuments());
		IntStream.range(0, uinDocuments.size()).forEach(index -> {
			UinDocument uinDoc = uinDocuments.get(index);
			Optional<UinDocumentDraft> draftDocRecord = draftToUpdate.getDocuments().stream()
					.filter(draftDoc -> uinDoc.getDoccatCode().contentEquals(draftDoc.getDoccatCode())).findFirst();
			if (draftDocRecord.isPresent()) {
				UinDocumentDraft draftDoc = draftDocRecord.get();
				if (!uinDoc.getDocId().contentEquals(draftDoc.getDocId())) {
					draftDoc.setRegId(regId);
					draftDoc.setDocId(uinDoc.getDocId());
					draftDoc.setDoctypCode(uinDoc.getDoctypCode());
					draftDoc.setDocName(uinDoc.getDocName());
					draftDoc.setDocfmtCode(uinDoc.getDocfmtCode());
					draftDoc.setDocHash(uinDoc.getDocHash());
					draftDoc.setUpdatedBy(IdRepoSecurityManager.getUser());
					draftDoc.setUpdatedDateTime(uinDoc.getUpdatedDateTime());
				}
				ListIterator<UinDocument> listIterator = uinObject.getDocuments().listIterator();
				while (listIterator.hasNext()) {
					if (listIterator.next().getDocId().contentEquals(draftDoc.getDocId()))
						listIterator.remove();
				}
			}
		});

		List<UinBiometricDraft> bioDraftList = mapper.convertValue(uinObject.getBiometrics(),
				new TypeReference<List<UinBiometricDraft>>() {
				});
		List<UinDocumentDraft> docDraftList = mapper.convertValue(uinObject.getDocuments(),
				new TypeReference<List<UinDocumentDraft>>() {
				});
		draftToUpdate.getBiometrics().addAll(bioDraftList);
		draftToUpdate.getDocuments().addAll(docDraftList);
		draftToUpdate.getBiometrics().forEach(bio -> bio.setRegId(regId));
		draftToUpdate.getDocuments().forEach(doc -> doc.setRegId(regId));
	}

	@Override
	/**
	 * Publishes a V1 draft as the live identity.
	 * <p>
	 * Runs DB work in a dedicated transaction, then sync credentials <em>after</em> that TX
	 * releases its JDBC connection (same pool-hold fix as add/update identity).
	 * </p>
	 *
	 * @param regId registration id of the draft
	 * @return response with the live identity status and, for a new identity, the draft VID
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         a validation error when the draft identity is invalid;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO publishDraft(String regId) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try {
			return tx.execute(status -> {
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
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT, startNanos);
		}
	}

	/**
	 * Body of the V1 publish; must run inside the transaction opened by {@link #publishDraft}.
	 * Updates the identity when it already exists, or adds it and activates a draft VID when
	 * it is new; then saves the anonymous profile, publishes the file rows and deletes the draft.
	 *
	 * @param regId registration id of the draft
	 * @return response with the live identity status and, for a new identity, the draft VID
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	private IdResponseDTO publishDraftInTransaction(String regId) throws IdRepoAppException {
		anonymousProfileHelper.setRegId(regId);
		try {
			String draftVid = null;
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
			if (uinDraft.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT,
						DRAFT_RECORD_NOT_FOUND);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			} else {
				UinDraft draft = uinDraft.get();
				anonymousProfileHelper
				.setNewCbeff(draft.getUinHash().split("_")[1],
						!anonymousProfileHelper.isNewCbeffPresent() && Objects.nonNull(draft.getBiometrics())
						&& !draft.getBiometrics().isEmpty()
						? draft.getBiometrics().get(draft.getBiometrics().size() - 1).getBioFileId()
								: null);
				IdRequestDTO idRequest = buildRequest(regId, draft);
				validateRequest(idRequest.getRequest());
				String uin = decryptUin(draft.getUin(), draft.getUinHash());
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
				this.discardDraft(regId);
				return constructIdResponse(null, uinObject.getStatusCode(), null, draftVid);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Rebuilds an identity request from a draft so it can go through the normal add or
	 * update identity path. Verified attributes are moved from the identity JSON to their
	 * own request field.
	 *
	 * @param regId registration id set on the request
	 * @param draft draft whose identity data is used
	 * @return identity request without documents
	 */
	@SuppressWarnings("unchecked")
	private IdRequestDTO buildRequest(String regId, UinDraft draft) {
		IdRequestDTO idRequest = new IdRequestDTO();
		RequestDTO request = new RequestDTO();
		request.setRegistrationId(regId);
		Map<String, Object> identityData = convertToObject(draft.getUinData(), Map.class);
		request.setVerifiedAttributes(
				mapper.convertValue(identityData.get(VERIFIED_ATTRIBUTES), new TypeReference<List<String>>() {
				}));
		identityData.remove(VERIFIED_ATTRIBUTES);
		request.setIdentity(identityData);
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
	 * {@code uin_document} rows of the published identity.
	 *
	 * @param draft     draft whose file rows are published
	 * @param uinObject live identity the rows are attached to
	 */
	private void publishDocuments(UinDraft draft, final Uin uinObject) {
		List<UinBiometric> uinBiometricList = draft.getBiometrics().stream().map(bio -> {
			UinBiometric uinBio = mapper.convertValue(bio, UinBiometric.class);
			uinBio.setUinRefId(uinObject.getUinRefId());
			uinBio.setLangCode("");
			return uinBio;
		}).collect(Collectors.toList());
		uinBiometricRepo.saveAll(uinBiometricList);
		List<UinDocument> uinDocumentList = draft.getDocuments().stream().map(doc -> {
			UinDocument uinDoc = mapper.convertValue(doc, UinDocument.class);
			uinDoc.setUinRefId(uinObject.getUinRefId());
			uinDoc.setLangCode("");
			return uinDoc;
		}).collect(Collectors.toList());
		uinDocumentRepo.saveAll(uinDocumentList);
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
		String salt = uinEncryptSaltRepo.retrieveSaltById(
				Integer.parseInt(encryptedUin.split(SPLITTER)[0]));
		String uin = new String(securityManager.decryptWithSalt(
				CryptoUtil.decodeURLSafeBase64(StringUtils.substringAfter(encryptedUin, SPLITTER)),
				CryptoUtil.decodePlainBase64(salt), uinRefId));
		if (!StringUtils.equals(super.getUinHash(uin), uinHash)) {
			throw new IdRepoAppUncheckedException(UIN_HASH_MISMATCH);
		}
		return uin;
	}

	@Override
	/**
	 * Discards a V1 draft by deleting its draft row (biometric and document draft rows are
	 * removed by cascade). Files are not deleted.
	 *
	 * @param regId registration id of the draft
	 * @return response with status {@code DISCARDED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	public IdResponseDTO discardDraft(String regId) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				Optional<UinDraft> draftOptional = uinDraftRepo.findByRegId(regId);
				if (draftOptional.isPresent()) {
					uinDraftRepo.deleteByRegId(regId);
					return constructIdResponse(null, "DISCARDED", null, null);
				} else {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
							"RID NOT FOUND IN DB");
					throw new IdRepoAppException(NO_RECORD_FOUND);
				}
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, DISCARD_DRAFT,
						e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			}
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, DISCARD_DRAFT, startNanos);
		}
	}

	@Override
	/**
	 * Reports whether a draft exists for a registration id (V1 or V2).
	 *
	 * @param regId registration id
	 * @return {@code true} when a draft row exists
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on lookup failure
	 */
	public boolean hasDraft(String regId) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				return uinDraftRepo.existsByRegId(regId);
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, "hasDraft", e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			}
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "hasDraft", startNanos);
		}
	}

	@Override
	/**
	 * Reads a V1 draft with all its identity data, biometrics and documents. Files are read
	 * from the UIN hash path; biometrics are returned as combined CBEFFs with templates in
	 * the requested formats.
	 *
	 * @param regId             registration id of the draft
	 * @param extractionFormats modality key to SDK format; may be empty
	 * @return draft identity data and Base64URL-encoded files
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         a file read or extraction error; {@code DATABASE_ACCESS_ERROR} on lookup failure
	 */
	public IdResponseDTO getDraft(String regId, Map<String, String> extractionFormats) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
				if (uinDraft.isPresent()) {
					UinDraft draft = uinDraft.get();
					List<DocumentsDTO> documents = new ArrayList<>();
					String uinHash = draft.getUinHash().split(SPLITTER)[1];
					for (UinBiometricDraft uinBiometricDraft : draft.getBiometrics()) {
						documents.add(new DocumentsDTO(uinBiometricDraft.getBiometricFileType(), CryptoUtil.encodeToURLSafeBase64(
								extractAndGetCombinedCbeff(uinHash, uinBiometricDraft.getBioFileId(), extractionFormats))));
					}
					for (UinDocumentDraft uinDocumentDraft : draft.getDocuments()) {
						documents.add(new DocumentsDTO(uinDocumentDraft.getDoccatCode(), CryptoUtil
								.encodeToURLSafeBase64(objectStoreHelper.getDemographicObject(uinHash, uinDocumentDraft.getDocId()))));
					}
					return constructIdResponse(draft.getUinData(), draft.getStatusCode(), documents, null);
				} else {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT,
							DRAFT_RECORD_NOT_FOUND);
					throw new IdRepoAppException(NO_RECORD_FOUND);
				}
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			}
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, startNanos);
		}
	}

	@Override
	/**
	 * Re-extracts biometric templates for a V1 draft under the UIN hash path. Does nothing
	 * when {@code extractionFormats} is empty.
	 *
	 * @param registrationId    registration id of the draft
	 * @param extractionFormats modality key to SDK format
	 * @return response with status {@code DRAFTED}
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code BIO_EXTRACTION_ERROR} when extraction fails;
	 *         {@code DATABASE_ACCESS_ERROR} on lookup failure
	 */
	public IdResponseDTO extractBiometrics(String registrationId, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			if (!extractionFormats.isEmpty())
				try {
					Optional<UinDraft> draftOpt = uinDraftRepo.findByRegId(registrationId);
					if (draftOpt.isPresent()) {
						extractBiometricsDraft(extractionFormats, draftOpt.get());
					} else {
						idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT,
								DRAFT_RECORD_NOT_FOUND);
						throw new IdRepoAppException(NO_RECORD_FOUND);
					}
				} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
					idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT,
							e.getMessage());
					throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
				}
			return constructIdResponse(null, DRAFTED, null, null);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "extractBiometrics",
					startNanos);
		}
	}

	/**
	 * V1: for each biometric file of the draft, deletes previously extracted files of the
	 * requested formats and extracts them again under the UIN hash path.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param draft             draft whose biometric files are processed
	 * @throws IdRepoAppException {@code BIO_EXTRACTION_ERROR} for any failure
	 */
	private void extractBiometricsDraft(Map<String, String> extractionFormats, UinDraft draft)
			throws IdRepoAppException {
		try {
			String uinHash = draft.getUinHash().split("_")[1];
			for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
				deleteExistingExtractedBioData(extractionFormats, uinHash, bioDraft);
				extractAndGetCombinedCbeff(uinHash, bioDraft.getBioFileId(), extractionFormats);
			}
		} catch (Exception e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	/**
	 * Deletes the live-path extracted files of one biometric file for every requested
	 * format, so the next extraction regenerates them.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param uinHash           uin hash (without salt id)
	 * @param bioDraft          biometric draft row whose extracted files are deleted
	 */
	private void deleteExistingExtractedBioData(Map<String, String> extractionFormats, String uinHash, UinBiometricDraft bioDraft) {
		extractionFormats.entrySet()
				.forEach(extractionFormat -> super.objectStoreHelper.deleteBiometricObject(uinHash,
						buildExtractionFileName(extractionFormat, bioDraft.getBioFileId())));
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
	private byte[] extractAndGetCombinedCbeff(String uinHash, String bioFileId, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormats(uinHash, bioFileId, extractionFormats,
				super.objectStoreHelper.getBiometricObject(uinHash, bioFileId));
	}

	/**
	 * Builds the name of an extracted template file, matching the name used by
	 * {@code BiometricExtractionServiceImpl}.
	 *
	 * @param extractionFormat modality key (for example {@code fingerExtractionFormat}) and format
	 * @param bioFileId        file id of the source CBEFF
	 * @return {@code {bioFileId without extension}.{modality}.{format}}
	 */
	private String buildExtractionFileName(Entry<String, String> extractionFormat, String bioFileId) {
		return bioFileId.split("\\.")[0].concat(DOT).concat(getModalityForFormat(extractionFormat.getKey())).concat(DOT)
				.concat(extractionFormat.getValue());
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
	 * Builds a draft API response.
	 *
	 * @param uinData   identity JSON to return, or {@code null}; verified attributes are moved
	 *                  to their own response field
	 * @param status    response status, for example {@code DRAFTED}
	 * @param documents files to return, or {@code null}
	 * @param vid       draft VID to return in the metadata, or {@code null}
	 * @return the response
	 */
	@SuppressWarnings("unchecked")
	private IdResponseDTO constructIdResponse(byte[] uinData, String status, List<DocumentsDTO> documents, String vid) {
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus(status);
		if (Objects.nonNull(documents))
			response.setDocuments(documents);
		if (Objects.nonNull(uinData)) {
			ObjectNode identityObject = convertToObject(uinData, ObjectNode.class);
			response.setIdentity(identityObject);
			response.setVerifiedAttributes(mapper.convertValue(identityObject.get(VERIFIED_ATTRIBUTES), List.class));
			identityObject.remove(VERIFIED_ATTRIBUTES);
		}
		idResponse.setResponse(response);
		if(Objects.nonNull(vid))
			idResponse.setMetadata(Map.of("vid", vid));
		return idResponse;
	}

	@Override
	/**
	 * Looks up the draft held for a UIN and lists the identity attributes it contains,
	 * leaving out the configured excluded attributes.
	 *
	 * @param uin UIN of the identity
	 * @return response with the draft's registration id, creation time and attributes;
	 *         no drafts when none exists
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on lookup or JSON failure
	 */
	public DraftResponseDto getDraftUin(String uin) throws IdRepoAppException{
		long startNanos = System.nanoTime();
		try {
			String uinHash = super.getUinHash(uin);
			DraftResponseDto draftResponseDto = new DraftResponseDto();
			try {
				UinDraft uinDraft = uinDraftRepo.findByUinHash(uinHash);
				DraftUinResponseDto draftUinResponseDto = new DraftUinResponseDto();
				if (uinDraft!=null) {
					draftUinResponseDto.setRid(uinDraft.getRegId());
					draftUinResponseDto.setCreatedDTimes(uinDraft.getCreatedDateTime().toString());
					draftUinResponseDto.setAttributes(getAttributeListFromUinData(uinDraft.getUinData()));
					draftResponseDto.setDrafts(List.of(draftUinResponseDto));
				}
			} catch (DataAccessException | TransactionException | JDBCConnectionException | JsonProcessingException e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, e.getMessage());
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			}
			return draftResponseDto;
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "getDraftUin", startNanos);
		}
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
		List<String> attributeList = new ArrayList<>();
		String resultString = new String(uinData, StandardCharsets.UTF_8);
		String excludedAttributeListProperty = environment.getProperty(EXCLUDED_ATTRIBUTE_LIST, DEFAULT_ATTRIBUTE_LIST);
		List<String> excludedListPropertyList = List.of(excludedAttributeListProperty.split(COMMA));
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(resultString);
		jsonNode.fieldNames().forEachRemaining(key -> {
			if(!excludedListPropertyList.contains(key)){
				attributeList.add(key);
			}
		});
		return attributeList;
	}

	// ---- V2 draft APIs: object-store files live under _draft/{ridHash}/ until publish ----

	/**
	 * Creates a programmatic transaction on the shared transaction manager. V2 methods use
	 * it instead of {@code @Transactional} so object-store calls stay outside transactions.
	 *
	 * @return new transaction template with default propagation and isolation
	 */
	private TransactionTemplate newTransactionTemplate() {
		return new TransactionTemplate(transactionManager);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A LOST packet ({@code generateUin=false} and no {@code uin}) is handled by
	 * {@link #createLostDraft}; every other case by {@link #createDraftWithUin}. Runs with
	 * {@code NOT_SUPPORTED} so no transaction is held while live files are copied.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO createDraftV2(String registrationId, String uin, boolean generateUin)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			if (!generateUin && uin == null) {
				return createLostDraft(registrationId);
			}
			return createDraftWithUin(registrationId, uin);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "createDraftV2", startNanos);
		}
	}

	/**
	 * Creates a draft with no UIN, identity data or files for a LOST packet. The UIN is
	 * stamped later by {@link #updateDraftUinData}.
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
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
						"RID ALREADY EXIST | regId=" + registrationId);
				throw new IdRepoAppException(RECORD_EXISTS);
			}
			// A RID in UIN history was already processed; reprocessing is allowed only if it is still the latest.
			if (super.uinHistoryRepo.existsByRegId(registrationId) && !super.uinRepo.existsByRegId(registrationId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
						"REQUESTED RID IS OLDER THAN THE LATEST PROCESSED RID | regId=" + registrationId);
				throw new IdRepoAppException(RID_OLDER_THAN_LATEST_PROCESSED);
			}
			UinDraft newDraft = new UinDraft();
			newDraft.setRegId(registrationId);
			newDraft.setStatusCode("DRAFT");
			newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
			newDraft.setCreatedDateTime(DateUtils2.getUTCCurrentDateTime());
			uinDraftRepo.save(newDraft);
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT, e.getMessage());
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
	 *         {@code DRAFT_OBJECT_COPY_FAILED} when a live file cannot be copied;
	 *         {@code DATABASE_ACCESS_ERROR} on persistence failure
	 */
	private IdResponseDTO createDraftWithUin(String registrationId, String uin) throws IdRepoAppException {
		try {
			UinDraft newDraft = newTransactionTemplate().execute(status -> {
				try {
					return getDraftForExistingUin(registrationId, uin);
				} catch (IdRepoAppException e) {
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			});
			if (newDraft != null) {
				copyLiveFilesToDraft(registrationId, newDraft);
			} else {
				newDraft = buildNewIdentityDraft(registrationId);
			}
			uinDraftRepo.save(newDraft);
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Builds a draft from the live identity for an UPDATE packet (by {@code uin}) or for a
	 * reprocessed packet (by registration id, when it is still the latest applied to the
	 * identity). Returns {@code null} for a first-time NEW registration. Must run inside a
	 * transaction because the identity's biometric and document collections are lazy.
	 *
	 * @param registrationId registration id of the packet
	 * @param uin            existing UIN, or {@code null}
	 * @return unsaved draft copied from the live identity, or {@code null} when there is none
	 * @throws IdRepoAppException {@code RECORD_EXISTS} when a draft already exists;
	 *         {@code RID_OLDER_THAN_LATEST_PROCESSED} when a newer registration has been
	 *         applied; {@code NO_RECORD_FOUND} when the identity cannot be found
	 */
	private UinDraft getDraftForExistingUin(String registrationId, String uin) throws IdRepoAppException {
		if (uinDraftRepo.existsByRegId(registrationId)) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
					"RID ALREADY EXIST | regId=" + registrationId);
			throw new IdRepoAppException(RECORD_EXISTS);
		}
		Uin liveUin = null;
		if (Objects.isNull(uin) && super.uinHistoryRepo.existsByRegId(registrationId)) {
			if (!super.uinRepo.existsByRegId(registrationId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
						"REQUESTED RID IS OLDER THAN THE LATEST PROCESSED RID | regId=" + registrationId);
				throw new IdRepoAppException(RID_OLDER_THAN_LATEST_PROCESSED);
			}
			liveUin = loadLiveUin(super.uinRepo.findWithBiometricsByRegId(registrationId));
			if (liveUin == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
						"UIN NOT FOUND FOR REPROCESS | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			uin = decryptUin(liveUin.getUin(), liveUin.getUinHash());
		} else if (Objects.nonNull(uin)) {
			liveUin = loadLiveUin(super.uinRepo.findWithBiometricsByUinHash(super.getUinHash(uin)));
			if (liveUin == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, CREATE_DRAFT,
						"UIN NOT EXIST");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		}
		if (liveUin == null) {
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
	 * Unwraps a loaded identity and initializes its biometric and document collections
	 * so they can be used after the transaction ends.
	 *
	 * @param loaded result of a {@code UinRepo} lookup
	 * @return the identity with collections initialized, or {@code null} when empty
	 */
	private Uin loadLiveUin(Optional<Uin> loaded) {
		if (loaded.isEmpty()) {
			return null;
		}
		Hibernate.initialize(loaded.get().getBiometrics());
		Hibernate.initialize(loaded.get().getDocuments());
		return loaded.get();
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
		String uin = generateUin();
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
	 * Copies every live biometric and document file listed on the draft from
	 * {@code {uinHash}/} to {@code _draft/{ridHash}/}, so the V2 update and read paths find
	 * them. The live files are kept.
	 *
	 * @param registrationId registration id of the draft
	 * @param newDraft       draft built from the live identity
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_COPY_FAILED} when a file cannot be copied
	 */
	private void copyLiveFilesToDraft(String registrationId, UinDraft newDraft) throws IdRepoAppException {
		List<UinBiometricDraft> bios = newDraft.getBiometrics();
		List<UinDocumentDraft> docs = newDraft.getDocuments();
		if ((bios == null || bios.isEmpty()) && (docs == null || docs.isEmpty())) {
			return;
		}
		String ridHash = objectStoreHelper.getRidHash(registrationId);
		String livePrefix = newDraft.getUinHash().split(SPLITTER)[1];
		if (bios != null) {
			for (UinBiometricDraft bio : bios) {
				objectStoreHelper.copyBiometricLiveToDraft(livePrefix, ridHash, bio.getBioFileId());
			}
		}
		if (docs != null) {
			for (UinDocumentDraft doc : docs) {
				objectStoreHelper.copyDemographicLiveToDraft(livePrefix, ridHash, doc.getDocId());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The UIN is encrypted here because the entity interceptor encrypts it only on insert.
	 * The draft is flushed immediately so a unique-key clash (another draft already holds
	 * this UIN) is reported as {@code RECORD_EXISTS} instead of failing at commit.
	 */
	@Override
	public IdResponseDTO updateDraftUinData(String registrationId, String uin) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			UinDraft draft = uinDraftRepo.findByRegId(registrationId).orElse(null);
			if (draft == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
						"RID NOT FOUND IN DB | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			String uinHash = super.getUinHash(uin);
			Optional<Uin> liveUin = super.uinRepo.findByUinHash(uinHash);
			if (liveUin.isEmpty()) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
						"UIN NOT FOUND | regId=" + registrationId);
				throw new IdRepoAppException(UIN_NOT_FOUND);
			}
			if (draft.getUinHash() != null && !draft.getUinHash().equals(uinHash)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
						"ATTEMPT TO RESTAMP DRAFT WITH A DIFFERENT UIN | regId=" + registrationId);
				throw new IdRepoAppException(DRAFT_UIN_HASH_MISMATCH);
			}
			// The save interceptor encrypts uin only on insert; this is an update, so encrypt here.
			int saltId = securityManager.getSaltKeyForId(uin);
			String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
			byte[] encryptedUin = securityManager.encryptWithSalt(uin.getBytes(),
					CryptoUtil.decodePlainBase64(encryptSalt), uinRefId);
			draft.setUin(saltId + SPLITTER + new String(encryptedUin));
			draft.setUinHash(uinHash);
			backfillDraftIdentityFromLiveUin(draft, liveUin.get());
			draft.setUpdatedBy(IdRepoSecurityManager.getUser());
			draft.setUpdatedDateTime(DateUtils2.getUTCCurrentDateTime());
			// flush so a unique-key clash surfaces here rather than at commit
			uinDraftRepo.saveAndFlush(draft);
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataIntegrityViolationException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(RECORD_EXISTS, e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (JSONException | IOException | InvalidJsonException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "updateDraftUinData", startNanos);
		}
	}

	/**
	 * Fills the draft's identity JSON from the live identity. When the draft has no
	 * identity data, the live data is copied as is. Otherwise both are merged leniently:
	 * fields present in the draft keep the draft value, fields only in the live identity
	 * are added. The identity data hash is recomputed.
	 *
	 * @param draft   LOST draft being stamped with a UIN
	 * @param liveUin live identity of that UIN
	 * @throws JSONException      when the JSON cannot be compared
	 * @throws IOException        when the merged JSON cannot be serialized
	 * @throws IdRepoAppException when the merge fails
	 */
	private void backfillDraftIdentityFromLiveUin(UinDraft draft, Uin liveUin)
			throws JSONException, IOException, IdRepoAppException {
		if (Objects.isNull(liveUin.getUinData()) || Objects.isNull(draft.getUinData())) {
			if (Objects.isNull(draft.getUinData()) && Objects.nonNull(liveUin.getUinData())) {
				draft.setUinData(liveUin.getUinData());
				draft.setUinDataHash(securityManager.hash(draft.getUinData()));
			}
			return;
		}
		Configuration configuration = Configuration.builder().jsonProvider(new JacksonJsonProvider())
				.mappingProvider(new JacksonMappingProvider()).build();
		DocumentContext inputData = JsonPath.using(configuration).parse(new String(draft.getUinData()));
		DocumentContext dbData = JsonPath.using(configuration).parse(new String(liveUin.getUinData()));
		JSONCompareResult comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(),
				JSONCompareMode.LENIENT);
		if (comparisonResult.failed()) {
			super.updateJsonObject(draft.getUinHash(), inputData, dbData, comparisonResult, false);
		}
		draft.setUinData(convertToBytes(convertToObject(dbData.jsonString().getBytes(), Map.class)));
		draft.setUinDataHash(securityManager.hash(draft.getUinData()));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * When the draft has no identity data yet (a LOST draft), the request identity is
	 * stored as is; otherwise it is merged with the V1 demographic merge. Documents and
	 * biometrics are then written to the draft path by {@link #updateDocumentsV2}.
	 */
	@Override
	public IdResponseDTO updateDraftV2(String registrationId, IdRequestDTO request) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			UinDraft draftToUpdate = uinDraftRepo.findByRegId(registrationId).orElse(null);
			if (draftToUpdate == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT,
						"RID NOT FOUND IN DB | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
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
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (JSONException | IOException | InvalidJsonException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, UPDATE_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "updateDraftV2", startNanos);
		}
	}

	/**
	 * Writes the request's biometric and document files under {@code _draft/{ridHash}/}
	 * and updates the draft's file rows. Reuses
	 * {@link IdRepoServiceImpl#updateDocuments(String, Uin, RequestDTO, boolean, boolean)}
	 * with the draft object store selected. Does nothing when the request has no documents.
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
		// updateCbeff derives the object prefix from uinHash.split("_")[1]
		uinObject.setUinHash(SPLITTER + ridHash);
		super.updateDocuments(ridHash, uinObject, requestDTO, true, true);
		updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Steps, in order:
	 * <ol>
	 * <li>Load and validate the draft in a short transaction ({@link #loadDraftForPublish}).</li>
	 * <li>Move {@code _draft/{ridHash}} files to the live {@code {uinHash}} path, so a
	 * retry finds them in place.</li>
	 * <li>In a second transaction, update the identity when it already exists, or add it
	 * and activate a draft VID when it is new; then publish the document rows.</li>
	 * <li>Delete the draft rows and save the anonymous profile.</li>
	 * </ol>
	 * Runs with {@code NOT_SUPPORTED}. Pending credential syncs are cleared on failure, and
	 * the remaining ones run after the identity transaction has released its connection.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO publishDraftV2(String regId) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		anonymousProfileHelper.setRegId(regId);
		TransactionTemplate tx = newTransactionTemplate();
		try {
			UinDraft draft = tx.execute(status -> {
				try {
					return loadDraftForPublish(regId);
				} catch (IdRepoAppException e) {
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			});
			String lastBioFileId = Objects.nonNull(draft.getBiometrics()) && !draft.getBiometrics().isEmpty()
					? draft.getBiometrics().get(draft.getBiometrics().size() - 1).getBioFileId()
					: null;
			anonymousProfileHelper.setNewCbeff(draft.getUinHash().split(SPLITTER)[1],
					!anonymousProfileHelper.isNewCbeffPresent() ? lastBioFileId : null);

			IdRequestDTO idRequest = buildRequest(regId, draft);
			validateRequest(idRequest.getRequest());
			String uin = decryptUin(draft.getUin(), draft.getUinHash());

			String srcPrefix = objectStoreHelper.getRidHash(draft.getRegId());
			String destPrefix = draft.getUinHash().split(SPLITTER)[1];
			objectStoreHelper.moveAllDraftBiometricsToLive(srcPrefix, destPrefix);
			objectStoreHelper.moveAllDraftDemographicsToLive(srcPrefix, destPrefix);

			boolean liveIdentityExists = uinRepo.existsByUinHash(draft.getUinHash());
			String draftVid = liveIdentityExists ? null : vidDraftHelper.generateDraftVid(uin);
			Uin liveUin = tx.execute(status -> {
				try {
					Uin committed;
					if (liveIdentityExists) {
						committed = super.updateIdentity(idRequest, uin);
					} else {
						committed = super.addIdentity(idRequest, uin);
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
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			PendingCredentialSync.clear();
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (IdRepoAppException | RuntimeException e) {
			PendingCredentialSync.clear();
			throw e;
		} finally {
			credentialStatusManager.runPendingAfterIdentityCommit();
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "publishDraftV2", startNanos);
		}
	}

	/**
	 * Loads a draft for publishing with its biometric and document collections initialized.
	 * Must run inside a transaction.
	 *
	 * @param regId registration id of the draft
	 * @return the draft
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when the draft does not exist;
	 *         {@code DRAFT_UIN_DETAILS_NOT_FOUND} when the UIN, UIN hash or identity data
	 *         is missing (a LOST draft that was never stamped)
	 */
	private UinDraft loadDraftForPublish(String regId) throws IdRepoAppException {
		UinDraft draft = uinDraftRepo.findByRegId(regId).orElse(null);
		if (draft == null) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT,
					DRAFT_RECORD_NOT_FOUND + " | regId=" + regId);
			throw new IdRepoAppException(NO_RECORD_FOUND);
		}
		if (draft.getUinHash() == null || draft.getUin() == null || draft.getUinData() == null) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, PUBLISH_DRAFT,
					"UIN details not found in draft | regId=" + regId);
			throw new IdRepoAppException(DRAFT_UIN_DETAILS_NOT_FOUND);
		}
		Hibernate.initialize(draft.getBiometrics());
		Hibernate.initialize(draft.getDocuments());
		return draft;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Biometrics are read from the draft path and returned as a combined CBEFF (see
	 * {@link #extractAndGetCombinedCbeffDraft}); documents are returned as stored.
	 */
	@Override
	public IdResponseDTO getDraftV2(String regId, Map<String, String> extractionFormats, String type)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		String requestedType = normalizeType(type);
		try {
			UinDraft draft = uinDraftRepo.findByRegId(regId).orElse(null);
			if (draft == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT,
						DRAFT_RECORD_NOT_FOUND + " | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
			boolean all = "all".equals(requestedType);
			List<DocumentsDTO> documents = new ArrayList<>();
			if ((all || "biometrics".equals(requestedType)) && draft.getBiometrics() != null) {
				for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
					documents.add(new DocumentsDTO(bioDraft.getBiometricFileType(), CryptoUtil.encodeToURLSafeBase64(
							extractAndGetCombinedCbeffDraft(ridHash, bioDraft.getBioFileId(), extractionFormats))));
				}
			}
			if ((all || "supportingdocuments".equals(requestedType)) && draft.getDocuments() != null) {
				for (UinDocumentDraft docDraft : draft.getDocuments()) {
					documents.add(new DocumentsDTO(docDraft.getDoccatCode(), CryptoUtil.encodeToURLSafeBase64(
							objectStoreHelper.getDraftDemographicObject(ridHash, docDraft.getDocId()))));
				}
			}
			byte[] identityPayload = all || "demographics".equals(requestedType) ? draft.getUinData() : null;
			return constructIdResponse(identityPayload, draft.getStatusCode(), documents, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "getDraftV2", startNanos);
		}
	}

	/**
	 * Validates and normalizes the {@code type} query parameter of {@link #getDraftV2}.
	 *
	 * @param type requested content type; may be {@code null} or blank
	 * @return lower-case {@code demographics}, {@code biometrics}, {@code supportingdocuments}
	 *         or {@code all}; {@code all} when {@code type} is {@code null} or blank
	 * @throws IdRepoAppException {@code INVALID_INPUT_PARAMETER} for any other value
	 */
	private String normalizeType(String type) throws IdRepoAppException {
		if (type == null || type.isBlank()) {
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
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT,
						"Invalid type query parameter: " + type);
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "type"));
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Runs with {@code NOT_SUPPORTED} so no transaction is held while the SDK runs.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO extractBiometricsV2(String registrationId, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			if (extractionFormats.isEmpty()) {
				return constructIdResponse(null, DRAFTED, null, null);
			}
			UinDraft draft = uinDraftRepo.findByRegId(registrationId).orElse(null);
			if (draft == null) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT,
						DRAFT_RECORD_NOT_FOUND + " | regId=" + registrationId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			extractBiometricsDraftV2(extractionFormats, draft);
			return constructIdResponse(null, DRAFTED, null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "extractBiometricsV2", startNanos);
		}
	}

	/**
	 * For each biometric file of the draft, deletes the previously extracted files of the
	 * requested formats and extracts them again, storing the results on the draft path.
	 *
	 * @param extractionFormats modality key to SDK format
	 * @param draft             draft whose biometric files are processed
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on persistence failure;
	 *         {@code BIO_EXTRACTION_ERROR} for any other extraction failure
	 */
	private void extractBiometricsDraftV2(Map<String, String> extractionFormats, UinDraft draft)
			throws IdRepoAppException {
		if (draft.getBiometrics() == null) {
			return;
		}
		String ridHash = objectStoreHelper.getRidHash(draft.getRegId());
		for (UinBiometricDraft bioDraft : draft.getBiometrics()) {
			try {
				for (Entry<String, String> extractionFormat : extractionFormats.entrySet()) {
					objectStoreHelper.deleteDraftBiometricObject(ridHash,
							buildExtractionFileName(extractionFormat, bioDraft.getBioFileId()));
				}
				extractAndGetCombinedCbeffDraft(ridHash, bioDraft.getBioFileId(), extractionFormats);
			} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
				throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
			} catch (IdRepoAppException e) {
				throw e;
			} catch (Exception e) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, GET_DRAFT, e.getMessage());
				throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
			}
		}
	}

	/**
	 * Reads a draft CBEFF and returns it with the requested modalities replaced by their
	 * extracted templates.
	 *
	 * @param ridHash           rid hash of the draft
	 * @param bioFileId         file id of the draft CBEFF
	 * @param extractionFormats modality key to SDK format; empty returns the CBEFF unchanged
	 *                          apart from exception BIRs being removed
	 * @return combined CBEFF XML
	 * @throws IdRepoAppException when the file cannot be read or extraction fails
	 */
	private byte[] extractAndGetCombinedCbeffDraft(String ridHash, String bioFileId,
			Map<String, String> extractionFormats) throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormatsDraft(ridHash, bioFileId, extractionFormats,
				objectStoreHelper.getDraftBiometricObject(ridHash, bioFileId));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Runs with {@code NOT_SUPPORTED}; the rows are deleted in their own short transaction
	 * by {@link #deleteDraftDbRecords}.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public IdResponseDTO discardDraftV2(String regId) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			if (!uinDraftRepo.existsByRegId(regId)) {
				idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, DISCARD_DRAFT,
						"RID NOT FOUND IN DB | regId=" + regId);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			String ridHash = objectStoreHelper.getRidHash(regId);
			objectStoreHelper.deleteAllDraftBiometrics(ridHash);
			objectStoreHelper.deleteAllDraftDemographics(ridHash);
			deleteDraftDbRecords(regId);
			return constructIdResponse(null, "DISCARDED", null, null);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, DISCARD_DRAFT, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} finally {
			IdentityServicePerformanceLog.log(idrepoDraftLogger, ID_REPO_DRAFT_SERVICE_IMPL, "discardDraftV2", startNanos);
		}
	}

	/**
	 * Deletes the biometric draft, document draft and draft rows of a registration in one
	 * transaction, children first.
	 *
	 * @param regId registration id of the draft
	 */
	private void deleteDraftDbRecords(String regId) {
		newTransactionTemplate().executeWithoutResult(status -> {
			uinBiometricDraftRepo.deleteByRegId(regId);
			uinDocumentDraftRepo.deleteByRegId(regId);
			uinDraftRepo.deleteByRegId(regId);
		});
	}
}
