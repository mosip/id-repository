package io.mosip.idrepository.credential.request.service.impl;

import java.time.LocalDateTime;
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

import io.mosip.idrepository.credential.request.constant.CredentialRequestErrorCodes;
import io.mosip.idrepository.credential.request.constant.CredentialStatusCode;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.request.dao.CredentialDao;
import io.mosip.idrepository.credential.request.dto.CredentialStatusEvent;
import io.mosip.idrepository.credential.request.dto.Event;
import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.credential.request.exception.CredentialRequestGeneratorException;
import io.mosip.idrepository.credential.request.service.CredentialRequestService;
import io.mosip.idrepository.credential.request.util.Utilities;
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
import io.mosip.idrepository.pipeline.CredentialIssuanceProcessor;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.StringUtils;

import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.mosip.idrepository.credential.request.constant.ApiName;
import io.mosip.idrepository.credential.request.util.CredReqRestUtil;
import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.dto.AuditRequestDTO;
import io.mosip.idrepository.core.dto.AuditResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.kernel.core.http.RequestWrapper;

/**
 * Default implementation of {@link CredentialRequestService}.
 * <p>
 * Role in the MOSIP credential pipeline: entry point for partner credential requests
 * ({@code POST /v1/credentialrequest/}). Persists and immediately issues credentials in-process;
 * exposes status, cancel, retry, and admin list APIs.
 * </p>
 *
 * @see CredentialDao
 * @see CredentialEntity
 * @see io.mosip.idrepository.pipeline.CredentialIssuanceProcessor
 * @see AuditHelper
 *
 * @author Sowmya
 */
@Component
public class CredentialRequestServiceImpl implements CredentialRequestService {

	/** Persists and queries credential request rows on the credential datasource. */
	@Autowired
	private CredentialDao credentialDao;

	/** Keycloak service-account principal used when partners update status via WebSub callback. */
	private static final String PRINT_USER = "service-account-mosip-print-client";

	/** Serializes/deserializes {@link CredentialIssueRequest} JSON stored in {@link CredentialEntity}. */
	@Autowired
	private ObjectMapper mapper;

	/** Class logger. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialRequestServiceImpl.class);

	/** Generates request IDs for new credential queue entries. */
	@Autowired
	private Utilities utilities;

	/** Hashes IDs for audit payloads. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** Outbound REST client for kernel audit manager. */
	@Autowired
	private CredReqRestUtil restUtil;

	/** Builds structured audit request wrappers. */
	@Autowired
	private AuditRequestBuilder auditBuilder;

	/** Log context label for create-credential operations. */
	private static final String CREATE_CREDENTIAL = "createCredentialIssuance";

	/** Log context class name. */
	private static final String CREDENTIAL_SERVICE = "CredentialRequestServiceImpl";

	/** Log context label for paginated request-ID queries. */
	private static final String GET_REQUESTIDS = "getRequestIds";

	/** Records audit events for credential-request lifecycle operations. */
	@Autowired
	private AuditHelper auditHelper;

	@Autowired
	private CredentialIssuanceProcessor credentialIssuanceProcessor;

	/** Log context label for cancel operations. */
	private static final String CANCEL_CREDENTIAL = "cancelCredentialRequest";

	/** Log context label for status-update operations. */
	private static final String UPDATE_STATUS_CREDENTIAL = "updateCredentialStatus";

	/**
	 * Queues a new credential issuance request with a server-generated request ID.
	 * <p>
	 * Persists the serialized {@link CredentialIssueRequest} with status {@code NEW}, then issues synchronously.
	 * </p>
	 *
	 * @param credentialIssueRequestDto partner issuance payload (id, credentialType, issuer, etc.)
	 * @return MOSIP response wrapper with assigned {@code requestId} or service errors; never throws
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
			credential.setCreateDateTime(DateUtils2.getUTCCurrentDateTime(EnvUtil.getDateTimePattern()));
			credential.setCreatedBy(IdRepoSecurityManager.getUser());
			credential.setStatusComment("Request created");
			credentialDao.save(credential);
			credentialIssuanceProcessor.issueByRequestId(requestId);
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
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			credentialIssueResponseWrapper
					.setResponsetime(localdatetime);
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

	/**
	 * Queues a credential issuance request using a caller-supplied request ID (typically a RID).
	 * <p>
	 * Same as {@link #createCredentialIssuance(CredentialIssueRequest)} but persists with the given
	 * {@code rid} as the primary key instead of generating a new ID.
	 * </p>
	 *
	 * @param credentialIssueRequestDto partner issuance payload
	 * @param rid                       pre-assigned request identifier (e.g. registration ID)
	 * @return MOSIP response wrapper with {@code requestId} equal to {@code rid} or service errors; never throws
	 */
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
			credential.setCreateDateTime(DateUtils2.getUTCCurrentDateTime());
			credential.setCreatedBy(IdRepoSecurityManager.getUser());
			credential.setStatusComment("Request created");
			credentialDao.save(credential);
			credentialIssuanceProcessor.issueByRequestId(rid);
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

