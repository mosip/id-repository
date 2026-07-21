package io.mosip.idrepository.credential.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;

import io.mosip.idrepository.credential.store.provider.CredentialProvider;
import io.mosip.idrepository.credential.store.provider.impl.IdAuthProvider;
import io.mosip.idrepository.credential.store.provider.impl.QrCodeProvider;
import io.mosip.idrepository.credential.store.provider.impl.VerCredProvider;
import io.mosip.idrepository.credential.store.util.CredentialStoreRestUtil;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;

/**
 * Legacy standalone credential-store bean definitions.
 * <p>
 * Wires credential format providers, outbound REST helpers, security, and Jackson tuning for
 * the pre-merge credential-service JVM. Superseded by
 * {@link io.mosip.idrepository.config.IdRepoLibraryConfig} in the consolidated deployable.
 * Retained for rollback and standalone credential-service runs where bean collisions are not a concern.
 * </p>
 *
 * @author Sowmya
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 * @see MvelConfig
 */
@Configuration
@EnableRetry
@PropertySource("classpath:bootstrap.properties")
public class CredentialStoreBeanConfig {

	/**
	 * Partner-policy bypass helper for environments without PMS integration.
	 *
	 * @return {@link DummyPartnerCheckUtil} bean (duplicate of core bean in merged JVM)
	 */
	@Bean
	public DummyPartnerCheckUtil dummyPartnerCheckUtil() {
		return new DummyPartnerCheckUtil();
	}

	/**
	 * Credential-store security manager for token and encryption operations.
	 * <p>
	 * In the merged deployable, the {@code @Primary} instance from
	 * {@link io.mosip.idrepository.core.security.IdRepoSecurityManager} replaces this bean.
	 * </p>
	 *
	 * @return {@link IdRepoSecurityManager} for standalone credential-service
	 */
	@Bean
	public IdRepoSecurityManager securityManager() {
		return new IdRepoSecurityManager();
	}

	/**
	 * IDA (ID Authentication) credential format provider.
	 *
	 * @return {@link IdAuthProvider} registered under bean name {@code idauth}
	 */
	@Bean("idauth")
	public CredentialProvider getIdAuthProvider() {
		return new IdAuthProvider();
	}

	/**
	 * Default credential format provider used when no partner-specific format is configured.
	 *
	 * @return base {@link CredentialProvider} registered under bean name {@code default}
	 */
	@Bean("default")
	public CredentialProvider getDefaultProvider() {
		return new CredentialProvider();
	}

	/**
	 * QR-code credential format provider.
	 *
	 * @return {@link QrCodeProvider} registered under bean name {@code qrcode}
	 */
	@Bean("qrcode")
	public CredentialProvider getQrCodeProvider() {
		return new QrCodeProvider();
	}

	/**
	 * Verifiable credential (VC) format provider.
	 *
	 * @return {@link VerCredProvider} registered under bean name {@code vercred}
	 */
	@Bean("vercred")
	public CredentialProvider getVerCredProvider() {
		return new VerCredProvider();
	}

	/**
	 * Outbound HTTP utility for credential-store external service calls.
	 * <p>
	 * Distinct from credential-request {@code CredReqRestUtil} to avoid RestUtil bean collisions.
	 * </p>
	 *
	 * @return {@link CredentialStoreRestUtil} for standalone credential-service
	 */
	@Bean
	public CredentialStoreRestUtil getRestUtil() {
		return new CredentialStoreRestUtil();
	}

	/**
	 * Audit event helper for credential issuance audit trails.
	 *
	 * @return {@link AuditHelper} for standalone credential-service
	 */
	@Bean
	public AuditHelper getAuditHelper() {
		return new AuditHelper();
	}

	/**
	 * Low-level REST helper wrapping kernel HTTP client utilities.
	 *
	 * @return {@link RestHelper} for standalone credential-service
	 */
	@Bean
	public RestHelper restHelper() {
		return new RestHelper();
	}
}
