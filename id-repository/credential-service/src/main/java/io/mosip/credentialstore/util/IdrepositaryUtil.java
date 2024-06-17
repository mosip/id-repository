package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.mosip.idrepository.core.dto.IdRequestByIdDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;

import javax.persistence.Id;

@Component
public class IdrepositaryUtil {

	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;
	
	@Value("${mosip.credential.service.fetch-identity.type:all}")
	private String identityType;

	private static final Logger LOGGER = IdRepoLogger.getLogger(IdrepositaryUtil.class);



	public IdResponseDTO getData(CredentialServiceRequestDto credentialServiceRequestDto,
			Map<String, String> bioAttributeFormatterMap)
			throws ApiNotAccessibleException, IdRepoException, JsonParseException, JsonMappingException, IOException {
		String requestId=credentialServiceRequestDto.getRequestId();
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					requestId, "Id repository get data entry");
			LOGGER.debug(String.format("Formatters: %s", bioAttributeFormatterMap.toString()));
			Map<String, Object> map = credentialServiceRequestDto.getAdditionalData();
			String idType = null;
			idType = (String) map.get("idType");

			String fingerExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.FINGER);
			String faceExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.FACE);
			String irisExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.IRIS);
			List<String> pathsegments = new ArrayList<>();
			pathsegments.add(credentialServiceRequestDto.getId());
			String queryParamName = "type";
			String queryParamValue = identityType;
			if (StringUtils.isNotEmpty(idType)) {
				queryParamName = queryParamName + ",idType";
				queryParamValue = queryParamValue + "," + idType;
			}
			if (StringUtils.isNotEmpty(fingerExtractionFormat)) {
				queryParamName = queryParamName + ",fingerExtractionFormat";
				queryParamValue = queryParamValue + "," + fingerExtractionFormat;
			}
			if (StringUtils.isNotEmpty(faceExtractionFormat)) {
				queryParamName = queryParamName + ",faceExtractionFormat";
				queryParamValue = queryParamValue + "," + faceExtractionFormat;
			}
			if (StringUtils.isNotEmpty(irisExtractionFormat)) {
				queryParamName = queryParamName + ",irisExtractionFormat";
				queryParamValue = queryParamValue + "," + irisExtractionFormat;
			}
			
			LOGGER.debug(String.format("getIdentity query param names:%s - query param values: %s", queryParamName, queryParamValue));

			String responseString = restUtil.getApi(ApiName.IDREPOGETIDBYID, pathsegments, queryParamName,
					queryParamValue, String.class);
			IdResponseDTO<Object> responseObject = mapper.readValue(responseString, IdResponseDTO.class);
			if (responseObject == null) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
				throw new IdRepoException();
			}
			if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {

				ServiceError error = responseObject.getErrors().get(0);
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						error.getMessage());
				throw new IdRepoException(error.getMessage());
			} else {
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"Id repository get data exit");
				return responseObject;
			}
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new io.mosip.credentialstore.exception.ApiNotAccessibleException(
						httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoException(e);
			}

		}
	}

	public IdResponseDTO getDataById(CredentialServiceRequestDto credentialServiceRequestDto,
								 Map<String, String> bioAttributeFormatterMap)
			throws ApiNotAccessibleException, IdRepoException, JsonParseException, JsonMappingException, IOException {
		String requestId=credentialServiceRequestDto.getRequestId();
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					requestId, "Id repository getDataById entry");
			LOGGER.debug(String.format("Formatters: %s", bioAttributeFormatterMap.toString()));
			Map<String, Object> map = credentialServiceRequestDto.getAdditionalData();
			String idType = null;
			idType = (String) map.get("idType");

			String fingerExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.FINGER);
			String faceExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.FACE);
			String irisExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.IRIS);
			IdRequestByIdDTO requestByIdDTO = new IdRequestByIdDTO();

			requestByIdDTO.setId(credentialServiceRequestDto.getId());
			requestByIdDTO.setType(identityType);

			if (StringUtils.isNotEmpty(idType)) {
				requestByIdDTO.setIdType(idType);
			}
			if (StringUtils.isNotEmpty(fingerExtractionFormat)) {
				requestByIdDTO.setFingerExtractionFormat(fingerExtractionFormat);
			}
			if (StringUtils.isNotEmpty(faceExtractionFormat)) {
				requestByIdDTO.setFaceExtractionFormat(faceExtractionFormat);
			}
			if (StringUtils.isNotEmpty(irisExtractionFormat)) {
				requestByIdDTO.setIrisExtractionFormat(irisExtractionFormat);
			}

			LOGGER.debug(String.format("getIdentity request: %s", requestByIdDTO.toString()));

			String responseString = restUtil.postApi(ApiName.IDREPORETRIEVEIDBYID, null, "", "",
					MediaType.APPLICATION_JSON, requestByIdDTO, String.class);

			IdResponseDTO responseObject = mapper.readValue(responseString, IdResponseDTO.class);
			if (responseObject == null) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
				throw new IdRepoException();
			}
			if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {

				ServiceError error = responseObject.getErrors().get(0);
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						error.getMessage());
				throw new IdRepoException(error.getMessage());
			} else {
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"Id repository getDataById exit");
				return responseObject;
			}
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new io.mosip.credentialstore.exception.ApiNotAccessibleException(
						httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoException(e);
			}

		}
	}
}
