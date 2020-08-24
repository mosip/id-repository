package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ServiceError;




@Component
public class IdrepositaryUtil {

	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	public IdResponseDTO getData(CredentialServiceRequestDto credentialServiceRequestDto,Map<String,String> bioAttributeFormatterMap)
			throws ApiNotAccessibleException, IOException, IdRepoException {
		
		Map<String,Object> map=credentialServiceRequestDto.getAdditionalData();
		String idType=(String) map.get("idType");
		String fingerExtractionFormat=bioAttributeFormatterMap.get(CredentialConstants.FINGER);
		String faceExtractionFormat=bioAttributeFormatterMap.get(CredentialConstants.FACE);
		String irisExtractionFormat=bioAttributeFormatterMap.get(CredentialConstants.IRIS);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(credentialServiceRequestDto.getId());
		String queryParamName = "TYPE,ID_TYPE,FINGER_EXTRACTION_FORMAT,IRIS_EXTRACTION_FORMAT,FACE_EXTRACTION_FORMAT";
		String queryParamValue = "all"+","+idType+","+fingerExtractionFormat+","+irisExtractionFormat +","+ faceExtractionFormat;

			String responseString = restUtil.getApi(ApiName.IDREPOGETIDBYUIN, pathsegments, queryParamName,
					queryParamValue, String.class);
			IdResponseDTO responseObject = mapper.readValue(responseString, IdResponseDTO.class);
		if (responseObject == null) {
			throw new IdRepoException();
		}
		if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
			ServiceError error = responseObject.getErrors().get(0);
			throw new IdRepoException(
					error.getMessage());
			} else {
				return responseObject;
			}

	}


}
