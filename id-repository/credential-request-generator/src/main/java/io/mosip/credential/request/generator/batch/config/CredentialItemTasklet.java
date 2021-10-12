package io.mosip.credential.request.generator.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.dao.CredentialDao;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.ApiNotAccessibleException;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


/**
 * @author Sowmya
 */
@Component
public class CredentialItemTasklet implements Tasklet {


	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RestUtil restUtil;

	/**
	 * The credentialDao.
	 */
	@Autowired
	private CredentialDao credentialDao;


	/**
	 * The Constant BIOMETRICS.
	 */
	private static final String PROCESS = "process";

	/**
	 * The Constant ID_REPO_SERVICE_IMPL.
	 */
	private static final String CREDENTIAL_ITEM_PROCESSOR = "CredentialItemProcessor";

	/**
	 * The Constant LOGGER.
	 */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemTasklet.class);

	private static final String CREDENTIAL_USER = "service-account-mosip-crereq-client";


	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, "CredentialItemTasklet",
				"Inside CredentialItemTasklet.execute() method");
		List<CredentialEntity> credentialEntities = credentialDao.getCredentials();

		credentialEntities.forEach(credential -> {
			try {
				LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
						"started processing item : " + credential.getCredentialId());
				CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class);
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
				String responseString = restUtil.postApi(ApiName.CRDENTIALSERVICE, null, "", "",
						MediaType.APPLICATION_JSON, credentialServiceRequestDto, String.class);

				CredentialServiceResponseDto responseObject = mapper.readValue(responseString, CredentialServiceResponseDto.class);

				if (responseObject != null &&
						responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
					LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
							responseObject.toString());

					credential.setStatusCode(CredentialStatusCode.FAILED.name());

				} else {
					CredentialServiceResponse credentialServiceResponse = responseObject.getResponse();
					credential.setCredentialId(credentialServiceResponse.getCredentialId());
					credential.setDataShareUrl(credentialServiceResponse.getDataShareUrl());
					credential.setIssuanceDate(credentialServiceResponse.getIssuanceDate());
					credential.setStatusCode(credentialServiceResponse.getStatus());
					credential.setSignature(credentialServiceResponse.getSignature());

				}
				credential.setUpdatedBy(CREDENTIAL_USER);
				LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
						"ended processing item : " + credential.getCredentialId());
			} catch (ApiNotAccessibleException e) {

				LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
						ExceptionUtils.getStackTrace(e));
				credential.setStatusCode("FAILED");
			} catch (IOException e) {

				LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
						ExceptionUtils.getStackTrace(e));
				credential.setStatusCode("FAILED");
			} catch (Exception e) {

				LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
						ExceptionUtils.getStackTrace(e));
				credential.setStatusCode("FAILED");
			} finally {
				credentialDao.update(credential);
			}
		});

		return RepeatStatus.FINISHED;
	}


}
