package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.dto.IdResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.kernel.core.exception.ServiceError;




@Component
public class IdrepositaryUtil {

	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	public IdResponseDto getData(String id, String formatter)
			throws ApiNotAccessibleException, IOException, IdRepoException {
		// TODO to call id repo new api by providing id and formatter (list or one
		// formatter need to decide)to get demo and
		// bio extracted details
		// now its calling existing api for testing


		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(id);
		String queryParamName = "type";
		String queryParamValue = "all";

			String responseString = restUtil.getApi(ApiName.IDREPOGETIDBYUIN, pathsegments, queryParamName,
					queryParamValue, String.class);
			IdResponseDto responseObject = mapper.readValue(responseString, IdResponseDto.class);
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
