package io.mosip.idrepository.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback Spring cache configuration using an in-memory
 * {@link ConcurrentMapCacheManager}.
 *
 * <p>
 * Provides a minimal {@link CacheManager} when no other implementation is present on
 * the application context. In the consolidated single-JVM deployable, the preferred
 * manager is typically a Caffeine-backed bean from
 * {@code io.mosip.idrepository.config.IdRepoLibraryConfig#cacheManager()}. This class
 * exists so lightweight, test, or partial scan contexts still get caching enabled
 * without a bean-definition clash.
 * </p>
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Enable Spring's {@link EnableCaching} infrastructure for core and identity
 *       {@code @Cacheable} methods (for example {@code id_attributes} on
 *       {@link io.mosip.idrepository.core.security.IdRepoSecurityManager})</li>
 *   <li>Register only when no {@link CacheManager} bean already exists
 *       ({@link ConditionalOnMissingBean})</li>
 *   <li>Avoid duplicate {@code cacheManager} definitions when the service-module
 *       library config is loaded</li>
 * </ul>
 *
 * <h2>Beans / wiring</h2>
 * <table border="1" summary="Beans defined by CacheConfig">
 *   <tr><th>Bean</th><th>Type</th><th>Condition</th></tr>
 *   <tr>
 *     <td>{@link #cacheManager()}</td>
 *     <td>{@link ConcurrentMapCacheManager}</td>
 *     <td>No other {@link CacheManager} on the context</td>
 *   </tr>
 * </table>
 * <p>
 * Imported / scanned from service HTTP-mode configuration (for example
 * {@code HttpModeScanConfiguration}) alongside {@link IdRepoDataSourceConfig}.
 * </p>
 *
 * <h2>Multi-datasource / salt notes</h2>
 * <p>
 * This class does not create datasources or salt repositories. Cache regions used by
 * crypto helpers (such as {@code id_attributes}) store derived hash attributes only;
 * salt rows themselves live in PU1 {@code mosip_idrepo} /
 * {@link io.mosip.idrepository.core.repository.UinHashSaltRepo} and must not be confused
 * with idmap VID salts.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Prefer Caffeine from IdRepoLibraryConfig in production.
 * // CacheConfig activates only when that (or another) CacheManager is absent:
 * &#64;Cacheable("id_attributes")
 * public Map&lt;String, String&gt; getIdHashAndAttributes(...) { ... }
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Service module scan configs that include this class for HTTP / test boots</li>
 *   <li>Any {@code @Cacheable} / {@code @CacheEvict} bean that needs a manager when
 *       Caffeine is not configured</li>
 * </ul>
 *
 * @see ConcurrentMapCacheManager
 * @see CacheManager
 * @see IdRepoDataSourceConfig
 * @see io.mosip.idrepository.core.security.IdRepoSecurityManager
 */
@Configuration
@EnableCaching
public class CacheConfig {

	/**
	 * Default in-memory {@link CacheManager} for lightweight or test contexts.
	 * <p>
	 * Creates an unbounded concurrent-map backed manager. Suitable for unit/integration
	 * tests and partial application contexts; production consolidated deployments should
	 * supply a sized Caffeine manager instead so this bean is skipped.
	 * </p>
	 *
	 * @return concurrent map cache manager registered as the sole {@link CacheManager}
	 *         when no other exists
	 */
	@Bean
	@ConditionalOnMissingBean(CacheManager.class)
	public ConcurrentMapCacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}
}
