package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;

@Component
public class IdrepositaryUtil {

	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	private static final Logger LOGGER = IdRepoLogger.getLogger(DataShareUtil.class);

	private static final String GET_DATA = "getData";

	private static final String IDREPOSITARYUTIL = "IdrepositaryUtil";

	public IdResponseDTO getData(CredentialServiceRequestDto credentialServiceRequestDto,
			Map<String, String> bioAttributeFormatterMap)
			throws ApiNotAccessibleException, IdRepoException, JsonParseException, JsonMappingException, IOException {
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), IDREPOSITARYUTIL, GET_DATA,

					"entry");
			Map<String, Object> map = credentialServiceRequestDto.getAdditionalData();
			String idType = null;
			idType = (String) map.get("idType");

			String fingerExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.FINGER);
			String faceExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.FACE);
			String irisExtractionFormat = bioAttributeFormatterMap.get(CredentialConstants.IRIS);
			List<String> pathsegments = new ArrayList<>();
			pathsegments.add(credentialServiceRequestDto.getId());
			String queryParamName = "type";
			String queryParamValue = "all";
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

			String responseString = restUtil.getApi(ApiName.IDREPOGETIDBYID, pathsegments, queryParamName,
					queryParamValue, String.class);
			IdResponseDTO responseObject = mapper.readValue(responseString, IdResponseDTO.class);
			if (responseObject == null) {
				LOGGER.error(IdRepoSecurityManager.getUser(), IDREPOSITARYUTIL, GET_DATA,
						CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
				throw new IdRepoException();
			}
			if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {

				ServiceError error = responseObject.getErrors().get(0);
				LOGGER.error(IdRepoSecurityManager.getUser(), IDREPOSITARYUTIL, GET_DATA, error.getMessage());
				throw new IdRepoException(error.getMessage());
			} else {
				LOGGER.debug(IdRepoSecurityManager.getUser(), IDREPOSITARYUTIL, GET_DATA, "exit");
				return responseObject;
			}
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), IDREPOSITARYUTIL, GET_DATA, ExceptionUtils.getStackTrace(e));
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
