package io.mosip.credentialstore.util;

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
import io.mosip.idrepository.core.dto.IdRequestByIdDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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
			IdRequestByIdDTO requestByIdDTO = new IdRequestByIdDTO();
			RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();

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
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			idDTORequestWrapper.setRequest(requestByIdDTO);
			idDTORequestWrapper.setRequesttime(localdatetime);
			LOGGER.debug(String.format("getIdentity request: %s", requestByIdDTO.toString()));

			String responseString = restUtil.postApi(ApiName.IDREPORETRIEVEIDBYID, null, "", "",
					MediaType.APPLICATION_JSON, idDTORequestWrapper, String.class);

			IdResponseDTO responseObject = mapper.readValue(responseString, IdResponseDTO.class);
			if (responseObject == null) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
				throw new IdRepoException();
			}
			if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = (ServiceError) responseObject.getErrors().get(0);
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
}
