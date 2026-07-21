package io.mosip.idrepository.vid.config;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Loads VID-specific MVEL masking expressions from the config server.
 * <p>
 * Exposes a {@link VariableResolverFactory} bean named {@code mask} used when
 * anonymizing VID-related identity attributes.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.config.MvelConfig
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 * @see VidRepoConfig
 */
@Configuration
public class VidMvelConfig {

	/** Base URI for config-server file storage. */
	@Value("${" + IdRepoConstants.CONFIG_SERVER_FILE_STORAGE_URI + "}")
	private String configServerFileStorageURL;

	/** Relative path to the MVEL masking script on the config server. */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_MVEL_FILE + "}")
	private String mvelFile;

	/** Plain (non-auth) REST client for config-server file fetch. */
	@Autowired
	@Qualifier("restTemplate")
	private RestTemplate restTemplate;

	/**
	 * Evaluates the MVEL mask script and returns the resolver factory.
	 *
	 * @return variable resolver factory bean {@code mask}
	 */
	@Bean("mask")
	/**
	 * Vid mask variable resolver factory.
	 * @return variable resolver factory
	 */
	public VariableResolverFactory vidMaskVariableResolverFactory() {
		String mvelExpression = restTemplate.getForObject(configServerFileStorageURL + mvelFile, String.class);
		VariableResolverFactory functionFactory = new MapVariableResolverFactory();
		MVEL.eval(mvelExpression, functionFactory);
		return functionFactory;
	}
}
