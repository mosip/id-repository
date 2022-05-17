package io.mosip.idrepository.credentialsfeeder;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;

/**
 * The Class CredentialsFeederApplication - Salt generator Job is a one-time job
 * which populates salts for hashing and encrypting data.
 *
 * @author Manoj SP
 */
@SpringBootApplication
@EnableBatchProcessing
@EnableAutoConfiguration(exclude = { ScheduledTasksEndpointAutoConfiguration.class })
@Import({ java.lang.String.class, IdRepoDataSourceConfig.class, CredentialServiceManager.class, TokenIDGenerator.class,
		IdRepoWebSubHelper.class, CredentialStatusManager.class, DummyPartnerCheckUtil.class, EnvUtil.class,
		PartnerServiceManager.class })
@ComponentScan(basePackages = {
		"io.mosip.idrepository.credentialsfeeder.*", "${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
				"io.mosip.idrepository.core.entity", "io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig" }))
public class CredentialsFeederBootApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		ApplicationContext applicationContext = SpringApplication.run(CredentialsFeederBootApplication.class, args);
		SpringApplication.exit(applicationContext);
	}

}
