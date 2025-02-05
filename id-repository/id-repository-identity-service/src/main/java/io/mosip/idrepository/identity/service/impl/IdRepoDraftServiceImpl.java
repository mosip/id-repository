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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
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
import org.springframework.transaction.annotation.Transactional;
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
import io.mosip.idrepository.identity.repository.UinBiometricRepo;
import io.mosip.idrepository.identity.repository.UinDocumentRepo;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
/**
 * @author Manoj SP
 *
 */
@Service
@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
public class IdRepoDraftServiceImpl extends IdRepoServiceImpl implements IdRepoDraftService<IdRequestDTO, IdResponseDTO> {

	private static final Logger idrepoDraftLogger = IdRepoLogger.getLogger(IdRepoDraftServiceImpl.class);
	private static final String COMMA = ",";
	private static final String DEFAULT_ATTRIBUTE_LIST = "UIN,verifiedAttributes,IDSchemaVersion";

	@Value("${" + MOSIP_KERNEL_IDREPO_JSON_PATH + "}")
	private String uinPath;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Autowired
	private UinDraftRepo uinDraftRepo;

	@Autowired
	private IdRequestValidator validator;

	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private RestHelper restHelper;

	@Autowired
	private UinBiometricRepo uinBiometricRepo;

	@Autowired
	private UinDocumentRepo uinDocumentRepo;
	
	@Autowired
	private IdRepoProxyServiceImpl proxyService;
	
	@Autowired
	private VidDraftHelper vidDraftHelper;

	@Autowired
	private Environment environment;
	
	@Value("${mosip.idrepo.create-identity.enable-force-merge:false}")
	private boolean isForceMergeEnabled;
	
	@Override
	public IdResponseDTO createDraft(String registrationId, String uin) throws IdRepoAppException {
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
				newDraft.setCreatedDateTime(DateUtils.getUTCCurrentDateTime());
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
	}

	private Object generateIdentityObject(Object uin) {
		List<String> pathList = new ArrayList<>(Arrays.asList("identity.UIN".split("\\.")));
		pathList.remove(ROOT_PATH);
		Collections.reverse(pathList);
		for (String string : pathList) {
			uin = new HashMap<>(Map.of(string, uin));
		}
		return uin;
	}

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
	public IdResponseDTO updateDraft(String registrationId, IdRequestDTO request) throws IdRepoAppException {
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
					draftToUpdate.setUpdatedDateTime(DateUtils.getUTCCurrentDateTime());
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
	}

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
				super.updateJsonObject(draftToUpdate.getUinHash(), inputData, dbData, comparisonResult, false);
			}
			draftToUpdate.setUinData(convertToBytes(convertToObject(dbData.jsonString().getBytes(), Map.class)));
			draftToUpdate.setUinDataHash(securityManager.hash(draftToUpdate.getUinData()));
			draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
			draftToUpdate.setUpdatedDateTime(DateUtils.getUTCCurrentDateTime());
		}
	}

	private void updateDocuments(RequestDTO requestDTO, UinDraft draftToUpdate) throws IdRepoAppException {
		if (Objects.nonNull(requestDTO.getDocuments()) && !requestDTO.getDocuments().isEmpty()) {
			Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
			String uinHashWithSalt = draftToUpdate.getUinHash().split(SPLITTER)[1];
			super.updateDocuments(uinHashWithSalt, uinObject, requestDTO, true);
			updateBiometricAndDocumentDrafts(requestDTO.getRegistrationId(), draftToUpdate, uinObject);
		}
	}

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
					draftBio.setUpdatedDateTime(DateUtils.getUTCCurrentDateTime());
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
	public IdResponseDTO publishDraft(String regId) throws IdRepoAppException {
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
	 * Builds the request.
	 *
	 * @param regId the reg id
	 * @param draft the draft
	 * @return the id request DTO
	 * @throws IdRepoAppException the id repo app exception
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

	private void validateRequest(RequestDTO request) throws IdRepoDataValidationException {
		Errors errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
		validator.validateRequest(request, errors, "create");
		DataValidationUtil.validate(errors);
	}

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

	private String decryptUin(String encryptedUin, String uinHash) throws IdRepoAppException {
		String salt = uinEncryptSaltRepo.getOne(Integer.valueOf(encryptedUin.split(SPLITTER)[0])).getSalt();
		String uin = new String(securityManager.decryptWithSalt(
				CryptoUtil.decodeURLSafeBase64(StringUtils.substringAfter(encryptedUin, SPLITTER)),
				CryptoUtil.decodePlainBase64(salt), uinRefId));
		if (!StringUtils.equals(super.getUinHash(uin), uinHash)) {
			throw new IdRepoAppUncheckedException(UIN_HASH_MISMATCH);
		}
		return uin;
	}

	@Override
	public IdResponseDTO discardDraft(String regId) throws IdRepoAppException {
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
	}

	@Override
	public boolean hasDraft(String regId) throws IdRepoAppException {
		try {
			return uinDraftRepo.existsByRegId(regId);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			idrepoDraftLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_SERVICE_IMPL, "hasDraft", e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public IdResponseDTO getDraft(String regId, Map<String, String> extractionFormats) throws IdRepoAppException {
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
	}

	@Override
	public IdResponseDTO extractBiometrics(String registrationId, Map<String, String> extractionFormats)
			throws IdRepoAppException {
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
	}

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

	private void deleteExistingExtractedBioData(Map<String, String> extractionFormats, String uinHash, UinBiometricDraft bioDraft) {
		extractionFormats.entrySet()
				.forEach(extractionFormat -> super.objectStoreHelper.deleteBiometricObject(uinHash,
						buildExtractionFileName(extractionFormat, bioDraft.getBioFileId())));
	}

	private byte[] extractAndGetCombinedCbeff(String uinHash, String bioFileId, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		return proxyService.getBiometricsForRequestedFormats(uinHash, bioFileId, extractionFormats,
				super.objectStoreHelper.getBiometricObject(uinHash, bioFileId));
	}

	private String buildExtractionFileName(Entry<String, String> extractionFormat, String bioFileId) {
		return bioFileId.split("\\.")[0].concat(DOT).concat(getModalityForFormat(extractionFormat.getKey())).concat(DOT)
				.concat(extractionFormat.getValue());
	}
	
	private String getModalityForFormat(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, "");
	}

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
	public DraftResponseDto getDraftUin(String uin) throws IdRepoAppException{
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
	}

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
}
