package io.mosip.idrepository.config;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.mosip.kernel.auth.defaultadapter.helper.TokenHelper;
import io.mosip.kernel.auth.defaultadapter.helper.TokenValidationHelper;
import io.mosip.kernel.auth.defaultadapter.helper.ValidateTokenHelper;

/**
 * Registers kernel-auth helper beans used by self-token interceptors and {@code AuthFilter}.
 * <p>
 * Helpers come from {@code kernel-auth-adapter} 1.4.1-SNAPSHOT. Explicit {@code @Bean}
 * registration with {@code @Primary} avoids duplicate-type ambiguity when
 * {@code KernelAuthSpringFactoriesFilteringClassLoader} and component scan would otherwise
 * resolve different {@code Class} objects for the same FQCN.
 * </p>
 *
 * <h2>Beans registered</h2>
 * <ul>
 *   <li>{@link ValidateTokenHelper} — online/offline JWT validation against IAM</li>
 *   <li>{@link TokenHelper} — client-credentials token fetch for {@code selfTokenRestTemplate}</li>
 *   <li>{@link TokenValidationHelper} — facade used by auth filter and self-token interceptors</li>
 * </ul>
 *
 * @see io.mosip.idrepository.config.KernelAuthSecurityConfig
 * @see io.mosip.idrepository.config.IdRepoSelfTokenStartupConfig
 * @see io.mosip.idrepository.bootstrap.KernelAuthSpringFactoriesFilteringClassLoader
 */
@Configuration
public class IdRepoKernelAuthHelperConfig {

	/**
	 * Primary {@link ValidateTokenHelper} bean created through the Spring factory so
	 * {@code @Autowired} fields inside the helper are populated.
	 *
	 * @param beanFactory Spring bean factory used to instantiate the helper
	 * @return configured validate-token helper
	 */
	@Bean
	@Primary
	public ValidateTokenHelper validateTokenHelper(AutowireCapableBeanFactory beanFactory) {
		return beanFactory.createBean(ValidateTokenHelper.class);
	}

	/**
	 * Primary {@link TokenHelper} for IAM client-credentials flows.
	 *
	 * @param beanFactory Spring bean factory
	 * @return configured token helper
	 */
	@Bean
	@Primary
	public TokenHelper tokenHelper(AutowireCapableBeanFactory beanFactory) {
		return beanFactory.createBean(TokenHelper.class);
	}

	/**
	 * Primary {@link TokenValidationHelper} delegating to {@link ValidateTokenHelper}.
	 *
	 * @param beanFactory Spring bean factory
	 * @return configured token validation helper
	 */
	@Bean
	@Primary
	public TokenValidationHelper tokenValidationHelper(AutowireCapableBeanFactory beanFactory) {
		return beanFactory.createBean(TokenValidationHelper.class);
	}
}