package io.mosip.credentialstore.config;

import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.provider.impl.IdAuthProvider;
import io.mosip.credentialstore.provider.impl.QrCodeProvider;
import io.mosip.credentialstore.provider.impl.VerCredProvider;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.helper.AuditHelper;



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

	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${credential.service.mvel.file}")
	private String mvelFile;

	@Autowired
	private RestTemplate restTemplate;

	@Bean("varres")
	public VariableResolverFactory getVariableResolverFactory() {
		String mvelExpression = restTemplate.getForObject(configServerFileStorageURL + mvelFile, String.class);
		VariableResolverFactory functionFactory = new MapVariableResolverFactory();
		MVEL.eval(mvelExpression, functionFactory);
		return functionFactory;
	}
}
