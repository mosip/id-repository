package io.mosip.credential.request.generator.util;

import io.mosip.credential.request.generator.constants.LoggerFileConstant;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CacheUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(CacheUtil.class);

    @Autowired
    CacheManager cacheManager;
    
	  @CachePut(cacheNames = IdRepoConstants.CREDENTIAL_TRANSACTION_CACHE, key = "#requestId")
	  public CredentialIssueStatusResponse updateCredentialTransaction(String requestId, CredentialEntity credentialEntity, String id) {
		  CredentialIssueStatusResponse credentialIssueStatusResponse = new CredentialIssueStatusResponse();
		  credentialIssueStatusResponse.setId(id);
		  credentialIssueStatusResponse.setRequestId(requestId);
		  credentialIssueStatusResponse.setStatusCode(credentialEntity.getStatusCode());
		  credentialIssueStatusResponse.setUrl(credentialEntity.getDataShareUrl());
		  LOGGER.info(LoggerFileConstant.SESSIONID.toString(), "CREDENTIAL_TRANSACTION_CACHE" + "requestId: " + requestId,
				  "statusCode: " + credentialEntity.getStatusCode());
	      return credentialIssueStatusResponse;
	  }
	  
	  public CredentialIssueStatusResponse getCredentialTransaction(String requestId) {
	        return cacheManager.getCache(IdRepoConstants.CREDENTIAL_TRANSACTION_CACHE).get(requestId, CredentialIssueStatusResponse.class); //NOSONAR getCache() will not be returning null here.
	    }

}
