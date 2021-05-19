package io.mosip.idrepository.core.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;

@EnableTransactionManagement
@EnableAsync
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.*")
public class IdRepoDataSourceConfig {

	@Autowired(required = false)
	private Interceptor interceptor;

	@Autowired
	private Environment env;
	
	@Bean
	public CredentialServiceManager credentialServiceManager() {
		return new CredentialServiceManager();
	}
	
	@Bean
	public DummyPartnerCheckUtil dummyPartnerCheckUtil() {
		return new DummyPartnerCheckUtil();
	}

	/**
	 * Entity manager factory.
	 *
	 * @param dataSource the data source
	 * @return the local container entity manager factory bean
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan("io.mosip.idrepository.*");

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaPropertyMap(additionalProperties());

		return em;
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
	
	@Bean("asyncThreadPoolTaskExecutor")
	public TaskExecutor getAsyncExecutor() {
	  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	  executor.setCorePoolSize(20);
	  executor.setMaxPoolSize(1000);
	  executor.setWaitForTasksToCompleteOnShutdown(true);
	  executor.setThreadNamePrefix("Async-");
	  executor.initialize(); // this is important, otherwise an error is thrown
	  return new DelegatingSecurityContextAsyncTaskExecutor(executor); // use this special TaskExecuter
	}

}
