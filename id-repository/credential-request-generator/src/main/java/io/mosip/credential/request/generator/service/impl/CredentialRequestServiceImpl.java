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

import io.mosip.credential.request.generator.batch.config.CredentialItemProcessor;
import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.constants.LoggerFileConstant;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialIssueException;

import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.credential.request.generator.util.Utilities;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
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

	/** The Constant LOGGER. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemProcessor.class);
	
	@Autowired
	private Utilities utilities;
	
	/** The Constant BIOMETRICS. */
	private static final String CREATE_CREDENTIAL = "createCredentialIssuance";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String CREDENTIAL_SERVICE = "CredentialRequestServiceImpl";
	
	@Autowired
	private AuditHelper auditHelper;
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credential.request.generator.service.CredentialRequestService#
	 * createCredentialIssuance(io.mosip.credential.request.generator.dto.
	 * CredentialIssueRequestDto)
	 */
	@Override
	public CredentialIssueResponseDto createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
				"started creating credential");
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialIssueResponseDto credentialIssueResponseDto = new CredentialIssueResponseDto();

		CredentialIssueResponse credentialIssueResponse = null;
		try{
			String requestId = utilities.generateId();
		

	    CredentialEntity credential=new CredentialEntity();
		credential.setRequestId(requestId);
			credential.setRequest(mapper.writeValueAsString(credentialIssueRequestDto));
			credential.setStatusCode(CredentialStatusCode.NEW.name());
		credential.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setCreatedBy(USER);

		credentialRepositary.save(credential);
		credentialIssueResponse = new CredentialIssueResponse();
		credentialIssueResponse.setRequestId(requestId);
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
				"ended creating credential");
	    }catch(DataAccessLayerException e) {
	    	auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
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
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID,"create credential request requested");
		}
		return credentialIssueResponseDto;
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
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);

		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
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
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,"Cancel the request");
		}
		return credentialIssueResponseDto;
	}
	;
}
