package io.mosip.idrepository.credentialsfeeder.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

/**
 * The Class CredentialsFeederConfig - Provides configuration for credential feeder application.
 *
 * @author Manoj SP
 */
@Configuration
public class CredentialsFeederConfig extends IdRepoDataSourceConfig {
	
	/**
	 * Batch config
	 *
	 * @return the batch configurer
	 */
	@Bean
	public BatchConfigurer batchConfig() {
		return new DefaultBatchConfigurer(null) {
			
			/**
			 * By default, Spring batch will try to create/update records 
			 * in the provided datasource related to Job completion, schedule etc.
			 * This override will stop spring batch to create/update any tables in provided
			 * Datasource and instead use Map based implementation internally.
			 *
			 */
			@Override
			public void setDataSource(DataSource dataSource) {
				// By default, Spring batch will try to create/update records in the provided
				// datasource related to Job completion, schedule etc.
				// This override will stop spring batch to create/update any tables in provided
				// Datasource and instead use Map based implementation internally.
			}
		};
	}
	
	@Bean("restHelperWithAuth")
	public RestHelper restHelper(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new RestHelper(webClient);
	}
	
	@Bean("securityManagerWithAuth")
	public IdRepoSecurityManager securityManager(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new IdRepoSecurityManager(restHelper(webClient));
	}
}
