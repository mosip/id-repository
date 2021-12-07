package io.mosip.idrepository.core.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.entity.UinEncryptSalt;
import io.mosip.idrepository.core.entity.UinHashSalt;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;

@EnableAsync
@EnableJpaRepositories(basePackageClasses = { UinHashSaltRepo.class, UinEncryptSaltRepo.class,
		CredentialRequestStatusRepo.class })
@EnableCaching
public class IdRepoDataSourceConfig {

	@Autowired(required = false)
	private Interceptor interceptor;

	@Autowired
	private Environment env;

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
		jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect");
		jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE);
		jpaProperties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
		jpaProperties.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
		jpaProperties.put("hibernate.ejb.interceptor", interceptor);
		return jpaProperties;
	}

	/**
	 * Builds the data source.
	 *
	 * @param dataSourceValues the data source values
	 * @return the data source
	 */
	private DataSource buildDataSource(Map<String, String> dataSourceValues) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(dataSourceValues.get("url"));
		dataSource.setUsername(dataSourceValues.get("username"));
		dataSource.setPassword(dataSourceValues.get("password"));
		dataSource.setDriverClassName(dataSourceValues.get("driverClassName"));
		dataSource.setSchema("idrepo");
		return dataSource;
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
}
