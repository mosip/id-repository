package io.mosip.idrepository.credentialsfeeder.config;

import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.credentialsfeeder.repository.UinRepo;

import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_ALIAS;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_DRIVERCLASSNAME;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_PASSWORD;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_URL;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_USERNAME;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DB_SCHEMA_NAME;

import org.springframework.core.env.Environment;
/**
 * The Class CredentialsFeederConfig - Provides configuration for credential feeder application.
 *
 * @author Manoj SP
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = { UinHashSaltRepo.class, UinEncryptSaltRepo.class,
		CredentialRequestStatusRepo.class, UinRepo.class })
public class CredentialsFeederConfig extends IdRepoDataSourceConfig {

	@Autowired
	private Environment env;
	/**
	 * Batch config
	 *
	 * @return the batch configurer
	 */
	/*@Bean
	public BatchConfigurer batchConfig() {
		return new DefaultBatchConfigurer(null) {
			
			*//**
			 * By default, Spring batch will try to create/update records 
			 * in the provided datasource related to Job completion, schedule etc.
			 * This override will stop spring batch to create/update any tables in provided
			 * Datasource and instead use Map based implementation internally.
			 *
			 *//*
			@Override
			public void setDataSource(DataSource dataSource) {
				// By default, Spring batch will try to create/update records in the provided
				// datasource related to Job completion, schedule etc.
				// This override will stop spring batch to create/update any tables in provided
				// Datasource and instead use Map based implementation internally.
			}
		};
	}*/

	@Bean
	@Primary
	public DataSource dataSource() {
		String alias = env.getProperty(DATASOURCE_ALIAS.getValue());
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl(env.getProperty(String.format(DATASOURCE_URL.getValue(), alias)));
		dataSource.setUsername(env.getProperty(String.format(DATASOURCE_USERNAME.getValue(), alias)));
		dataSource.setPassword(env.getProperty(String.format(DATASOURCE_PASSWORD.getValue(), alias)));
		dataSource.setSchema(env.getProperty(DB_SCHEMA_NAME.getValue()));
		dataSource.setDriverClassName(env.getProperty(String.format(DATASOURCE_DRIVERCLASSNAME.getValue(), alias)));
		return dataSource;
	}
	
	@Bean
	public RestHelper restHelper(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new RestHelper(webClient);
	}
	
	@Bean
	public IdRepoSecurityManager securityManager(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new IdRepoSecurityManager(restHelper(webClient));
	}
	
	@Bean
	@Qualifier("webSubHelperExecutor")
	public Executor webSubHelperExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 3));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idauth-websub-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    executor.initialize();
	    return executor;
	}
}