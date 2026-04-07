package io.mosip.credentialstore.config;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.github.benmanes.caffeine.cache.Caffeine;

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
@EnableAsync
public class CredentialStoreBeanConfig {

	// --- Thread Pool ---
	@Value("${credential.service.executor.core-pool-size:10}")
	private int executorCorePoolSize;

	@Value("${credential.service.executor.max-pool-size:20}")
	private int executorMaxPoolSize;

	@Value("${credential.service.executor.queue-capacity:200}")
	private int executorQueueCapacity;

	@Value("${credential.service.executor.thread-name-prefix:cred-async-}")
	private String executorThreadNamePrefix;

	// --- Caffeine Cache (IDREPO_DATA) ---
	@Value("${credential.cache.idrepo.expire-after-write-minutes:5}")
	private long idrepoCacheExpireMinutes;

	@Value("${credential.cache.idrepo.maximum-size:500}")
	private long idrepoCacheMaxSize;

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

	@Bean("credentialServiceExecutor")
	public Executor credentialServiceExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(executorCorePoolSize);
		executor.setMaxPoolSize(executorMaxPoolSize);
		executor.setQueueCapacity(executorQueueCapacity);
		executor.setThreadNamePrefix(executorThreadNamePrefix);
		executor.initialize();
		return executor;
	}

	@Bean
	public CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(
				new ConcurrentMapCache("DATASHARE_POLICIES"),
				new ConcurrentMapCache("PARTNER_EXTRACTOR_FORMATS"),
				new ConcurrentMapCache("topics"),
				new CaffeineCache("IDREPO_DATA",
						Caffeine.newBuilder()
								.expireAfterWrite(idrepoCacheExpireMinutes, TimeUnit.MINUTES)
								.maximumSize(idrepoCacheMaxSize)
								.build())));
		return cacheManager;
	}
}