package io.mosip.idrepository.core.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.entity.UinEncryptSalt;
import io.mosip.idrepository.core.entity.UinHashSalt;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.util.EnvUtil;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Primary JPA persistence-unit configuration for the {@code mosip_idrepo} database
 * (identity + shared core entities).
 *
 * <p>
 * Defines the {@code @Primary} datasource, entity-manager factory, and transaction
 * manager used by default {@code @Transactional} methods. Enables JPA repositories for
 * core salt, credential-request status, and handle tables. Identity-domain repositories
 * are added by the service-module extension
 * {@code io.mosip.idrepository.identity.config.IdRepoConfig}.
 * </p>
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Wire PU1 against PostgreSQL schema {@code idrepo} / database
 *       {@code mosip_idrepo}</li>
 *   <li>Scan identity and core entity packages and explicitly register salt / status /
 *       handle managed classes</li>
 *   <li>Expose a {@link RestRequestBuilder} covering all
 *       {@link RestServicesConstants} service names (may be superseded by a
 *       {@code @Primary} builder from library config in the consolidated JVM)</li>
 * </ul>
 *
 * <h2>Beans / wiring</h2>
 * <table border="1" summary="Primary beans from IdRepoDataSourceConfig">
 *   <tr><th>Bean name / method</th><th>Type</th><th>Role</th></tr>
 *   <tr>
 *     <td>{@code idRepoDataSource} / {@link #dataSource()}</td>
 *     <td>{@link HikariDataSource}</td>
 *     <td>Pooled JDBC to identity DB via {@link IdRepoHikariDataSourceFactory#identityPool}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #entityManagerFactory()}</td>
 *     <td>{@link LocalContainerEntityManagerFactoryBean}</td>
 *     <td>{@code @Primary} EMF; Hibernate + optional entity interceptor</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #transactionManager()}</td>
 *     <td>{@link JpaTransactionManager}</td>
 *     <td>{@code @Primary} TM for default {@code @Transactional}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getRestRequestBuilder()}</td>
 *     <td>{@link RestRequestBuilder}</td>
 *     <td>Outbound REST request templates for MOSIP services</td>
 *   </tr>
 * </table>
 * <p>
 * Optional injection: {@code idRepoEntityInterceptor} ({@link Interceptor}) for
 * identity crypto on entity load/save. Hibernate class-loader settings come from
 * {@link IdRepoHibernateJpaProperties#applyKernelAuthClassLoaderSettings(Map)}.
 * </p>
 *
 * <h2>Multi-datasource / salt notes</h2>
 * <p>
 * This config is <strong>PU1 only</strong>. Do not point VID or credential entities here:
 * </p>
 * <ul>
 *   <li>PU1 — {@code mosip_idrepo}: UIN, identity, {@link UinHashSalt},
 *       {@link UinEncryptSalt}, {@link CredentialRequestStatus}, {@link Handle}</li>
 *   <li>PU2 — {@code mosip_idmap}: VID + idmap salt tables (separate config)</li>
 *   <li>PU3 — {@code mosip_credential}: credential store + Spring Batch metadata</li>
 * </ul>
 * <p>
 * Salt routing: {@link UinHashSaltRepo} / {@link UinEncryptSaltRepo} on this PU must not
 * be mixed with idmap VID salt repositories. Mis-routing causes silent crypto failures.
 * Populate salt rows via the salt-generator Job after DB deploy — not via this HTTP
 * service.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Default transactions bind to this PU's transactionManager
 * &#64;Transactional
 * public void updateIdentity(...) { ... }
 *
 * // Datasource bean name for explicit wiring
 * &#64;Qualifier("idRepoDataSource") HikariDataSource ds;
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link UinHashSaltRepo}, {@link UinEncryptSaltRepo},
 *       {@link CredentialRequestStatusRepo}, {@link HandleRepo}</li>
 *   <li>Identity services / interceptors that use the primary EMF and
 *       {@code idRepoEntityInterceptor}</li>
 *   <li>Service {@code IdRepoConfig} / {@code HttpModeScanConfiguration} importing this
 *       class</li>
 *   <li>{@link io.mosip.idrepository.core.security.IdRepoSecurityManager} salt lookups
 *       against PU1 repositories</li>
 * </ul>
 *
 * @see IdRepoHikariDataSourceFactory
 * @see IdRepoHibernateJpaProperties
 * @see UinHashSaltRepo
 * @see UinEncryptSaltRepo
 * @see CredentialRequestStatusRepo
 * @see HandleRepo
 * @see EnvUtil
 * @see RestRequestBuilder
 */
@EnableAsync
@EnableJpaRepositories(basePackageClasses = { UinHashSaltRepo.class, UinEncryptSaltRepo.class,
		CredentialRequestStatusRepo.class, HandleRepo.class })
@EnableCaching
public class IdRepoDataSourceConfig {

	/**
	 * Optional Hibernate session interceptor for identity crypto (encrypt/decrypt on
	 * entity flush/load).
	 * <p>
	 * Injected by qualifier {@code idRepoEntityInterceptor} when present; may be
	 * {@code null} in contexts that do not register the interceptor bean.
	 * </p>
	 */
	@Autowired(required = false)
	@Qualifier("idRepoEntityInterceptor")
	private Interceptor interceptor;
	
	/**
	 * Environment property accessor used to build the identity Hikari pool
	 * ({@link IdRepoHikariDataSourceFactory#identityPool(EnvUtil)}).
	 */
	@Autowired
	private EnvUtil env;

	/**
	 * Creates the {@code @Primary} JPA {@link LocalContainerEntityManagerFactoryBean}
	 * for {@code mosip_idrepo}.
	 * <p>
	 * Scans {@code io.mosip.idrepository.identity.entity} and
	 * {@code io.mosip.idrepository.core.entity}, applies Hibernate vendor settings from
	 * {@link #additionalProperties()}, and post-processes the persistence unit to ensure
	 * {@link UinEncryptSalt}, {@link UinHashSalt}, {@link CredentialRequestStatus}, and
	 * {@link Handle} are managed even if package scanning misses them.
	 * </p>
	 *
	 * @return primary entity-manager factory bound to {@link #dataSource()}
	 */
	@Bean
	@Primary
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("io.mosip.idrepository.identity.entity", "io.mosip.idrepository.core.entity");

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaPropertyMap(additionalProperties());
		em.setPersistenceUnitPostProcessors(new PersistenceUnitPostProcessor() {

			@Override
			public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
				pui.addManagedClassName(UinEncryptSalt.class.getName());
				pui.addManagedClassName(UinHashSalt.class.getName());
				pui.addManagedClassName(CredentialRequestStatus.class.getName());
				pui.addManagedClassName(Handle.class.getName());
			}
		});
		return em;
	}

	/**
	 * Primary JPA transaction manager for {@code mosip_idrepo}.
	 * <p>
	 * Bound to {@link #entityManagerFactory()}. Default (unqualified)
	 * {@code @Transactional} methods use this manager. Credential PU3 work must use
	 * {@code @Transactional("credentialTransactionManager")} instead.
	 * </p>
	 *
	 * @return transaction manager bound to {@link #entityManagerFactory()}
	 */
	@Bean
	@Primary
	public JpaTransactionManager transactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return transactionManager;
	}

	/**
	 * Builds Hibernate JPA property map for the primary persistence unit.
	 * <p>
	 * Applies kernel-auth class-loader precedence via
	 * {@link IdRepoHibernateJpaProperties}, PostgreSQL dialect, naming strategies, and
	 * the optional {@link #interceptor} as
	 * {@code hibernate.session_factory.interceptor}.
	 * </p>
	 *
	 * @return mutable map of Hibernate / JPA properties applied to the EMF
	 */
	private Map<String, Object> additionalProperties() {
		Map<String, Object> jpaProperties = new HashMap<>();
		IdRepoHibernateJpaProperties.applyKernelAuthClassLoaderSettings(jpaProperties);
		jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE);
		jpaProperties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
		jpaProperties.put("hibernate.physical_naming_strategy", org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl.class.getName());
		jpaProperties.put("hibernate.session_factory.interceptor", interceptor);
		return jpaProperties;
	}

	/**
	 * Primary pooled datasource for {@code mosip_idrepo} (schema {@code idrepo}).
	 * <p>
	 * Delegates to {@link IdRepoHikariDataSourceFactory#identityPool(EnvUtil)} using
	 * JDBC and pool keys from {@link EnvUtil} / {@code IdRepoConstants}.
	 * </p>
	 *
	 * @return HikariCP datasource registered as bean {@code idRepoDataSource} and
	 *         marked {@code @Primary}
	 */
	@Bean("idRepoDataSource")
	@Primary
	public HikariDataSource dataSource() {
		return IdRepoHikariDataSourceFactory.identityPool(env);
	}
	
	/**
	 * Builds a {@link RestRequestBuilder} for all {@link RestServicesConstants} outbound
	 * service names.
	 * <p>
	 * In the consolidated deployable a {@code @Primary} builder from
	 * {@code IdRepoLibraryConfig} may supersede this bean; keep the method for core /
	 * partial contexts that still need REST template construction.
	 * </p>
	 *
	 * @return REST request builder initialized with every
	 *         {@link RestServicesConstants#getServiceName()} value
	 * @see RestServicesConstants
	 * @see RestRequestBuilder
	 */
	@Bean
	public RestRequestBuilder getRestRequestBuilder() {
		return new RestRequestBuilder(Arrays.stream(RestServicesConstants.values())
				.map(RestServicesConstants::getServiceName).collect(Collectors.toList()));
	}
}
