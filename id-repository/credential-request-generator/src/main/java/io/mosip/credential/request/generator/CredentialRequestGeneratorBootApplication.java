package io.mosip.credential.request.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.credential.request.generator.api.config.CredentialRequestGeneratorConfig;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;

/**
 * The Class CredentialRequestGeneratorApp.
 *
 */
@SpringBootApplication
@Import(value = { java.lang.String.class, DummyPartnerCheckUtil.class, RestHelper.class, IdRepoSecurityManager.class,
		CredentialRequestGeneratorConfig.class })
@ComponentScan(basePackages = { "io.mosip.credential.*", "${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = { "io.mosip.kernel.dataaccess.hibernate.config.*" }),
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { HibernateDaoConfig.class,
				IdRepoDataSourceConfig.class }) })

@EnableScheduling
public class CredentialRequestGeneratorBootApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(CredentialRequestGeneratorBootApplication.class, args);
	}
}
