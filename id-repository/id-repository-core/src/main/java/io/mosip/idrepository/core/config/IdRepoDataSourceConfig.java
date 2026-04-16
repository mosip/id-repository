package io.mosip.idrepository.core.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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

@EnableAsync
@EnableJpaRepositories(basePackageClasses = { UinHashSaltRepo.class, UinEncryptSaltRepo.class,
		CredentialRequestStatusRepo.class, HandleRepo.class })
@EnableCaching
public class IdRepoDataSourceConfig {

	@Autowired(required = false)
	private Interceptor interceptor;
	
	@Autowired
	private EnvUtil env;

	/**
	 * Entity manager factory.
	 *
	 * @param dataSource the data source
	 * @return the local container entity manager factory bean
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("io.mosip.idrepository.*");

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

	@Bean
	public JpaTransactionManager transactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return transactionManager;
	}

	/**
	 * Additional properties.
	 *
	 * @return the properties
	 */
	private Map<String, Object> additionalProperties() {
		Map<String, Object> jpaProperties = new HashMap<>();
		jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE);
		jpaProperties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
		jpaProperties.put("hibernate.physical_naming_strategy", org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl.class.getName());
		jpaProperties.put("hibernate.session_factory.interceptor", interceptor);
		// Batch INSERT/UPDATE: sends up to 25 statements per round-trip instead of 1
		jpaProperties.put("hibernate.jdbc.batch_size", "25");
		jpaProperties.put("hibernate.order_inserts", "true");
		jpaProperties.put("hibernate.order_updates", "true");
		// Fetch 50 rows per round-trip when streaming result sets (default is 1 in many drivers)
		jpaProperties.put("hibernate.jdbc.fetch_size", "50");
		// Skip BEGIN on read-only transactions — avoids round-trip overhead when HikariCP manages autocommit
		jpaProperties.put("hibernate.connection.provider_disables_autocommit", "true");
		return jpaProperties;
	}

	/**
	 * Builds a pooled HikariCP data source.
	 *
	 * <p>Pool sizing is controlled by:
	 * <ul>
	 *   <li>{@code mosip.idrepo.db.pool.maximum-pool-size} (default 10)</li>
	 *   <li>{@code mosip.idrepo.db.pool.minimum-idle} (default 2)</li>
	 *   <li>{@code mosip.idrepo.db.pool.connection-timeout} (default 5000 ms)</li>
	 *   <li>{@code mosip.idrepo.db.pool.idle-timeout} (default 300000 ms)</li>
	 *   <li>{@code mosip.idrepo.db.pool.max-lifetime} (default 600000 ms)</li>
	 * </ul>
	 *
	 * @param dataSourceValues the data source values
	 * @return the data source
	 */
	private DataSource buildDataSource(Map<String, String> dataSourceValues) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dataSourceValues.get("url"));
		config.setUsername(dataSourceValues.get("username"));
		config.setPassword(dataSourceValues.get("password"));
		config.setDriverClassName(dataSourceValues.get("driverClassName"));
		config.setSchema("idrepo");
		config.setAutoCommit(false);
		config.setMaximumPoolSize(env.getProperty("mosip.idrepo.db.pool.maximum-pool-size", Integer.class, 60));
		config.setMinimumIdle(env.getProperty("mosip.idrepo.db.pool.minimum-idle", Integer.class, 10));
		config.setConnectionTimeout(env.getProperty("mosip.idrepo.db.pool.connection-timeout", Long.class, 30000L));
		config.setIdleTimeout(env.getProperty("mosip.idrepo.db.pool.idle-timeout", Long.class, 300000L));
		config.setMaxLifetime(env.getProperty("mosip.idrepo.db.pool.max-lifetime", Long.class, 600000L));
		// Log a stack trace if a connection is held longer than this threshold.
		// Helps identify which code path is holding connections during S3/REST I/O.
		config.setLeakDetectionThreshold(env.getProperty("mosip.idrepo.db.pool.leak-detection-threshold", Long.class, 20000L));
		return new HikariDataSource(config);
	}

	/**
	 * Data source.
	 *
	 * @return the data source
	 */
	@Bean("idRepoDataSource")
	public DataSource dataSource() {
//		If sharding is enabled, need to uncomment
//		return buildDataSource(db.get("shard"));
		
//		If sharding is enabled, need to comment below code
		Map<String, String> dbValues = new HashMap<>();
		dbValues.put("url", env.getProperty("mosip.idrepo.identity.db.url"));
		dbValues.put("username", env.getProperty("mosip.idrepo.identity.db.username"));
		dbValues.put("password", env.getProperty("mosip.idrepo.identity.db.password"));
		dbValues.put("driverClassName", env.getProperty("mosip.idrepo.identity.db.driverClassName"));
		return buildDataSource(dbValues);
	}
	
	@Bean
	public AfterburnerModule afterburnerModule() {
		return new AfterburnerModule();
	}
	
	@Bean
	public RestRequestBuilder getRestRequestBuilder() {
		return new RestRequestBuilder(Arrays.stream(RestServicesConstants.values())
				.map(RestServicesConstants::getServiceName).collect(Collectors.toList()));
	}
	
	
}
