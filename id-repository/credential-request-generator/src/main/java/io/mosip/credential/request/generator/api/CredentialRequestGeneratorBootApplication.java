package io.mosip.credential.request.generator.api;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;

/**
 * The Class CredentialRequestGeneratorApp.
 *
 * @author Sowmya
 */
@EnableCaching
@SpringBootApplication(exclude = HibernateDaoConfig.class)
@Import(value = { java.lang.String.class, DummyPartnerCheckUtil.class, RestHelper.class, IdRepoSecurityManager.class })
@ComponentScan(basePackages = { "io.mosip.*",
"${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
				"io.mosip.idrepository.core.config.IdRepoDataSourceConfig.*", "io.mosip.kernel.dataaccess.hibernate.config.*" }))
@EnableBatchProcessing
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
