package io.mosip.credential.request.generator.batch.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.constants.LoggerFileConstant;
import io.mosip.credential.request.generator.dao.CredentialDao;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.ApiNotAccessibleException;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.credential.request.generator.util.TrimExceptionMessage;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

@Component
public class CredentialItemReprocessTasklet implements Tasklet {
	
	@Value("${credential.batch.thread.count:10}")
	private int threadCount;

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestUtil restUtil;
	
	/**
	 * The credentialDao.
	 */
	@Autowired
	private CredentialDao credentialDao;

	/** The retry max count. */
	@Value("${credential.request.retry.max.count}")
	private int retryMaxCount;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemReprocessTasklet.class);
	
	private static final String CREDENTIAL_USER = "service-account-mosip-crereq-client";
	

	/**
	 * The Constant ID_REPO_SERVICE_IMPL.
	 */
	private static final String CREDENTIAL_ITEM_REPROCESS_TASKLET = "CredentialItemReprocessTasklet";
	
	ForkJoinPool forkJoinPool;

	@PostConstruct
	public void init() {
		forkJoinPool = new ForkJoinPool(threadCount);
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		String batchId = UUID.randomUUID().toString();
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
				"Inside CredentialItemReprocessTasklet.execute() method");
		List<CredentialEntity> credentialEntities = credentialDao.getCredentialsForReprocess(batchId);

		try {
			forkJoinPool.submit(() -> credentialEntities.parallelStream().forEach(credential -> {
				int retryCount = credential.getRetryCount() != null ? credential.getRetryCount() : 0;
				TrimExceptionMessage trimMessage = new TrimExceptionMessage();
				try {
					if ((CredentialStatusCode.FAILED.name().equalsIgnoreCase(credential.getStatusCode())
							&& (retryCount <= retryMaxCount))
							|| (CredentialStatusCode.RETRY.name().equalsIgnoreCase(credential.getStatusCode()))) {
						LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
								"started processing item : " + credential.getRequestId());
					CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credential.getRequest(),
							CredentialIssueRequestDto.class);

					CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
					credentialServiceRequestDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
					credentialServiceRequestDto.setId(credentialIssueRequestDto.getId());
					credentialServiceRequestDto.setIssuer(credentialIssueRequestDto.getIssuer());
					credentialServiceRequestDto.setRecepiant(credentialIssueRequestDto.getIssuer());
					credentialServiceRequestDto.setSharableAttributes(credentialIssueRequestDto.getSharableAttributes());
					credentialServiceRequestDto.setUser(credentialIssueRequestDto.getUser());
					credentialServiceRequestDto.setRequestId(credential.getRequestId());
					credentialServiceRequestDto.setEncrypt(credentialIssueRequestDto.isEncrypt());
					credentialServiceRequestDto.setEncryptionKey(credentialIssueRequestDto.getEncryptionKey());
					credentialServiceRequestDto.setAdditionalData(credentialIssueRequestDto.getAdditionalData());
					LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
							"Calling CRDENTIALSERVICE : " + credential.getRequestId());
					String responseString = restUtil.postApi(ApiName.CRDENTIALSERVICE, null, "", "", MediaType.APPLICATION_JSON,
							credentialServiceRequestDto, String.class);

					LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
							"Received response from CRDENTIALSERVICE : " + credential.getRequestId());
					CredentialServiceResponseDto responseObject = mapper.readValue(responseString,
							CredentialServiceResponseDto.class);

					if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
						LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
								credential.getRequestId(), responseObject.toString());
						ErrorDTO error = responseObject.getErrors().get(0);
						credential.setStatusCode(CredentialStatusCode.FAILED.name());
						credential.setStatusComment(error.getMessage());
							credential.setRetryCount(retryCount + 1);

					} else {
						CredentialServiceResponse credentialServiceResponse = responseObject.getResponse();
						credential.setCredentialId(credentialServiceResponse.getCredentialId());
						credential.setDataShareUrl(credentialServiceResponse.getDataShareUrl());
						credential.setIssuanceDate(credentialServiceResponse.getIssuanceDate());
						credential.setStatusCode(credentialServiceResponse.getStatus());
						credential.setSignature(credentialServiceResponse.getSignature());
						credential.setStatusComment("credentials issued to partner");

					}

					} else {
						credential.setStatusCode(CredentialStatusCode.FAILED.name());
						credential.setStatusComment(CredentialRequestErrorCodes.RETRY_COUNT_EXCEEDED.getErrorMessage());
					}
					credential.setUpdatedBy(CREDENTIAL_USER);
					credential.setUpdateDateTime(DateUtils.getUTCCurrentDateTime());
					LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
							"ended processing item : " + credential.getRequestId());
				} catch (ApiNotAccessibleException e) {

					LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
							ExceptionUtils.getStackTrace(e));
					credential.setStatusCode("FAILED");
					credential.setStatusComment(trimMessage.trimExceptionMessage(e.getMessage()));
					credential.setRetryCount(retryCount + 1);
				} catch (IOException e) {

					LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
							ExceptionUtils.getStackTrace(e));
					credential.setStatusCode("FAILED");
					credential.setStatusComment(trimMessage.trimExceptionMessage(e.getMessage()));
					credential.setRetryCount(retryCount + 1);
				} catch (Exception e) {
					String errorMessage;
					if (e.getCause() instanceof HttpClientErrorException) {
						HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
						errorMessage = httpClientException.getResponseBodyAsString();
					} else if (e.getCause() instanceof HttpServerErrorException) {
						HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
						errorMessage = httpServerException.getResponseBodyAsString();
					} else {
						errorMessage = e.getMessage();
					}

					LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
							ExceptionUtils.getStackTrace(e));
					credential.setStatusCode("FAILED");
					credential.setStatusComment(trimMessage.trimExceptionMessage(errorMessage));
					credential.setRetryCount(retryCount + 1);
				}
			})).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_REPROCESS_TASKLET, "batchid = " + batchId,
						ExceptionUtils.getStackTrace(e));
		}
		if (!CollectionUtils.isEmpty(credentialEntities))
			credentialDao.update(batchId, credentialEntities);

		return RepeatStatus.FINISHED;
	}
}