package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_GENERATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

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
import io.mosip.idrepository.identity.entity.UinBiometricDraft;
import io.mosip.idrepository.identity.entity.UinDocumentDraft;
import io.mosip.idrepository.identity.entity.UinDraft;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;

/**
 * @author Manoj SP
 *
 */
@Service
@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
public class IdRepoDraftServiceImpl extends IdRepoServiceImpl implements IdRepoDraftService<IdRequestDTO, IdResponseDTO> {

	private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoDraftServiceImpl.class);

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
	private ObjectMapper mapper;

	@Override
	public IdResponseDTO createDraft(IdRequestDTO request) throws IdRepoAppException {
		String registrationId = request.getRequest().getRegistrationId();
		UinDraft newDraft = new UinDraft();
		newDraft.setRegId(registrationId);
		newDraft.setStatusCode("DRAFT");
		if (super.uinHistoryRepo.existsByRegId(registrationId)) {
			newDraft.setUin(super.uinRepo.getUinByRid(registrationId));
			newDraft.setUinHash(super.uinRepo.getUinHashByRid(registrationId));
		} else {
			String uin = generateUin();
			int modValue = getModValue(uin);
			newDraft.setUin(super.getUinToEncrypt(uin, modValue));
			newDraft.setUinHash(super.getUinHash(uin, modValue));
		}
		newDraft.setCreatedBy(IdRepoSecurityManager.getUser());
		newDraft.setCreatedDateTime(DateUtils.getUTCCurrentDateTime());
		uinDraftRepo.save(newDraft);
		return constructIdResponse(null, "DRAFTED", null);
	}

	private String generateUin() throws IdRepoAppException {
		try {
			RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.UIN_GENERATOR_SERVICE, null,
					ResponseWrapper.class);
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(restRequest);
			return response.getResponse().get("uin");
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "IdRepoDraftServiceImpl", "generateUin", e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "IdRepoDraftServiceImpl", "generateUin", e.getMessage());
			throw new IdRepoAppException(UIN_GENERATION_FAILED, e);
		}
	}

	@Override
	public IdResponseDTO updateDraft(IdRequestDTO request) throws IdRepoAppException {
		try {
			String registrationId = request.getRequest().getRegistrationId();
			Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(registrationId);
			if (uinDraft.isPresent()) {
				UinDraft draftToUpdate = uinDraft.get();
				if (Objects.isNull(draftToUpdate.getUinData())) {
					byte[] uinData = super.convertToBytes(request.getRequest().getIdentity());
					draftToUpdate.setUinData(uinData);
					draftToUpdate.setUinDataHash(securityManager.hash(uinData));
				} else {
					updateDemographicData(request, draftToUpdate);
					updateDocuments(request.getRequest(), draftToUpdate);
					uinDraftRepo.save(draftToUpdate);
				}
			}
		} catch (JSONException | InvalidJsonException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "IdRepoDraftServiceImpl", "UpdateDraft", e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
		return constructIdResponse(null, "DRAFTED", null);
	}

	private void updateDemographicData(IdRequestDTO request, UinDraft draftToUpdate) throws JSONException, IdRepoAppException {
		if (Objects.nonNull(request.getRequest()) && Objects.nonNull(request.getRequest().getIdentity())) {
			RequestDTO requestDTO = request.getRequest();
			Configuration configuration = Configuration.builder().jsonProvider(new JacksonJsonProvider())
					.mappingProvider(new JacksonMappingProvider()).build();
			DocumentContext inputData = JsonPath.using(configuration).parse(requestDTO.getIdentity());
			DocumentContext dbData = JsonPath.using(configuration).parse(new String(draftToUpdate.getUinData()));
			JSONCompareResult comparisonResult = JSONCompare.compareJSON(inputData.jsonString(), dbData.jsonString(),
					JSONCompareMode.LENIENT);

			if (comparisonResult.failed()) {
				super.updateIdentityObject(inputData, dbData, comparisonResult);
				draftToUpdate.setUinData(convertToBytes(convertToObject(dbData.jsonString().getBytes(), Map.class)));
				draftToUpdate.setUinDataHash(securityManager.hash(draftToUpdate.getUinData()));
				draftToUpdate.setUpdatedBy(IdRepoSecurityManager.getUser());
				draftToUpdate.setUpdatedDateTime(DateUtils.getUTCCurrentDateTime());
			}
		}
	}

	private void updateDocuments(RequestDTO requestDTO, UinDraft draftToUpdate) throws IdRepoAppException {
		if (Objects.nonNull(requestDTO.getDocuments()) && !requestDTO.getDocuments().isEmpty()) {
			Uin uinObject = mapper.convertValue(draftToUpdate, Uin.class);
			String uinHashWithSalt = draftToUpdate.getUinHash().split(SPLITTER)[1];
			super.updateDocuments(uinHashWithSalt, uinObject, requestDTO, true);
			draftToUpdate
					.setBiometrics(mapper.convertValue(uinObject.getBiometrics(), new TypeReference<List<UinBiometricDraft>>() {
					}));
			draftToUpdate.setDocuments(mapper.convertValue(uinObject.getDocuments(), new TypeReference<List<UinDocumentDraft>>() {
			}));
		}
	}

	@Override
	public IdResponseDTO publishDraft(String regId) throws IdRepoAppException {
		Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
		if (uinDraft.isEmpty()) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "IdRepoDraftServiceImpl", "publishDraft",
					"DRAFT RECORD NOT FOUND");
			throw new IdRepoAppException(NO_RECORD_FOUND);
		} else {
			IdRequestDTO idRequest = new IdRequestDTO();
			RequestDTO request = new RequestDTO();
			request.setRegistrationId(regId);
			request.setIdentity(CryptoUtil.encodeBase64(uinDraft.get().getUinData()));
			idRequest.setRequest(request);
			Errors errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
			validator.validateRequest(request, errors, "create");
			DataValidationUtil.validate(errors);
			String uin = decryptUin(uinDraft);
			String responseStatus = null;
			if (uinHistoryRepo.existsByRegId(regId)) {
				responseStatus = super.updateIdentity(idRequest, uin).getStatusCode();
			} else {
				responseStatus = super.addIdentity(idRequest, uin).getStatusCode();
			}
			this.discardDraft(regId);
			return constructIdResponse(null, responseStatus, null);
		}
	}

	private String decryptUin(Optional<UinDraft> uinDraft) throws IdRepoAppException {
		UinDraft draft = uinDraft.get();
		String salt = uinEncryptSaltRepo.getOne(Integer.valueOf(draft.getUin().split(SPLITTER)[0])).getSalt();
		String uin = new String(securityManager.decryptWithSalt(((String) draft.getUin().split(SPLITTER)[1]).getBytes(),
				salt.getBytes(), uinRefId));
		if (!StringUtils.equals(securityManager.hash(uin.getBytes()), draft.getUinHash().split(SPLITTER)[1])) {
			throw new IdRepoAppUncheckedException(UIN_HASH_MISMATCH);
		}
		return uin;
	}

	@Override
	public IdResponseDTO discardDraft(String regId) {
		uinDraftRepo.findByRegId(regId).ifPresent(uinDraftRepo::delete);
		return constructIdResponse(null, "DISCARDED", null);
	}

	@Override
	public IdResponseDTO hasDraft(String regId) {
		return constructIdResponse(null, String.valueOf(uinDraftRepo.existsByRegId(regId)), null);
	}

	@Override
	public IdResponseDTO getDraft(String regId) throws IdRepoAppException {
		Optional<UinDraft> uinDraft = uinDraftRepo.findByRegId(regId);
		if (uinDraft.isPresent()) {
			UinDraft draft = uinDraft.get();
			String uinHash = draft.getUin().split(SPLITTER)[1];
			List<DocumentsDTO> documents = new ArrayList<>();
			for (UinBiometricDraft uinBiometricDraft : draft.getBiometrics()) {
				documents.add(new DocumentsDTO(uinBiometricDraft.getBiometricFileType(), CryptoUtil
						.encodeBase64(objectStoreHelper.getBiometricObject(uinHash, uinBiometricDraft.getBioFileId()))));
			}
			for (UinDocumentDraft uinDocumentDraft : draft.getDocuments()) {
				documents.add(new DocumentsDTO(uinDocumentDraft.getDoccatCode(),
						CryptoUtil.encodeBase64(objectStoreHelper.getDemographicObject(uinHash, uinDocumentDraft.getDocId()))));
			}
			return constructIdResponse(draft.getUinData(), draft.getStatusCode(), documents);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "IdRepoDraftServiceImpl", "publishDraft",
					"DRAFT RECORD NOT FOUND");
			throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	private IdResponseDTO constructIdResponse(byte[] uinData, String status, List<DocumentsDTO> documents) {
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus(status);
		if (Objects.nonNull(documents))
			response.setDocuments(documents);
		if (Objects.nonNull(uinData))
			response.setIdentity(convertToObject(uinData, Object.class));
		idResponse.setResponse(response);
		return idResponse;
	}

}
