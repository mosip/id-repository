package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.DataShareResponseDto;
import io.mosip.credentialstore.dto.ErrorDTO;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialServiceException;

@Component
public class DataShareUtil {
	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	public DataShare getDataShare(byte[] data, String policyId, String partnerId)
			throws ApiNotAccessibleException, IOException, CredentialServiceException {
		
		ByteArrayResource contentsAsResource = new ByteArrayResource(data) {
			
		};
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("file", contentsAsResource);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(policyId);
		pathsegments.add(partnerId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
				map, headers);
		String responseString = restUtil.postApi(ApiName.CREATEDATASHARE, pathsegments, "", "",
				MediaType.MULTIPART_FORM_DATA, requestEntity, String.class);
		DataShareResponseDto responseObject = mapper.readValue(responseString, DataShareResponseDto.class);

		if (responseObject == null) {
			throw new CredentialServiceException(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
					CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
		}
		if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
			ErrorDTO error = responseObject.getErrors().get(0);
			throw new CredentialServiceException(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
					error.getMessage());
		} else {
			return responseObject.getDataShare();
		}


	}
}
