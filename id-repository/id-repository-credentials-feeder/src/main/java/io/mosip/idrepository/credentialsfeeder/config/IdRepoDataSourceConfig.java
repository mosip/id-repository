package io.mosip.idrepository.credentialsfeeder.config;

import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_DRIVERCLASSNAME;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_PASSWORD;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_URL;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DATASOURCE_USERNAME;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The Class IdRepoDataSourceConfig
 *
 * @author Manoj SP
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.credentialsfeeder.repository")
public class IdRepoDataSourceConfig {
	
	private static final String MOSIP_IDREPO_IDENTITY_DB = "mosip.idrepo.identity.db";
	
	@Autowired
	private Environment env;

	@Bean
	@Primary
	public DataSource identityDataSource() {
		String alias = MOSIP_IDREPO_IDENTITY_DB;
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl(env.getProperty(String.format(DATASOURCE_URL.getValue(), alias)));
		dataSource.setUsername(env.getProperty(String.format(DATASOURCE_USERNAME.getValue(), alias)));
		dataSource.setPassword(env.getProperty(String.format(DATASOURCE_PASSWORD.getValue(), alias)));
		dataSource.setDriverClassName(env.getProperty(String.format(DATASOURCE_DRIVERCLASSNAME.getValue(), alias)));
		return dataSource;
	}
	
}
