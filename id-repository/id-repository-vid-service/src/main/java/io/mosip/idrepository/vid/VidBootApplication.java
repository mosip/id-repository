package io.mosip.idrepository.vid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;

/**
 * The Class IdRepoVidApplication.
 *
 * @author Prem Kumar
 */
@SpringBootApplication
@Import({ IdRepoSecurityManager.class, DummyPartnerCheckUtil.class })
@ComponentScan(basePackages = { "io.mosip.idrepository.vid.*", "io.mosip.idrepository.core.*",
		"io.mosip.kernel.websub.api.config", "io.mosip.kernel.idvalidator.vid.impl", "io.mosip.kernel.idvalidator.uin.impl",
		"${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
				"io.mosip.idrepository.core.entity",
				"io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig" }))
public class VidBootApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(VidBootApplication.class, args);
	}
}
