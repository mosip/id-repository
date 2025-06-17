package io.mosip.credential.request.generator;

import io.mosip.kernel.websub.api.client.SubscriberClientImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.credential.request.generator.api.config.CredentialRequestGeneratorConfig;
import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;

/**
 * The Class CredentialRequestGeneratorApp.
 *
 */
@SpringBootApplication
@Import(value = { java.lang.String.class, DummyPartnerCheckUtil.class, RestHelper.class, IdRepoSecurityManager.class,
		CredentialRequestGeneratorConfig.class})
@ComponentScan(basePackages = { "io.mosip.credential.*","io.mosip.idrepository.*", "io.mosip.kernel.*", "${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = { "io.mosip.kernel.dataaccess.hibernate.config.*" }),
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { HibernateDaoConfig.class,
				IdRepoDataSourceConfig.class }) })

@EnableScheduling
@EnableAspectJAutoProxy
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