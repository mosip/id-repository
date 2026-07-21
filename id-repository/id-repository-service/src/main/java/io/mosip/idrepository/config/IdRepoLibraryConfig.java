package io.mosip.idrepository.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.retry.annotation.EnableRetry;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariDataSource;

import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor;
import io.mosip.idrepository.credential.request.util.CryptoUtil;
import io.mosip.idrepository.credential.store.config.MvelConfig;
import io.mosip.idrepository.credential.store.provider.CredentialProvider;
import io.mosip.idrepository.credential.store.provider.impl.IdAuthProvider;
import io.mosip.idrepository.credential.store.provider.impl.QrCodeProvider;
import io.mosip.idrepository.credential.store.provider.impl.VerCredProvider;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.config.IdRepoHibernateJpaProperties;
import io.mosip.idrepository.core.config.IdRepoHikariDataSourceFactory;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.jobs.IdRepoSchedulerConfiguration;
import io.mosip.idrepository.vid.config.VidMvelConfig;
import io.mosip.idrepository.vid.config.VidRepoConfig;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.config.IdentityWebsubConfig;
import io.mosip.idrepository.identity.config.IdRepoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;

/**
 * Central Spring configuration for the consolidated ID-Repository JVM.
 * <p>
 * Wires persistence, caching, outbound REST, credential format providers, and imports domain
 * configuration from identity, VID, credential store, and scheduler modules. Imported explicitly
 * by {@link io.mosip.idrepository.IdRepositoryBootApplication}; excluded from
 * {@link HttpModeScanConfiguration} to avoid duplicate bean definitions.
 * </p>
 *
 * <h2>Persistence units</h2>
 * <ul>
 *   <li>PU1 (primary) — {@code mosip_idrepo} via {@code IdRepoDataSourceConfig} in core (imported through {@code IdRepoConfig})</li>
 *   <li>PU2 — {@code mosip_idmap} via {@code VidRepoConfig}</li>
 *   <li>PU3 — {@code mosip_credential} via {@link #credentialDataSource()} and {@link #credentialEntityManagerFactory}</li>
 * </ul>
 *
 * <h2>Imported configurations</h2>
 * <ul>
 *   <li>{@code IdRepoConfig} — identity EMF, WebSub executor, security helpers</li>
 *   <li>{@code MvelConfig} / {@code VidMvelConfig} — demographic masking expressions</li>
 *   <li>{@code IdRepoSchedulerConfiguration} — partner OLV cache eviction scheduler</li>
 *   <li>{@code VidRepoConfig} — VID datasource and services</li>
 *   <li>{@code IdentityWebsubConfig} — publish/subscribe topics at {@code ApplicationReadyEvent}</li>
 * </ul>
 *
 * <h2>Cache regions</h2>
 * <p>
 * Partner-facing caches ({@code DATASHARE_POLICIES}, etc.) use {@code mosip.idrepo.partner.cache.ttl.minutes};
 * internal DB/salt caches use {@code mosip.idrepo.internal.cache.ttl.minutes}.
 * </p>
 *
 * @see io.mosip.idrepository.core.config.IdRepoDataSourceConfig
 * @see io.mosip.idrepository.identity.config.IdRepoConfig
 * @see io.mosip.idrepository.core.jobs.IdRepoSchedulerConfiguration
 * @see io.mosip.idrepository.vid.config.VidRepoConfig
 */
@Configuration
@EnableCaching
@EnableRetry
@Import({ IdRepoConfig.class, MvelConfig.class, IdRepoSchedulerConfiguration.class,
		VidRepoConfig.class, VidMvelConfig.class, IdentityWebsubConfig.class })
public class IdRepoLibraryConfig {

	/** Config-server property accessor for cache TTL and datasource URLs. */
	@Autowired
	private EnvUtil env;

	/**
	 * Primary Caffeine {@link org.springframework.cache.CacheManager} with per-region TTL.
	 * <p>
	 * External partner/PMS caches use a shorter expire-after-write; default cache spec applies
	 * to internal id-repo regions (salts, identity attributes, WebSub topic cache).
	 * </p>
	 *
	 * @return cache manager with custom partner regions registered
	 */
	@Bean
	@Primary
	public CaffeineCacheManager cacheManager() {
		int partnerTtlMinutes = Integer.parseInt(env.getProperty(IdRepoConstants.PARTNER_CACHE_TTL_MINUTES,
				String.valueOf(IdRepoConstants.PARTNER_CACHE_TTL_DEFAULT_MINUTES)));
		int internalTtlMinutes = Integer.parseInt(env.getProperty(IdRepoConstants.INTERNAL_CACHE_TTL_MINUTES,
				String.valueOf(IdRepoConstants.INTERNAL_CACHE_TTL_DEFAULT_MINUTES)));

		CaffeineCacheManager manager = new CaffeineCacheManager();
		manager.setAllowNullValues(false);

		Caffeine<Object, Object> partnerSpec = Caffeine.newBuilder()
				.maximumSize(5_000)
				.expireAfterWrite(partnerTtlMinutes, TimeUnit.MINUTES)
				.recordStats();
		for (String cacheName : IdRepoConstants.EXTERNAL_SERVICE_CACHE_NAMES) {
			manager.registerCustomCache(cacheName, partnerSpec.build());
		}

		manager.setCaffeine(Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(internalTtlMinutes, TimeUnit.MINUTES)
				.recordStats());
		return manager;
	}