			credentialIssueResponseWrapper
					.setResponsetime(DateUtils2.getUTCCurrentDateTime());
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

	/**
	 * Cancels a queued credential request that has not yet been processed.
	 * <p>
	 * Only requests in {@link CredentialStatusCode#NEW} status can be cancelled; processed requests
	 * return a business error.
	 * </p>
	 *
	 * @param requestId credential request identifier to cancel
	 * @return MOSIP response wrapper with cancelled request details or service errors; never throws
	 */
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
			if (entity.isPresent()) {
				CredentialEntity credentialEntity = entity.get();
				if (credentialEntity.getStatusCode().equalsIgnoreCase("NEW")) {
					credentialEntity.setStatusCode(CredentialStatusCode.CANCELLED.name());
					credentialEntity.setUpdateDateTime(DateUtils2.getUTCCurrentDateTime(EnvUtil.getDateTimePattern()));
					credentialEntity.setUpdatedBy(IdRepoSecurityManager.getUser());
					credentialEntity.setStatusComment("Cancel the request");
					credentialDao.save(credentialEntity);
					CredentialIssueRequestDto credentialIssueRequestDto = mapper
							.readValue(credentialEntity.getRequest(),
									CredentialIssueRequestDto.class);
					credentialIssueResponse = new CredentialIssueResponse();
					credentialIssueResponse.setId(credentialIssueRequestDto.getId());
					credentialIssueResponse.setRequestId(requestId);

					LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CANCEL_CREDENTIAL,
							"Cancelling credential status of " + requestId);
				} else {
					ServiceError error = new ServiceError();
					error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_PROCESSED_ERROR.getErrorCode());
					error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_PROCESSED_ERROR.getErrorMessage());
					errorList.add(error);
				}

			} else {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
				errorList.add(error);
			}
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
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			credentialIssueResponseWrapper
					.setResponsetime(localdatetime);
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

	/**
	 * Returns the current processing status of a credential request.
	 *
	 * @param requestId credential request identifier
	 * @return MOSIP response wrapper with status code, data-share URL (if issued), and UIN/VID id; or service errors; never throws
	 */
	@Override
	public ResponseWrapper<CredentialIssueStatusResponse> getCredentialRequestStatus(String requestId) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started getting  credential status");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueStatusResponseWrapper = new ResponseWrapper<CredentialIssueStatusResponse>();

		CredentialIssueStatusResponse credentialIssueStatusResponse = new CredentialIssueStatusResponse();
		try {
			Optional<CredentialEntity> entity = credentialDao.findById(requestId);
			if (entity.isPresent()) {
				CredentialEntity credentialEntity = entity.get();
				CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credentialEntity.getRequest(),
						CredentialIssueRequestDto.class);

				credentialIssueStatusResponse.setId(credentialIssueRequestDto.getId());
				credentialIssueStatusResponse.setRequestId(requestId);
				credentialIssueStatusResponse.setStatusCode(credentialEntity.getStatusCode());
				credentialIssueStatusResponse.setUrl(credentialEntity.getDataShareUrl());
				LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CANCEL_CREDENTIAL,
						"get credential status of " + requestId);
			} else {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
				errorList.add(error);

			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended getting  credential status");
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
		} finally {
			credentialIssueStatusResponseWrapper.setId(EnvUtil.getCredReqServiceId());
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			credentialIssueStatusResponseWrapper
					.setResponsetime(localdatetime);
			credentialIssueStatusResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialIssueStatusResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueStatusResponseWrapper.setResponse(credentialIssueStatusResponse);
			}

		}
		return credentialIssueStatusResponseWrapper;
	}

	/**
	 * Updates credential request status from a partner WebSub callback (e.g. print partner acknowledgment).
	 * <p>
	 * Sets status and optional data-share URL; uses {@link #PRINT_USER} as the updater principal.
	 * </p>
	 *
	 * @param credentialStatusEvent WebSub payload containing requestId, status, and optional URL
	 * @throws CredentialRequestGeneratorException if the request ID is not found or persistence fails
	 */
	@Override
	public void updateCredentialStatus(CredentialStatusEvent credentialStatusEvent) throws CredentialRequestGeneratorException {
		String requestId=null;
		try {

			Event event=credentialStatusEvent.getEvent();
			requestId=credentialStatusEvent.getEvent().getRequestId();
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"started updating  credential status");
			Optional<CredentialEntity> entity = credentialDao.findById(requestId);
			if (entity.isPresent()) {
				CredentialEntity credentialEntity = entity.get();
				credentialEntity.setStatusCode(event.getStatus());
				if(!StringUtils.isEmpty(event.getUrl())) {
					credentialEntity.setDataShareUrl(event.getUrl());
				}
				credentialEntity.setUpdateDateTime(DateUtils2.getUTCCurrentDateTime(EnvUtil.getDateTimePattern()));
				credentialEntity.setUpdatedBy(PRINT_USER);
				credentialEntity.setStatusComment("updated the status from partner");
				credentialDao.save(credentialEntity);
				LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CANCEL_CREDENTIAL,
						"updated the status of  " + requestId);
			} else {

				throw new CredentialRequestGeneratorException();
			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended updating  credential status");
			audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,"update the request");
		}catch (DataAccessLayerException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, UPDATE_STATUS_CREDENTIAL,
					ExceptionUtils.getStackTrace(e));
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,e);

			throw new CredentialRequestGeneratorException();

		}

	}

	/**
	 * Returns a paginated list of credential request IDs filtered by status and optional effective datetime.
	 *
	 * @param statusCode       credential status to filter (e.g. {@code NEW}, {@code ISSUED})
	 * @param effectivedtimes  optional ISO datetime lower bound for {@code updateDateTime}; empty for no filter
	 * @param pageNumber       zero-based page index
	 * @param pageSize         number of rows per page
	 * @param sortBy           entity field name to sort on
	 * @param direction        sort direction ({@code ASC} or {@code DESC})
	 * @return MOSIP response wrapper with {@link PageDto} of request summaries or service errors; never throws
	 */
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
				LocalDateTime effectiveDateTime=DateUtils2.parseToLocalDateTime(effectivedtimes);
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
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			credentialRequestIdsResponseWrapper.setResponsetime(localdatetime);
			credentialRequestIdsResponseWrapper.setVersion(EnvUtil.getCredReqServiceVersion());
			if (!errorList.isEmpty()) {
				credentialRequestIdsResponseWrapper.setErrors(errorList);
			} else {
				credentialRequestIdsResponseWrapper.setResponse(pageDto);
			}

		}
		return credentialRequestIdsResponseWrapper;
	}

	/**
	 * Retriggers a failed or stuck credential request and issues it synchronously.
	 *
	 * @param requestId credential request identifier to retrigger
	 * @return MOSIP response wrapper with updated request details or service errors; never throws
	 */
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
				credentialEntity.setUpdateDateTime(DateUtils2.getUTCCurrentDateTime(EnvUtil.getDateTimePattern()));
				credentialEntity.setUpdatedBy(IdRepoSecurityManager.getUser());
				credentialEntity.setStatusComment("retrigger the request");
				CredentialIssueRequestDto credentialIssueRequestDto = mapper
						.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class);
				credentialDao.save(credentialEntity);
				credentialIssuanceProcessor.issueByRequestId(requestId);
				credentialIssueResponse = new CredentialIssueResponse();
				credentialIssueResponse.setId(credentialIssueRequestDto.getId());
				credentialIssueResponse.setRequestId(requestId);

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
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			credentialIssueResponseWrapper.setResponsetime(localdatetime);
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

	/**
	 * Posts an audit event to the kernel audit manager (used for WebSub status updates).
	 *
	 * @param module  audit module identifier
	 * @param event   audit event type
	 * @param id      subject identifier (hashed before transmission)
	 * @param idType  subject ID type
	 * @param desc    human-readable audit description
	 */
	public void audit(AuditModules module, AuditEvents event, String id, IdType idType, String desc) {
		RequestWrapper<AuditRequestDTO> auditRequest = auditBuilder.buildRequest(module, event,
				securityManager.hash(id.getBytes()), idType, desc);
		HttpEntity<RequestWrapper<AuditRequestDTO>> httpEntity = new HttpEntity<>(auditRequest);
		try {
			restUtil.postApi(ApiName.KERNELAUDITMANAGER, null, id, desc, MediaType.APPLICATION_JSON, httpEntity,
					AuditResponseDTO.class);
		} catch (IdRepoDataValidationException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		}
	}
}
