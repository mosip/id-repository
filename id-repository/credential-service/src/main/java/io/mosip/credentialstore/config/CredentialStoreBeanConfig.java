package io.mosip.credentialstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.provider.impl.CredentialDefaultProvider;
import io.mosip.credentialstore.provider.impl.IdAuthProvider;
import io.mosip.credentialstore.provider.impl.PinBasedProvider;
import io.mosip.credentialstore.util.CredentialFormatterMapperUtil;
import io.mosip.credentialstore.util.RestUtil;


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
	 * Gets the pin based provider.
	 *
	 * @return the pin based provider
	 */
	@Bean("pin")
		public CredentialProvider getPinBasedProvider() {

			return new PinBasedProvider();
		
	}

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}

	@Bean
	public CredentialFormatterMapperUtil getCredentialFormatterMapperUtil() {
		return new CredentialFormatterMapperUtil();
	}

}
