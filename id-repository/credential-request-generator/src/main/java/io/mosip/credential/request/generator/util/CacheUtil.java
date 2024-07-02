package io.mosip.credential.request.generator.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CacheUtil {
	
    @Autowired
    CacheManager cacheManager;
    
	  @CachePut(cacheNames = IdRepoConstants.CREDENTIAL_TRANSACTION_CACHE, key = "#requestId")
	  public CredentialIssueStatusResponse updateCredentialTransaction(String requestId, CredentialEntity credentialEntity, String id) {
		  
	      return createCredentialIssueStatusResponse(requestId, credentialEntity, id);
	  }
	  
	  @CachePut(cacheNames = IdRepoConstants.CREDENTIAL_TRANSACTION_CACHE, key = "#requestId")
	  public CredentialIssueStatusResponse updateCredentialTransaction(String requestId, CredentialIssueStatusResponse credentialIssueStatusResponse) {
		  
	      return credentialIssueStatusResponse;
	  }
	  
	  
	  public CredentialIssueStatusResponse createCredentialIssueStatusResponse(String requestId, CredentialEntity credentialEntity, String id) {
		  CredentialIssueStatusResponse credentialIssueStatusResponse = new CredentialIssueStatusResponse();
		  credentialIssueStatusResponse.setId(id);
		  credentialIssueStatusResponse.setRequestId(requestId);
		  credentialIssueStatusResponse.setStatusCode(credentialEntity.getStatusCode());
		  credentialIssueStatusResponse.setUrl(credentialEntity.getDataShareUrl());
	      return credentialIssueStatusResponse;
	  }
	  
	  public CredentialIssueStatusResponse getCredentialTransaction(String requestId) {
	        return cacheManager.getCache(IdRepoConstants.CREDENTIAL_TRANSACTION_CACHE).get(requestId, CredentialIssueStatusResponse.class); //NOSONAR getCache() will not be returning null here.
	    }

}
