package io.mosip.credentialstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.provider.impl.IdAuthProvider;
import io.mosip.credentialstore.provider.impl.QrCodeProvider;
import io.mosip.credentialstore.provider.impl.VerCredProvider;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;

/**
 * The Class CredentialStoreConfig.
 *
 * @author Sowmya
 */
@Configuration
@EnableRetry
@PropertySource("classpath:bootstrap.properties")
public class CredentialStoreBeanConfig {

	@Bean
	public DummyPartnerCheckUtil dummyPartnerCheckUtil() {
		return new DummyPartnerCheckUtil();
	}

	@Bean
	public IdRepoSecurityManager securityManager() {
		return new IdRepoSecurityManager();
	}

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

		return new CredentialProvider();
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

	/**
	 * Gets the qrCode provider.
	 *
	 * @return the default provider
	 */
	@Bean("vercred")
	public CredentialProvider getVerCredProvider() {

		return new VerCredProvider();
	}

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}

	@Bean
	public AuditHelper getAuditHelper() {
		return new AuditHelper();

	}

	@Bean
	public RestHelper restHelper() {
		return new RestHelper();
	}



	@Bean
	public AfterburnerModule afterburnerModule() {
		return new AfterburnerModule();
	}
}
