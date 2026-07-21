package io.mosip.idrepository.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import io.mosip.idrepository.core.config.CacheConfig;
import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.credential.request.api.config.CredentialRequestGeneratorConfig;
import io.mosip.idrepository.credential.store.api.config.CredentialStoreConfig;
import io.mosip.idrepository.credential.store.config.CredentialStoreBeanConfig;

/**
 * Component-scan boundaries for HTTP (long-lived) deployment of ID-Repository.
 * <p>
 * Discovers REST controllers, domain {@code @Service} beans, kernel auth/WebSub adapters, and
 * commons utilities while excluding duplicate {@code @Configuration} classes already imported by
 * {@link io.mosip.idrepository.IdRepositoryBootApplication}. Salt-generator code
 * ({@code io.mosip.idrepository.saltgenerator.*}) is intentionally out of scope — it runs only in
 * the separate salt-generator module/Job chart.
 * </p>
 *
 * <h2>Scanned base packages</h2>
 * <ul>
 *   <li>{@code io.mosip.idrepository} — service + core types on the deployable classpath</li>
 *   <li>{@code io.mosip.kernel} — auth adapter, WebSub client shadows, websub API</li>
 *   <li>{@code io.mosip.commons} — shared MOSIP utilities</li>
 *   <li>{@code ${mosip.auth.adapter.impl.basepackage}} — typically {@code io.mosip.kernel.auth.defaultadapter}</li>
 * </ul>
 *
 * <h2>Excluded configurations</h2>
 * <p>
 * Assignable-type filters prevent double registration of datasource, cache, credential API configs,
 * legacy Swagger config, VID repo config, library config, and kernel {@code SecurityConfig}
 * (replaced by {@link KernelAuthSecurityConfig}). AspectJ filters drop unused kernel crypto
 * packages and khazana POSIX adapter from the scan.
 * </p>
 *
 * @see io.mosip.idrepository.IdRepositoryBootApplication
 * @see IdRepoLibraryConfig
 * @see KernelAuthSecurityConfig
 */
@Configuration
@ComponentScan(basePackages = {
		"io.mosip.idrepository",
		"io.mosip.kernel",
		"io.mosip.commons",
		"${mosip.auth.adapter.impl.basepackage}"
}, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = {
				"io.mosip.kernel.zkcryptoservice.*",
				"io.mosip.kernel.tokenidgenerator.*",
				"io.mosip.kernel.signature.*",
				"io.mosip.kernel.partnercertservice.*",
				"io.mosip.kernel.lkeymanager.*",
				"io.mosip.kernel.keymanagerservice.*",
				"io.mosip.kernel.keymanager.*",
				"io.mosip.kernel.keygenerator.*",
				"io.mosip.kernel.cryptomanager.*",
				"io.mosip.kernel.crypto.*",
				"io.mosip.kernel.clientcrypto.*",
				"io.mosip.commons.khazana.impl.PosixAdapter",
				"io.mosip.commons.khazana.util.*",
				"io.mosip.kernel.dataaccess.hibernate.config.*",
				"io.mosip.kernel.core.logger.config.SleuthLoggingAutoConfiguration",
				"io.mosip.kernel.auth.defaultadapter.helper..*"
		}),
		@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "io.mosip.idrepository.IdRepositoryBootApplication"),
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
				IdRepoDataSourceConfig.class,
				CacheConfig.class,
				CredentialStoreBeanConfig.class,
				CredentialStoreConfig.class,
				CredentialRequestGeneratorConfig.class,
				io.mosip.idrepository.identity.config.SwaggerConfig.class,
				io.mosip.idrepository.vid.config.VidRepoConfig.class,
				io.mosip.idrepository.vid.config.VidMvelConfig.class,
				IdRepoLibraryConfig.class,
				io.mosip.kernel.auth.defaultadapter.config.SecurityConfig.class
		})
})
public class HttpModeScanConfiguration {
}