package io.mosip.idrepository.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * The Class IdRepoApplication.
 *
 * @author Manoj SP
 */
@SpringBootApplication(exclude = MailSenderAutoConfiguration.class)
@EnableCaching
@ComponentScan(basePackages={ "io.mosip.*" ,"${mosip.auth.adapter.impl.basepackage}"}, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASPECTJ, 
				pattern = {"io.mosip.kernel.zkcryptoservice.*",
						"io.mosip.kernel.tokenidgenerator.*",
						"io.mosip.kernel.signature.*",
						"io.mosip.kernel.partnercertservice.*",
						"io.mosip.kernel.lkeymanager.*",
						"io.mosip.kernel.keymanagerservice.*",
						"io.mosip.kernel.keymanager.*",
						"io.mosip.kernel.keygenerator.*",
						"io.mosip.kernel.cryptomanager.*",
						"io.mosip.kernel.crypto.*",
						"io.mosip.kernel.clientcrypto.*",
						"io.mosip.commons.khazana.impl.PosixAdapter",
						"io.mosip.commons.khazana.util.*"
}) })
public class IdRepoBootApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(IdRepoBootApplication.class, args);
	}
}
