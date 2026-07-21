package io.mosip.idrepository.vid.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import io.mosip.idrepository.core.entity.UinEncryptSalt;
import io.mosip.idrepository.core.entity.UinHashSalt;
import io.mosip.idrepository.core.config.IdRepoHikariDataSourceFactory;
import io.mosip.idrepository.core.config.IdRepoHibernateJpaProperties;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.vid.interceptor.IdRepoVidEntityInterceptor;
import io.mosip.idrepository.vid.repository.VidRepo;
import io.mosip.idrepository.vid.repository.VidUinEncryptSaltRepo;
import io.mosip.idrepository.vid.repository.VidUinHashSaltRepo;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;

/**
 * Second persistence unit for {@code mosip_idmap} (VID domain).
 * <p>
 * Uses separate encrypt/hash salt repositories ({@link VidUinEncryptSaltRepo},
 * {@link VidUinHashSaltRepo}) on {@code mosip_idmap} to avoid cross-database salt mis-routing.
 * Imported via {@link io.mosip.idrepository.config.IdRepoLibraryConfig}.
 * </p>
 *
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 * @see VidUinEncryptSaltRepo
 * @see VidUinHashSaltRepo
 * @see io.mosip.idrepository.vid.config.VidMvelConfig
 */
@Configuration
@ConfigurationProperties("mosip.idrepo.vid")
public class VidRepoConfig {

	/** Hibernate interceptor for VID entity crypto operations. */
	@Autowired
	private IdRepoVidEntityInterceptor idRepoVidEntityInterceptor;

	/** VID type to ID-schema field mapping from {@code mosip.idrepo.vid.id}.
     * -- SETTER --
     *
     * @param id VID ID mapping properties
     */
	@Setter
    private Map<String, String> id;

	/** Allowed VID status values from {@code mosip.idrepo.vid.allowedstatus} (config server).
     * -- SETTER --
     *
     * @param status allowed VID status list
     */
	@Setter
    private List<String> allowedStatus;

    /**
	 * @return unmodifiable VID ID field map bean
	 */
	@Bean("vidIdMap")
	public Map<String, String> vidIdMap() {
		return Collections.unmodifiableMap(id);
	}

	/**
	 * @return unmodifiable allowed VID status list bean
	 */
	@Bean("vidAllowedStatusList")
	/**
	 * Vid allowed status list.
	 * @return list<string>
	 */
	public List<String> vidAllowedStatusList() {
		return Collections.unmodifiableList(allowedStatus);
	}

	/**
	 * @param env environment utility for VID DB connection properties
	 * @return {@code mosip_idmap} datasource
	 */
	@Bean("vidDataSource")
	/**
	 * Vid data source.
	 * @param env env
	 * @return data source
	 */
	public DataSource vidDataSource(EnvUtil env) {
		return IdRepoHikariDataSourceFactory.vidPool(env);
	}

	/**
	 * @param env environment utility
	 * @return entity manager factory for VID entities
	 */
	@Bean("vidEntityManagerFactory")
	/**
	 * Vid entity manager factory.
	 * @param env env
	 * @return local container entity manager factory bean
	 */
	public LocalContainerEntityManagerFactoryBean vidEntityManagerFactory(EnvUtil env) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(vidDataSource(env));
		em.setPackagesToScan("io.mosip.idrepository.vid.entity");
		em.setPersistenceUnitName("idmap");

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaPropertyMap(vidJpaProperties());
		em.setPersistenceUnitPostProcessors(new PersistenceUnitPostProcessor() {
			@Override
			public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
				pui.addManagedClassName(UinEncryptSalt.class.getName());
				pui.addManagedClassName(UinHashSalt.class.getName());
			}
		});
		return em;
	}

	/**
	 * @param vidEntityManagerFactory VID persistence unit factory
	 * @return JPA transaction manager for idmap
	 */
	@Bean("vidTransactionManager")
	/**
	 * Vid transaction manager.
	 * @param vidEntityManagerFactory vid entity manager factory
	 * @return jpa transaction manager
	 */
	public JpaTransactionManager vidTransactionManager(
			@org.springframework.beans.factory.annotation.Qualifier("vidEntityManagerFactory")
			LocalContainerEntityManagerFactoryBean vidEntityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(vidEntityManagerFactory.getObject());
		return transactionManager;
	}

	/** @return Hibernate properties for the VID persistence unit */
	private Map<String, Object> vidJpaProperties() {
		Map<String, Object> jpaProperties = new HashMap<>();
		IdRepoHibernateJpaProperties.applyKernelAuthClassLoaderSettings(jpaProperties);
		jpaProperties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
		jpaProperties.put("hibernate.physical_naming_strategy",
				org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl.class.getName());
		jpaProperties.put("hibernate.session_factory.interceptor", idRepoVidEntityInterceptor);
		jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE);
		return jpaProperties;
	}

	/**
	 * Enables Spring Data JPA repositories on the VID persistence unit.
	 */
	@Configuration
	@EnableJpaRepositories(
			entityManagerFactoryRef = "vidEntityManagerFactory",
			transactionManagerRef = "vidTransactionManager",
			basePackageClasses = { VidRepo.class, VidUinHashSaltRepo.class, VidUinEncryptSaltRepo.class },
			repositoryBaseClass = HibernateRepositoryImpl.class)
	static class VidJpaConfig {
	}
}
