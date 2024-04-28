package io.mosip.credential.request.generator.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.constants.LoggerFileConstant;
import io.mosip.credential.request.generator.dao.CredentialDao;
import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.dto.Event;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorException;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.credential.request.generator.util.CacheUtil;
import io.mosip.credential.request.generator.util.Utilities;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialIssueRequest;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.dto.CredentialRequestIdsDto;
import io.mosip.idrepository.core.dto.PageDto;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;

/**
 *
 * @author Sowmya
 *
 * The Class CredentialRequestServiceImpl.
 */
@Component
public class CredentialRequestServiceImpl implements CredentialRequestService {

	@Autowired
	private CredentialDao credentialDao;
	
	/** The Constant USER. */
	private static final String PRINT_USER = "service-account-mosip-print-client";

	@Autowired
	private ObjectMapper mapper;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialRequestServiceImpl.class);

	@Autowired
	private Utilities utilities;


	private static final String CREATE_CREDENTIAL = "createCredentialIssuance";


	private static final String CREDENTIAL_SERVICE = "CredentialRequestServiceImpl";

	private static final String GET_REQUESTIDS = "getRequestIds";

	@Autowired
	private AuditHelper auditHelper;
	
    @Autowired
    private CacheUtil cacheUtil;

	private static final String CANCEL_CREDENTIAL = "cancelCredentialRequest";


	private static final String UPDATE_STATUS_CREDENTIAL = "updateCredentialStatus";

	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.credential.request.generator.service.CredentialRequestService#
	 * createCredentialIssuance(io.mosip.credential.request.generator.dto.
	 * CredentialIssueRequestDto)
	 */
	@Override
	public ResponseWrapper<CredentialIssueResponse> createCredentialIssuance(CredentialIssueRequest credentialIssueRequestDto) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
				"started creating credential");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();

		CredentialIssueResponse credentialIssueResponse = null;
		String requestId = utilities.generateId();
		try{
			CredentialEntity credential=new CredentialEntity();
			credential.setRequestId(requestId);
			credential.setRequest(mapper.writeValueAsString(credentialIssueRequestDto));
			credential.setStatusCode(CredentialStatusCode.NEW.name());
			credential.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			credential.setCreatedBy(IdRepoSecurityManager.getUser());
			credential.setStatusComment("Request created");
			credentialDao.save(credential);
			cacheUtil. setCredentialTransaction(requestId, credential,credentialIssueRequestDto.getId());
			credentialIssueResponse = new CredentialIssueResponse();
			credentialIssueResponse.setRequestId(requestId);
			credentialIssueResponse.setId(credentialIssueRequestDto.getId());
			LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
					"ended creating credential");
		}catch(DataAccessLayerException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.CREATING_CREDENTIAL_REQUEST, requestId, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.CREATING_CREDENTIAL_REQUEST, requestId, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} finally {
			credentialIssueResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			credentialIssueResponseWrapper.setResponsetime(getCurrentDateTime());
			credentialIssueResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialIssueResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueResponseWrapper.setResponse(credentialIssueResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID,"create credential request requested");
		}
		return credentialIssueResponseWrapper;
	}

	@Override
	public ResponseWrapper<CredentialIssueResponse> createCredentialIssuanceByRid(CredentialIssueRequest credentialIssueRequestDto, String rid) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
				"started creating credential");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();
		CredentialIssueResponse credentialIssueResponse = null;
		try{
			CredentialEntity credential=new CredentialEntity();
			credential.setRequestId(rid);
			credential.setRequest(mapper.writeValueAsString(credentialIssueRequestDto));
			credential.setStatusCode(CredentialStatusCode.NEW.name());
			credential.setCreateDateTime(DateUtils.getUTCCurrentDateTime());
			credential.setCreatedBy(IdRepoSecurityManager.getUser());
			credential.setStatusComment("Request created");
			credentialDao.save(credential);
			cacheUtil. setCredentialTransaction(rid, credential,credentialIssueRequestDto.getId());
			credentialIssueResponse = new CredentialIssueResponse();
			credentialIssueResponse.setRequestId(rid);
			credentialIssueResponse.setId(credentialIssueRequestDto.getId());
			LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
					"ended creating credential");
		}catch(DataAccessLayerException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.CREATING_CREDENTIAL_REQUEST, rid, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.CREATING_CREDENTIAL_REQUEST, rid, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} finally {
			credentialIssueResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			credentialIssueResponseWrapper.setResponsetime(getCurrentDateTime());
			credentialIssueResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialIssueResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueResponseWrapper.setResponse(credentialIssueResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID,"create credential request requested");
		}
		return credentialIssueResponseWrapper;
	}

	@Override
	public ResponseWrapper<CredentialIssueResponse> cancelCredentialRequest(String requestId) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), 
				requestId,
				"started cancelling credential");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();

		CredentialIssueResponse credentialIssueResponse = null;
		try {
			Optional<CredentialEntity> entity = credentialDao.findById(requestId);
			if(!entity.isPresent()) {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
				errorList.add(error);
				return credentialIssueResponseWrapper;
			}

			CredentialEntity credentialEntity = entity.get();
			if (credentialEntity.getStatusCode().equalsIgnoreCase("NEW")) {
				credentialEntity.setStatusCode(CredentialStatusCode.CANCELLED.name());
				credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				credentialEntity.setUpdatedBy(IdRepoSecurityManager.getUser());
				credentialEntity.setStatusComment("Cancel the request");
				credentialDao.save(credentialEntity);
				CredentialIssueRequestDto credentialIssueRequestDto = mapper
						.readValue(credentialEntity.getRequest(),
								CredentialIssueRequestDto.class);
				credentialIssueResponse = new CredentialIssueResponse();
				credentialIssueResponse.setId(credentialIssueRequestDto.getId());
				credentialIssueResponse.setRequestId(requestId);
				cacheUtil.updateCredentialTransaction(requestId, credentialEntity,credentialIssueRequestDto.getId());

				LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CANCEL_CREDENTIAL,
						"Cancelling credential status of " + requested);
				return credentialIssueResponseWrapper;
			}

			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_PROCESSED_ERROR.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_PROCESSED_ERROR.getErrorMessage());
			errorList.add(error);
		
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended cancelling credential");
			
		} catch (DataAccessLayerException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
		} catch (JsonProcessingException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(),LoggerFileConstant.REQUEST_ID.toString(),
					requestId,
					ExceptionUtils.getStackTrace(e));
		} finally {
			credentialIssueResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			credentialIssueResponseWrapper.setResponsetime(getCurrentDateTime());
			credentialIssueResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialIssueResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueResponseWrapper.setResponse(credentialIssueResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,"Cancel the request");
		}
		return credentialIssueResponseWrapper;
	}


	@Override
	public ResponseWrapper<CredentialIssueStatusResponse> getCredentialRequestStatus(String requestId) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started getting  credential status");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueStatusResponseWrapper = new ResponseWrapper<CredentialIssueStatusResponse>();

		CredentialIssueStatusResponse credentialIssueStatusResponse = new CredentialIssueStatusResponse();
		try {
			Optional<CredentialIssueStatusResponse> cachedResponse = Optional.ofNullable(cacheUtil.getCredentialTransaction(requestId));
			if(cachedResponse.isPresent()) {
				credentialIssueStatusResponse = cachedResponse.get();
				return credentialIssueStatusResponseWrapper;
			}
			
			Optional<CredentialEntity> result = credentialDao.findById(requestId);
			CredentialEntity credentialEntity = result.isPresent() ? result.get() : null;
			if (credentialEntity!=null && credentialEntity.getRequestId() != null) {
				CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credentialEntity.getRequest(),
						CredentialIssueRequestDto.class);
				credentialIssueStatusResponse.setId(credentialIssueRequestDto.getId());
				credentialIssueStatusResponse.setRequestId(requestId);
				credentialIssueStatusResponse.setStatusCode(credentialEntity.getStatusCode());
				credentialIssueStatusResponse.setUrl(credentialEntity.getDataShareUrl());
				LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"Built credential issue status response from entity object");
				return credentialIssueStatusResponseWrapper;
			}

			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
			errorList.add(error);			
				
		} catch (DataAccessLayerException e) {
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
		} catch (JsonProcessingException e) {

			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
		}
		finally {
			credentialIssueStatusResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			credentialIssueStatusResponseWrapper.setResponsetime(getCurrentDateTime());
			credentialIssueStatusResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialIssueStatusResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueStatusResponseWrapper.setResponse(credentialIssueStatusResponse);
			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Ended getting credential status");

		}
		return credentialIssueStatusResponseWrapper;
	}


	@Override
	public void updateCredentialStatus(CredentialStatusEvent credentialStatusEvent) throws CredentialRequestGeneratorException {
		String requestId=null;
		try {

			Event event=credentialStatusEvent.getEvent();
			requestId=credentialStatusEvent.getEvent().getRequestId();
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"started updating  credential status");
			Optional<CredentialEntity> entity = credentialDao.findById(requestId);
			if(!entity.isPresent())
				throw new CredentialRequestGeneratorException();

			CredentialEntity credentialEntity = entity.get();
			credentialEntity.setStatusCode(event.getStatus());
			if(!StringUtils.isEmpty(event.getUrl())) {
				credentialEntity.setDataShareUrl(event.getUrl());
			}
			credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			credentialEntity.setUpdatedBy(PRINT_USER);
			credentialEntity.setStatusComment("updated the status from partner");
			credentialDao.save(credentialEntity);
			CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credentialEntity.getRequest(),
					CredentialIssueRequestDto.class);
			cacheUtil.updateCredentialTransaction(requestId, credentialEntity, credentialIssueRequestDto.getId());
			
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended updating  credential status");
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,"update the request");
		}catch (DataAccessLayerException | JsonProcessingException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, UPDATE_STATUS_CREDENTIAL,
					ExceptionUtils.getStackTrace(e));
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
			throw new CredentialRequestGeneratorException();
		}
	}

	@Override
	public ResponseWrapper<PageDto<CredentialRequestIdsDto>> getRequestIds(String statusCode, String effectivedtimes,
																		   int pageNumber,
																		   int pageSize,
																		   String sortBy, String direction) {
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialRequestIdsResponseWrapper = new ResponseWrapper<PageDto<CredentialRequestIdsDto>>();
		PageDto<CredentialRequestIdsDto> pageDto = null;
		try {
			List<CredentialRequestIdsDto> requestDetails = new ArrayList<>();

			Page<CredentialEntity> pageData=null;
			if (StringUtils.isEmpty(effectivedtimes)) {
				pageData = credentialDao.findByStatusCode(statusCode,
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(direction), sortBy)));
			}else {
				LocalDateTime effectiveDateTime=DateUtils.parseToLocalDateTime(effectivedtimes);
				pageData = credentialDao.findByStatusCodeWithEffectiveDtimes(statusCode, effectiveDateTime,
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(direction), sortBy)));
			}

			if (pageData != null && pageData.getContent() != null && !pageData.getContent().isEmpty()) {
				List<CredentialEntity> credentialRequestList = pageData.getContent();
				for (CredentialEntity credential : credentialRequestList) {
					CredentialRequestIdsDto credentialRequestIdsDto=new CredentialRequestIdsDto();
					CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credential.getRequest(),
							CredentialIssueRequestDto.class);
					credentialRequestIdsDto.setRequestId(credential.getRequestId());
					credentialRequestIdsDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
					credentialRequestIdsDto.setPartner(credentialIssueRequestDto.getIssuer());
					credentialRequestIdsDto.setStatusCode(credential.getStatusCode());
					credentialRequestIdsDto.setStatusComment(credential.getStatusComment());

					credentialRequestIdsDto.setCreateDateTime(credential.getCreateDateTime().toString());
					credentialRequestIdsDto.setUpdateDateTime(credential.getUpdateDateTime().toString());

					requestDetails.add(credentialRequestIdsDto);
				}

				pageDto = new PageDto<>(pageData.getNumber(), pageSize, pageData.getSort(), pageData.getTotalElements(),
						pageData.getTotalPages(), requestDetails);
			} else {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.DATA_NOT_FOUND.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.DATA_NOT_FOUND.getErrorMessage());
				errorList.add(error);
			}

		} catch (JsonProcessingException e) {
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, GET_REQUESTIDS,
					ExceptionUtils.getStackTrace(e));
		} catch (DateTimeParseException e) {
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATE_PARSE_ERROR.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATE_PARSE_ERROR.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, GET_REQUESTIDS,
					ExceptionUtils.getStackTrace(e));
		} catch (DataAccessLayerException e) {

			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, GET_REQUESTIDS,
					ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {

			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, GET_REQUESTIDS,
					ExceptionUtils.getStackTrace(e));
		} finally {
			credentialRequestIdsResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			credentialRequestIdsResponseWrapper.setResponsetime(getCurrentDateTime());
			credentialRequestIdsResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialRequestIdsResponseWrapper.setErrors(errorList);
			} else {
				credentialRequestIdsResponseWrapper.setResponse(pageDto);
			}

		}
		return credentialRequestIdsResponseWrapper;
	}

	@Override
	public ResponseWrapper<CredentialIssueResponse> retriggerCredentialRequest(String requestId) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started updating to retry credential");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();

		CredentialIssueResponse credentialIssueResponse = null;
		try {
			Optional<CredentialEntity> entity = credentialDao.findById(requestId);
			if (entity.isPresent()) {
				CredentialEntity credentialEntity = entity.get();

				credentialEntity.setStatusCode(CredentialStatusCode.RETRY.name());
				credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				credentialEntity.setUpdatedBy(IdRepoSecurityManager.getUser());
				credentialEntity.setStatusComment("retrigger the request");
				CredentialIssueRequestDto credentialIssueRequestDto = mapper
						.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class);
				credentialDao.save(credentialEntity);
				credentialIssueResponse = new CredentialIssueResponse();
				credentialIssueResponse.setId(credentialIssueRequestDto.getId());
				credentialIssueResponse.setRequestId(requestId);
				cacheUtil.updateCredentialTransaction(requestId, credentialEntity, credentialIssueRequestDto.getId());

				LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CANCEL_CREDENTIAL,
						"updated to RETRY credential status of " + requestId);

			} else {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
				errorList.add(error);
			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended updating to retry credential");
		} catch (DataAccessLayerException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.RETRY_CREDENTIAL_REQUEST, requestId, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
		} catch (JsonProcessingException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
					AuditEvents.RETRY_CREDENTIAL_REQUEST, requestId, IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
		} finally {
			credentialIssueResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			credentialIssueResponseWrapper.setResponsetime(getCurrentDateTime());
			credentialIssueResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialIssueResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueResponseWrapper.setResponse(credentialIssueResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.RETRY_CREDENTIAL_REQUEST,
					requestId, IdType.ID, "retrigger the request");
		}
		return credentialIssueResponseWrapper;
	}

	private LocalDateTime getCurrentDateTime() {
		DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
		return LocalDateTime.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);	
	}

}
