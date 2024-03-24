package io.mosip.credential.request.generator.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CacheUtil {
	
    @Autowired
    CacheManager cacheManager;
    

	  @Cacheable(value = "credential_transaction", key = "#requestId")
	  public CredentialEntity setCredentialTransaction(String requestId, CredentialEntity credentialEntity) {
	      return credentialEntity;
	  }
    
	  @CacheEvict(value = "credential_transaction", key = "#requestId")
	  private void evictCredentialTransaction(String requestId, CredentialEntity credentialEntity) {
		  // TODO  This method doesn't need todo anything as of now. 
	      return ;
	  } 
	  
	  public void updateCredentialTransaction(String requestId, CredentialEntity credentialEntity) {
		  if(getCredentialTransaction(requestId)!=null) {
			  evictCredentialTransaction(requestId,credentialEntity);
			  setCredentialTransaction(requestId,credentialEntity);
		  }
		  else {
			  setCredentialTransaction(requestId,credentialEntity);
		  }
	  }
	  
	  public CredentialEntity getCredentialTransaction(String requestId) {
	        return cacheManager.getCache("credential_transaction").get(requestId, CredentialEntity.class);	//NOSONAR getCache() will not be returning null here.
	    }

}
