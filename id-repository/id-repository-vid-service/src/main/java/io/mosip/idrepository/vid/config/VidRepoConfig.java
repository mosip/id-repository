package io.mosip.idrepository.vid.config;

import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_DRIVER_CLASS_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_PASSWORD;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_USERNAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.entity.UinEncryptSalt;
import io.mosip.idrepository.core.entity.UinHashSalt;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.vid.repository.VidRepo;

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
@EnableJpaRepositories(basePackageClasses = { VidRepo.class, UinHashSaltRepo.class, UinEncryptSaltRepo.class })
public class VidRepoConfig {

	/** The Interceptor. */
	@Autowired
	private Interceptor interceptor;
	
	@Autowired
	private EnvUtil env;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.core.dao.config.BaseDaoConfig#jpaProperties()
	 */
	public Map<String, Object> jpaProperties() {
		Map<String, Object> jpaProperties = new HashMap<>();
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
	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(env.getProperty(VID_DB_URL));
		dataSource.setUsername(env.getProperty(VID_DB_USERNAME));
		dataSource.setPassword(env.getProperty(VID_DB_PASSWORD));
		dataSource.setDriverClassName(env.getProperty(VID_DB_DRIVER_CLASS_NAME));
		return dataSource;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("io.mosip.idrepository.vid.*");

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaPropertyMap(jpaProperties());
		em.setPersistenceUnitPostProcessors(new PersistenceUnitPostProcessor() {

			@Override
			public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
				pui.addManagedClassName(UinEncryptSalt.class.getName());
				pui.addManagedClassName(UinHashSalt.class.getName());
			}
		});
		return em;
	}

	@Bean
	public CredentialServiceManager credentialServiceManager(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new CredentialServiceManager(restHelperWithAuth(webClient));
	}
	
	@Bean
	public RestHelper restHelperWithAuth(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new RestHelper(webClient);
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