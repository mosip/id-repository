package io.mosip.idrepository.saltgenerator.config;

import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_DRIVERCLASSNAME;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_PASSWORD;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_SCHEMA;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_URL;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_USERNAME;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;

/**
 * The Class SaltGeneratorIdMapDataSourceConfig - Provides configuration for Salt
 * generator application.
 *
 * @author Manoj SP
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.saltgenerator.repository.idrepo",
entityManagerFactoryRef = "identityEntityManagerFactory",
transactionManagerRef= "identityTransactionManager"
)
public class SaltGeneratorIdRepoDataSourceConfig {
	
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
		dataSource.setSchema(env.getProperty(String.format(DATASOURCE_SCHEMA.getValue(), alias)));
		return dataSource;
	}
	
	 /*Primary Entity manager*/
	   @Primary
	   @Bean(name = "identityEntityManagerFactory")
	   public LocalContainerEntityManagerFactoryBean identityEntityManagerFactory(EntityManagerFactoryBuilder builder) {
	       return builder
	               .dataSource(identityDataSource())
	               .packages(IdentityHashSaltEntity.class)
	               .properties(additionalProperties())
	               .build();
	   }
	   
	   /**
		 * Additional properties.
		 *
		 * @return the map
		 */
		private Map<String, Object> additionalProperties() {
			Map<String, Object> jpaProperties = new HashMap<>();
			jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect");
			return jpaProperties;
		}
	   
	   @Primary
	   @Bean(name = "identityTransactionManager")
	   public PlatformTransactionManager identityTransactionManager(
	           final @Qualifier("identityEntityManagerFactory") LocalContainerEntityManagerFactoryBean memberEntityManagerFactory) {
	       return new JpaTransactionManager(memberEntityManagerFactory.getObject());

	   }
	
}
