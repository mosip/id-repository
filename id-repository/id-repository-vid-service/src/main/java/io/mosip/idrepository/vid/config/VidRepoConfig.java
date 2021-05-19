package io.mosip.idrepository.vid.config;

import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_DRIVER_CLASS_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_PASSWORD;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_USERNAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;

/**
 * The Class Vid Repo Config.
 * 
 * 
 * @author Manoj SP
 * @author Prem Kumar
 *
 */
@Configuration
@ConfigurationProperties("mosip.idrepo.vid")
@EnableAsync
public class VidRepoConfig extends HibernateDaoConfig {

	/** The env. */
	@Autowired
	private Environment env;

	/** The Interceptor. */
	@Autowired
	private Interceptor interceptor;

	/** The id. */
	private Map<String, String> id;

	/** The status. */
	private List<String> allowedStatus;

	/**
	 * Sets the id.
	 *
	 * @param id the id
	 */
	public void setId(Map<String, String> id) {
		this.id = id;
	}

	/**
	 * Sets the status.
	 *
	 * @param status the status
	 */
	public void setAllowedStatus(List<String> status) {
		this.allowedStatus = status;
	}

	/**
	 * Id.
	 *
	 * @return the map
	 */
	@Bean
	public Map<String, String> id() {
		return Collections.unmodifiableMap(id);
	}

	/**
	 * Status.
	 *
	 * @return the map
	 */
	@Bean
	public List<String> allowedStatus() {
		return Collections.unmodifiableList(allowedStatus);
	}
	
	@Bean
	public CredentialServiceManager credentialServiceManager() {
		return new CredentialServiceManager();
	}
	
	@Bean
	public DummyPartnerCheckUtil dummyPartnerCheckUtil() {
		return new DummyPartnerCheckUtil();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.core.dao.config.BaseDaoConfig#jpaProperties()
	 */
	@Override
	public Map<String, Object> jpaProperties() {
		Map<String, Object> jpaProperties = super.jpaProperties();
		jpaProperties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
		jpaProperties.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
		jpaProperties.put("hibernate.ejb.interceptor", interceptor);
		jpaProperties.replace("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect");
		return jpaProperties;
	}

	/**
	 * Builds the data source.
	 *
	 * @return the data source
	 */
	@Override
	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(env.getProperty(VID_DB_URL));
		dataSource.setUsername(env.getProperty(VID_DB_USERNAME));
		dataSource.setPassword(env.getProperty(VID_DB_PASSWORD));
		dataSource.setDriverClassName(env.getProperty(VID_DB_DRIVER_CLASS_NAME));
		return dataSource;
	}	
	
	@Bean("withSecurityContext")
	public DelegatingSecurityContextAsyncTaskExecutor taskExecutor() {
		return new DelegatingSecurityContextAsyncTaskExecutor(threadPoolTaskExecutor());
	}

	private ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(3);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("idrepo-");
		executor.initialize();
		return executor;
	}
}