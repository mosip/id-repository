package io.mosip.idrepository.saltgenerator.config;

import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_DRIVERCLASSNAME;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_PASSWORD;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_SCHEMA;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_URL;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.DATASOURCE_USERNAME;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity;

/**
 * The Class SaltGeneratorIdMapDataSourceConfig - Provides configuration for Salt
 * generator application.
 *
 * @author Manoj SP
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.saltgenerator.repository.idmap",
entityManagerFactoryRef = "vidEntityManagerFactory",
transactionManagerRef= "vidTransactionManager"
)
public class SaltGeneratorIdMapDataSourceConfig {
	
	private static final String MOSIP_IDREPO_VID_DB = "mosip.idrepo.vid.db";
	@Autowired
	private Environment env;

	@Bean
	public DataSource vidDataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase embeddedDatabase = builder
				.setType(EmbeddedDatabaseType.H2)
				.generateUniqueName(true)
				.build();
		return embeddedDatabase;
	}
	
	 /*Primary Entity manager*/
	 @Bean(name = "vidEntityManagerFactory")
	 public LocalContainerEntityManagerFactoryBean vidEntityManagerFactory(
			 EntityManagerFactoryBuilder builder) {  // Injecting Builder explicitly
		 return builder
				 .dataSource(vidDataSource())
				 .packages(VidHashSaltEntity.class)
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
			jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
			return jpaProperties;
		}
		
	   
	   @Bean(name = "vidTransactionManager")
	   public PlatformTransactionManager vidTransactionManager(
	           final @Qualifier("vidEntityManagerFactory") LocalContainerEntityManagerFactoryBean memberEntityManagerFactory) {
	       return new JpaTransactionManager(memberEntityManagerFactory.getObject());

	   }

	@Bean
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
			JpaVendorAdapter jpaVendorAdapter,
			ObjectProvider<PersistenceUnitManager> persistenceUnitManager) {
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, new HashMap<>(), persistenceUnitManager.getIfAvailable());
	}

	@Bean
	public JpaVendorAdapter jpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

}
