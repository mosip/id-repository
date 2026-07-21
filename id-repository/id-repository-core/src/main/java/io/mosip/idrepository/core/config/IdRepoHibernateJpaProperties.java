package io.mosip.idrepository.core.config;

import java.util.Map;

/**
 * Shared Hibernate JPA property helpers for all persistence units in the consolidated
 * ID-Repository service.
 *
 * <p>
 * Applies settings that must be identical on PU1 ({@code mosip_idrepo}), PU2
 * ({@code mosip_idmap}), and PU3 ({@code mosip_credential}) so Hibernate SPI discovery
 * behaves correctly under the kernel-auth class-loader filter used by the launcher.
 * </p>
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Avoid Hibernate {@code ServiceLoader} scanning a mismatched thread-context
 *       class loader (TCCL) when kernel-auth has installed a different application
 *       class loader</li>
 *   <li>Prevent {@code BytecodeProvider} / {@code StrategyRegistrationProvider}
 *       {@code not a subtype} warnings and degraded Hibernate defaults</li>
 *   <li>Keep a single constant for the TCCL lookup-precedence property key</li>
 * </ul>
 *
 * <h2>Beans / wiring</h2>
 * <p>
 * Not a Spring bean. Call {@link #applyKernelAuthClassLoaderSettings(Map)} when building
 * each EMF's {@code jpaPropertyMap}, for example from:
 * </p>
 * <ul>
 *   <li>{@link IdRepoDataSourceConfig} (primary identity PU)</li>
 *   <li>Service-module credential / VID entity-manager factories</li>
 *   <li>{@code IdRepoLibraryConfig} credential EMF setup</li>
 * </ul>
 *
 * <h2>Multi-datasource / salt notes</h2>
 * <p>
 * These properties are class-loader / SPI related only. They do not select schemas or
 * salt tables. Each PU still needs its own datasource
 * ({@link IdRepoHikariDataSourceFactory}) and must keep idrepo vs idmap salt entities
 * on the correct persistence unit.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * Map&lt;String, Object&gt; jpaProperties = new HashMap&lt;&gt;();
 * IdRepoHibernateJpaProperties.applyKernelAuthClassLoaderSettings(jpaProperties);
 * // then add dialect, naming strategies, interceptors, etc.
 * em.setJpaPropertyMap(jpaProperties);
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link IdRepoDataSourceConfig#entityManagerFactory()}</li>
 *   <li>Credential-request / credential-store JPA configuration in the service module</li>
 *   <li>Any additional EMF that runs inside the same kernel-auth filtered JVM</li>
 * </ul>
 *
 * @see IdRepoDataSourceConfig
 * @see IdRepoHikariDataSourceFactory
 * @see org.hibernate.cfg.EnvironmentSettings#TC_CLASSLOADER
 */
public final class IdRepoHibernateJpaProperties {

	/**
	 * Hibernate property key controlling thread-context class-loader lookup precedence
	 * during SPI / service loading.
	 * <p>
	 * Set to {@code never} via {@link #applyKernelAuthClassLoaderSettings(Map)} so
	 * Hibernate does not prefer a TCCL that differs from the application class loader
	 * installed by the ID-Repository launcher (kernel-auth filter).
	 * </p>
	 *
	 * @see org.hibernate.cfg.EnvironmentSettings#TC_CLASSLOADER
	 * @see #applyKernelAuthClassLoaderSettings(Map)
	 */
	public static final String TCCL_LOOKUP_PRECEDENCE = "hibernate.classLoader.tccl_lookup_precedence";

	/**
	 * Prevents instantiation; use static helpers only.
	 */
	private IdRepoHibernateJpaProperties() {
	}

	/**
	 * Applies kernel-auth-safe Hibernate class-loader settings to a JPA property map.
	 * <p>
	 * Puts {@link #TCCL_LOOKUP_PRECEDENCE}{@code never} if absent
	 * ({@link Map#putIfAbsent}). Without this, Hibernate SPI {@code ServiceLoader}
	 * may scan the TCCL when it differs from the application class loader installed by
	 * {@code IdRepositoryLauncher} (kernel-auth filter), causing
	 * {@code BytecodeProvider} / {@code StrategyRegistrationProvider} {@code not a subtype}
	 * warnings and fallback to degraded defaults.
	 * </p>
	 *
	 * @param jpaProperties mutable map passed to
	 *                      {@code LocalContainerEntityManagerFactoryBean#setJpaPropertyMap};
	 *                      must not be {@code null}
	 * @throws NullPointerException if {@code jpaProperties} is {@code null}
	 * @see #TCCL_LOOKUP_PRECEDENCE
	 */
	public static void applyKernelAuthClassLoaderSettings(Map<String, Object> jpaProperties) {
		jpaProperties.putIfAbsent(TCCL_LOOKUP_PRECEDENCE, "never");
	}

}
