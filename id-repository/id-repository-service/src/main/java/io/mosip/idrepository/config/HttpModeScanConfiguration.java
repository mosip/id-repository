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
 *   <li>{@code io.mosip.kernel.websub} — WebSub client (folded into kernel-core)</li>
 *   <li>{@code io.mosip.kernel.core.retry} / {@code io.mosip.kernel.core.util} — {@code @WithRetry} + {@code RetryUtil}</li>
 *   <li>{@code io.mosip.kernel.idvalidator.uin|vid|rid} — identity validators</li>
 *   <li>{@code io.mosip.kernel.idobjectvalidator} — identity JSON schema validator</li>
 *   <li>{@code io.mosip.kernel.cbeffutil} — CBEFF XML helper</li>
 *   <li>{@code io.mosip.kernel.biosdk} — BioAPI factory for biometric extraction</li>
 *   <li>{@code io.mosip.commons.khazana} — object-store adapters (S3/MinIO)</li>
 *   <li>{@code ${mosip.auth.adapter.impl.basepackage}} — typically {@code io.mosip.kernel.auth.defaultadapter}</li>
 * </ul>
 * <p>
 * Do not scan {@code io.mosip.kernel} as a whole. {@code kernel-core} ships {@code @Component}
 * types such as Orika {@code DataMapperImpl} (constructor takes source/destination {@code Class},
 * not Spring beans) and ID generators that need kernel JPA repositories this service does not
 * provide. Unused kernel-core auto-configurations are listed in
 * {@code spring.autoconfigure.exclude}.
 * </p>
 *
 * <h2>Excluded configurations</h2>
 * <p>
 * Assignable-type filters prevent double registration of datasource, cache, credential API configs,
 * legacy Swagger config, VID repo config, library config, and kernel {@code SecurityConfig}
 * (replaced by {@link KernelAuthSecurityConfig}). Auth helpers are registered by
 * {@link IdRepoKernelAuthHelperConfig}; khazana POSIX adapter is not used locally (S3/MinIO).
 * </p>
 *
 * @see io.mosip.idrepository.IdRepositoryBootApplication
 * @see IdRepoLibraryConfig
 * @see KernelAuthSecurityConfig
 */
@Configuration
@ComponentScan(basePackages = {
		"io.mosip.idrepository",
		"io.mosip.kernel.websub",
		"io.mosip.kernel.core.retry",
		"io.mosip.kernel.core.util",
		"io.mosip.kernel.idvalidator.uin",
		"io.mosip.kernel.idvalidator.vid",
		"io.mosip.kernel.idvalidator.rid",
		"io.mosip.kernel.idobjectvalidator",
		"io.mosip.kernel.cbeffutil",
		"io.mosip.kernel.biosdk",
		"io.mosip.commons.khazana",
		"${mosip.auth.adapter.impl.basepackage}"
}, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = {
				"io.mosip.commons.khazana.impl.PosixAdapter",
				"io.mosip.commons.khazana.util.*",
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