	/**
	 * Outbound REST builder preloaded with every {@link RestServicesConstants} config key.
	 *
	 * @return primary {@link RestRequestBuilder} for audit, cryptomanager, PMS, etc.
	 */
	@Bean
	@Primary
	public RestRequestBuilder restRequestBuilder() {
		return new RestRequestBuilder(Arrays.stream(RestServicesConstants.values())
				.map(RestServicesConstants::getServiceName).collect(Collectors.toList()));
	}

	/**
	 * Credential-store HTTP client (PMS, datashare, keymanager paths).
	 * <p>
	 * Qualifier {@code credentialStoreRestUtil} distinguishes this from credreq {@code CredReqRestUtil}.
	 * </p>
	 *
	 * @return credential store REST utility bean
	 */
	@Bean("credentialStoreRestUtil")
	public io.mosip.idrepository.credential.store.util.CredentialStoreRestUtil credentialStoreRestUtil() {
		return new io.mosip.idrepository.credential.store.util.CredentialStoreRestUtil();
	}

	/** IDA-format credential provider ({@code @Qualifier("idauth")}). */
	@Bean("idauth")
	public CredentialProvider idAuthProvider() {
		return new IdAuthProvider();
	}

	/** Default JSON credential provider ({@code @Qualifier("default")}). */
	@Bean("default")
	public CredentialProvider defaultCredentialProvider() {
		return new CredentialProvider();
	}

	/** QR-code credential provider ({@code @Qualifier("qrcode")}). */
	@Bean("qrcode")
	public CredentialProvider qrCodeProvider() {
		return new QrCodeProvider();
	}

	/** Verifiable credential (VC) provider ({@code @Qualifier("vercred")}). */
	@Bean("vercred")
	public CredentialProvider verCredProvider() {
		return new VerCredProvider();
	}

	/**
	 * HikariCP pool for {@code mosip_credential} (PU3).
	 * <p>
	 * Pool sizes from {@code mosip.idrepo.credential.pool.*} via {@link IdRepoHikariDataSourceFactory}.
	 * </p>
	 *
	 * @return credential datasource bean
	 */
	@Bean(name = "credentialDataSource")
	public HikariDataSource credentialDataSource() {
		return IdRepoHikariDataSourceFactory.credentialPool(env);
	}

	/**
	 * JPA entity manager factory for credential entities on {@code mosip_credential}.
	 *
	 * @param credentialDataSource credential PU datasource
	 * @param credReqRestUtil      optional credreq REST client for the Hibernate interceptor
	 * @param cryptoUtil           optional crypto helper for the Hibernate interceptor
	 * @return credential persistence unit factory
	 */
	@Bean(name = "credentialEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean credentialEntityManagerFactory(
			@Qualifier("credentialDataSource") DataSource credentialDataSource,
			@Autowired(required = false) @Qualifier("credReqRestUtil") io.mosip.idrepository.credential.request.util.CredReqRestUtil credReqRestUtil,
			@Autowired(required = false) CryptoUtil cryptoUtil) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(credentialDataSource);
		em.setPackagesToScan(CredentialEntity.class.getPackage().getName());
		em.setPersistenceUnitName("credential");
		JpaVendorAdapter vendor = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendor);
		Map<String, Object> props = new HashMap<>();
		IdRepoHibernateJpaProperties.applyKernelAuthClassLoaderSettings(props);
		props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		props.put("hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE);
		if (credReqRestUtil != null && cryptoUtil != null) {
			props.put("hibernate.session_factory.interceptor",
					new CredentialTransactionInterceptor(credReqRestUtil, cryptoUtil));
		}
		em.setJpaPropertyMap(props);
		return em;
	}

	/**
	 * JPA transaction manager bound to the credential persistence unit (PU3).
	 *
	 * @param emf credential entity manager factory bean
	 * @return JPA transaction manager for {@code credentialTransactionManager}
	 */
	@Bean(name = "credentialTransactionManager")
	public JpaTransactionManager credentialTransactionManager(
			@Qualifier("credentialEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
		JpaTransactionManager tm = new JpaTransactionManager();
		tm.setEntityManagerFactory(emf.getObject());
		return tm;
	}

	/**
	 * Enables Spring Data JPA repositories on {@code io.mosip.idrepository.credential.request.repository}
	 * using PU3 beans ({@code credentialEntityManagerFactory}, {@code credentialTransactionManager}).
	 */
	@Configuration
	@EnableJpaRepositories(
			entityManagerFactoryRef = "credentialEntityManagerFactory",
			transactionManagerRef = "credentialTransactionManager",
			basePackages = "io.mosip.idrepository.credential.request.repository",
			repositoryBaseClass = HibernateRepositoryImpl.class)
	static class CredentialJpaConfig {
	}
}