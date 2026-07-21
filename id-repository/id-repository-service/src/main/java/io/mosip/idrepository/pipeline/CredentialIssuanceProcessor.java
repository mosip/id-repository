package io.mosip.idrepository.pipeline;

import io.mosip.kernel.core.util.DateUtils2;
import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import io.mosip.idrepository.credential.request.constant.CredentialStatusCode;
import io.mosip.idrepository.credential.request.dao.CredentialDao;
import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.credential.request.helper.CredentialIssueRequestHelper;
import io.mosip.idrepository.credential.request.util.TrimExceptionMessage;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Synchronous credential issuance for a single {@code credential_transaction} row.
 * <p>
 * Replaces the former Spring Batch {@code CredentialItemTasklet} path: queue and issue
 * run in the same request thread with no background polling.
 * </p>
 */
@Component
public class CredentialIssuanceProcessor {

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialIssuanceProcessor.class);

	private static final String CREDENTIAL_USER = "service-account-mosip-crereq-client";

	@Autowired
	private CredentialIssueRequestHelper credentialIssueRequestHelper;

	@Autowired
	@Lazy
	private InProcessCredentialClient inProcessCredentialClient;

	@Autowired
	private CredentialDao credentialDao;

	@Autowired
	@Qualifier("credentialTransactionManager")
	private PlatformTransactionManager credentialTransactionManager;

	/**
	 * Loads a queued row by request id, issues the credential in-process, and persists the outcome.
	 * <p>
	 * Identity retrieval and credential issuance run outside the credential PU transaction so the
	 * idrepo Hibernate session can load lazy {@code Uin} collections.
	 * </p>
	 *
	 * @param requestId primary key of {@code credential_transaction}
	 */
	public void issueByRequestId(String requestId) {
		Optional<CredentialEntity> optional = credentialDao.findById(requestId);
		if (optional.isEmpty()) {
			LOGGER.warn(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "issueByRequestId",
					"No credential_transaction row for requestId=" + requestId);
			return;
		}
		CredentialEntity credential = optional.get();
		processEntity(credential);
		new TransactionTemplate(credentialTransactionManager).executeWithoutResult(status -> credentialDao.save(credential));
	}

	private void processEntity(CredentialEntity credential) {
		TrimExceptionMessage trimMessage = new TrimExceptionMessage();
		int retryCount = 0;
		try {
			LOGGER.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "processEntity",
					"Processing credential issuance for requestId=" + credential.getRequestId());
			CredentialIssueRequestDto credentialIssueRequestDto = credentialIssueRequestHelper
					.getCredentialIssueRequestDto(credential);
			CredentialServiceRequestDto credentialServiceRequestDto = credentialIssueRequestHelper
					.getCredentialServiceRequestDto(credentialIssueRequestDto, credential.getRequestId());

			CredentialServiceResponseDto responseObject = inProcessCredentialClient
					.issueCredential(credentialServiceRequestDto);

			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ErrorDTO error = responseObject.getErrors().get(0);
				credential.setStatusCode(CredentialStatusCode.FAILED.name());
				credential.setStatusComment(error.getMessage());
				retryCount = credential.getRetryCount() != null ? credential.getRetryCount() + 1 : 1;
			} else if (responseObject != null && responseObject.getResponse() != null) {
				CredentialServiceResponse credentialServiceResponse = responseObject.getResponse();
				credential.setCredentialId(credentialServiceResponse.getCredentialId());
				credential.setDataShareUrl(credentialServiceResponse.getDataShareUrl());
				credential.setIssuanceDate(credentialServiceResponse.getIssuanceDate());
				credential.setStatusCode(credentialServiceResponse.getStatus());
				credential.setSignature(credentialServiceResponse.getSignature());
				credential.setStatusComment("credentials issued to partner");
			}
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "processEntity",
					ExceptionUtils.getStackTrace(e));
			credential.setStatusCode(CredentialStatusCode.FAILED.name());
			credential.setStatusComment(trimMessage.trimExceptionMessage(e.getMessage()));
			retryCount = credential.getRetryCount() != null ? credential.getRetryCount() + 1 : 1;
		} catch (Exception e) {
			String errorMessage;
			if (e.getCause() instanceof HttpClientErrorException httpClientException) {
				errorMessage = httpClientException.getResponseBodyAsString();
			} else if (e.getCause() instanceof HttpServerErrorException httpServerException) {
				errorMessage = httpServerException.getResponseBodyAsString();
			} else {
				errorMessage = e.getMessage();
			}
			LOGGER.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "processEntity",
					ExceptionUtils.getStackTrace(e));
			credential.setStatusCode(CredentialStatusCode.FAILED.name());
			credential.setStatusComment(trimMessage.trimExceptionMessage(errorMessage));
			retryCount = credential.getRetryCount() != null ? credential.getRetryCount() + 1 : 1;
		}
		credential.setUpdatedBy(CREDENTIAL_USER);
		credential.setUpdateDateTime(DateUtils2.getUTCCurrentDateTime());
		if (retryCount != 0) {
			credential.setRetryCount(retryCount);
		}
	}
}
