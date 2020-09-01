package io.mosip.credentialstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.PropertySource;


import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.provider.impl.CredentialDefaultProvider;
import io.mosip.credentialstore.provider.impl.IdAuthProvider;
import io.mosip.credentialstore.provider.impl.QrCodeProvider;


import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.helper.AuditHelper;



/**
 * The Class CredentialStoreConfig.
 *
 * @author Sowmya
 */
@Configuration
@PropertySource("classpath:bootstrap.properties")
public class CredentialStoreBeanConfig {

	
	/**
	 * Gets the id auth provider.
	 *
	 * @return the id auth provider
	 */
	@Bean("idauth")
	public CredentialProvider getIdAuthProvider() {

		return new IdAuthProvider();
	}

	/**
	 * Gets the default provider.
	 *
	 * @return the default provider
	 */
	@Bean("default")
	public CredentialProvider getDefaultProvider() {

		return new CredentialDefaultProvider();
	}
	/**
	 * Gets the qrCode provider.
	 *
	 * @return the default provider
	 */
	@Bean("qrcode")
	public CredentialProvider getQrCodeProvider() {

		return new QrCodeProvider();
	}
	

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}


	@Bean
	public AuditHelper getAuditHelper() {
		return new AuditHelper();
		
	}
}
