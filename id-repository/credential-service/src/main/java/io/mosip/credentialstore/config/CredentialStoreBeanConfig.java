package io.mosip.credentialstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.provider.impl.CredentialDefaultProvider;
import io.mosip.credentialstore.provider.impl.IdAuthProvider;
import io.mosip.credentialstore.repositary.UinHashSaltRepo;
import io.mosip.credentialstore.util.CredentialFormatterMapperUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;


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

	

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}

	@Bean
	public CredentialFormatterMapperUtil getCredentialFormatterMapperUtil() {
		return new CredentialFormatterMapperUtil();
	}

	@Bean
	public AuditHelper getAuditHelper() {
		return new AuditHelper();
		
	}
}
