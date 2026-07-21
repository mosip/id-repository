package io.mosip.idrepository.credential.store.config;

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
 * Loads credential issuance MVEL masking expressions from the config server.
 * <p>
 * Downloads the MVEL script identified by {@link #mvelFile}, evaluates it into a
 * {@link VariableResolverFactory}, and exposes the result as bean {@code varres}. Credential
 * providers use this factory to mask or transform identity attributes before packaging credentials.
 * </p>
 * <p>
 * Imported via {@link io.mosip.idrepository.config.IdRepoLibraryConfig} in the consolidated
 * deployable. A parallel configuration exists for VID in
 * {@link io.mosip.idrepository.vid.config.VidMvelConfig}.
 * </p>
 *
 * @author Neha Farheen
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 * @see io.mosip.idrepository.vid.config.VidMvelConfig
 */
@Configuration
public class MvelConfig {

	/**
	 * Base URL of the config-server file storage endpoint used to fetch the MVEL script.
	 * Bound to {@link IdRepoConstants#CONFIG_SERVER_FILE_STORAGE_URI}.
	 */
	@Value("${" + IdRepoConstants.CONFIG_SERVER_FILE_STORAGE_URI + "}")
	private String configServerFileStorageURL;

	/**
	 * Relative path of the credential masking MVEL file on the config server.
	 * Bound to {@link IdRepoConstants#CREDENTIAL_SERVICE_MVEL_FILE}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_MVEL_FILE + "}")
	private String mvelFile;

	/**
	 * Plain (non–self-token) REST client for downloading the MVEL expression from config server.
	 */
	@Autowired
	@Qualifier("plainRestTemplate")
	private RestTemplate restTemplate;

	/**
	 * Downloads, evaluates, and registers the credential attribute masking MVEL expression.
	 * <p>
	 * The returned factory is shared across credential format providers for consistent
	 * attribute masking during issuance.
	 * </p>
	 *
	 * @return evaluated {@link VariableResolverFactory} registered as bean {@code varres}
	 */
	@Bean("varres")
	public VariableResolverFactory getVariableResolverFactory() {
		String mvelExpression = restTemplate.getForObject(configServerFileStorageURL + mvelFile, String.class);
		VariableResolverFactory functionFactory = new MapVariableResolverFactory();
		MVEL.eval(mvelExpression, functionFactory);
		return functionFactory;
	}
}
