package io.mosip.credential.request.generator.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;
import io.mosip.credential.request.generator.dto.CredentialIssueResponseDto;
import io.mosip.credential.request.generator.dto.ErrorDTO;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialIssueException;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.util.DateUtils;

/**
 * 
 * @author Sowmya
 *
 * The Class CredentialRequestServiceImpl.
 */
public class CredentialRequestServiceImpl implements CredentialRequestService {
	
	/** The credential repositary. */
	@Autowired
	CredentialRepositary<CredentialEntity, String> credentialRepositary;
	
	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.request.datetime.pattern";

	/** The Constant CREDENTIAL_REQUEST_SERVICE_ID. */
	private static final String CREDENTIAL_REQUEST_SERVICE_ID = "mosip.credential.request.service.id";

	/** The Constant CREDENTIAL_REQUEST_SERVICE_VERSION. */
	private static final String CREDENTIAL_REQUEST_SERVICE_VERSION = "mosip.credential.request.service.version";

	@Autowired
	private ObjectMapper mapper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credential.request.generator.service.CredentialRequestService#
	 * createCredentialIssuance(io.mosip.credential.request.generator.dto.
	 * CredentialIssueRequestDto)
	 */
	@Override
	public CredentialIssueResponseDto createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto) {
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialIssueResponseDto credentialIssueResponseDto = new CredentialIssueResponseDto();

		CredentialIssueResponse credentialIssueResponse = null;
		try{
			String requestId = generateId();
		

	    CredentialEntity credential=new CredentialEntity();
		credential.setRequestId(requestId);
			credential.setRequest(mapper.writeValueAsString(credentialIssueRequestDto));
			credential.setStatusCode(CredentialStatusCode.NEW.name());
		credential.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setCreatedBy(USER);
			// TODO whether to add cellname
		credentialRepositary.save(credential);
			credentialIssueResponse = new CredentialIssueResponse();
			credentialIssueResponse.setRequestId(requestId);
	    }catch(DataAccessLayerException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);

		} catch (Exception e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
		} finally {
			credentialIssueResponseDto.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			} else {
				credentialIssueResponseDto.setResponse(credentialIssueResponse);
			}

		}
		return credentialIssueResponseDto;
	}

	/**
	 * Generate id.
	 *
	 * @return the string
	 */
	private String generateId() {
		return UUID.randomUUID().toString();
	}

	@Override
	public CredentialIssueResponseDto cancelCredentialRequest(String requestId) {
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialIssueResponseDto credentialIssueResponseDto = new CredentialIssueResponseDto();

		CredentialIssueResponse credentialIssueResponse = null;
		try {
			Optional<CredentialEntity> entity = credentialRepositary.findById(requestId);
			if (entity != null) {
				CredentialEntity credentialEntity = entity.get();
				credentialEntity.setStatusCode(CredentialStatusCode.CANCELLED.name());
				credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				credentialEntity.setUpdatedBy(USER);
				credentialRepositary.update(credentialEntity);
				credentialIssueResponse = new CredentialIssueResponse();
				credentialIssueResponse.setRequestId(requestId);
			} else {
				throw new CredentialIssueException(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode(),
						CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
			}
		} catch (DataAccessLayerException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);

		} catch (Exception e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
		} finally {
			credentialIssueResponseDto.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			} else {
				credentialIssueResponseDto.setResponse(credentialIssueResponse);
			}

		}
		return credentialIssueResponseDto;
	}
	;
}
