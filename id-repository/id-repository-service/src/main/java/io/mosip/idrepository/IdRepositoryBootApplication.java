package io.mosip.idrepository;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;

import io.mosip.idrepository.bootstrap.KernelAuthSpringFactoriesFilteringClassLoader;
import io.mosip.idrepository.config.HttpModeScanConfiguration;
import io.mosip.idrepository.config.IdRepoApiPathConfig;
import io.mosip.idrepository.config.IdRepoKernelAuthHelperConfig;
import io.mosip.idrepository.config.IdRepoOpenApiConfig;
import io.mosip.idrepository.core.config.CacheConfig;
import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.config.IdRepoLibraryConfig;
import io.mosip.idrepository.credential.request.api.config.CredentialRequestGeneratorConfig;
import io.mosip.idrepository.credential.store.api.config.CredentialStoreConfig;
import io.mosip.idrepository.credential.store.config.CredentialStoreBeanConfig;
import io.mosip.idrepository.identity.config.SwaggerConfig;
import io.mosip.idrepository.vid.config.VidMvelConfig;
import io.mosip.idrepository.vid.config.VidRepoConfig;

/**
 * Spring Boot entry point for the MOSIP ID-Repository HTTP service.
 * <p>
 * Hosts identity, credential, credential-request, and VID REST APIs. Business logic lives in
 * {@code id-repository-service}; {@code id-repository-core} is the shared library (IDA API).
 * </p>
 * <p>
 * Salt population is a separate deployable: {@code id-repository-salt-generator} (K8s Job via
 * {@code helm/idrepo-saltgen}). {@code kernel-core} auto-configures unused ID generators and
 * validators; those class names are listed in {@code excludeName} so Boot 4 does not instantiate them.
 * </p>
 *
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 */
@SpringBootApplication(excludeName = {
		// kernel-core AutoConfiguration.imports / spring.factories — unused in this JVM
		"io.mosip.kernel.idgenerator.vid.impl.VidGeneratorImpl",
		"io.mosip.kernel.idgenerator.vid.util.VidFilterUtils",
		"io.mosip.kernel.idgenerator.tokenid.impl.TokenIdGeneratorImpl",
		"io.mosip.kernel.idgenerator.machineid.impl.MachineIdGeneratorImpl",
		"io.mosip.kernel.idgenerator.regcenterid.impl.RegistrationCenterIdGeneratorImpl",
		"io.mosip.kernel.idgenerator.mispid.impl.MispIdGeneratorImpl",
		"io.mosip.kernel.licensekeygenerator.misp.impl.MISPLicenseKeyGeneratorImpl",
		"io.mosip.kernel.licensekeygenerator.misp.util.MISPLicenseKeyGeneratorUtil",
		"io.mosip.kernel.idgenerator.rid.impl.RidGeneratorImpl",
		"io.mosip.kernel.idvalidator.prid.impl.PridValidatorImpl",
		"io.mosip.kernel.idvalidator.mispid.impl.MispIdValidatorImpl",
		"io.mosip.kernel.templatemanager.velocity.builder.TemplateManagerBuilderImpl",
		"io.mosip.kernel.pdfgenerator.impl.PDFGeneratorImpl",
		"io.mosip.kernel.qrcode.generator.zxing.QrcodeGeneratorImpl",
		"io.mosip.kernel.transliteration.icu4j.impl.TransliterationImpl",
		"io.mosip.kernel.applicanttype.api.impl.ApplicantTypeImpl"
})
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
		IdRepoDataSourceConfig.class,
		CacheConfig.class,
		CredentialStoreBeanConfig.class,
		CredentialStoreConfig.class,
		CredentialRequestGeneratorConfig.class,
		SwaggerConfig.class,
		VidRepoConfig.class,
		VidMvelConfig.class
}))
@Import({ IdRepoLibraryConfig.class, IdRepoOpenApiConfig.class, IdRepoApiPathConfig.class,
		IdRepoKernelAuthHelperConfig.class, HttpModeScanConfiguration.class })
public class IdRepositoryBootApplication {

	public static void main(String[] args) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		SpringApplication application = new SpringApplication(new DefaultResourceLoader(classLoader),
				IdRepositoryBootApplication.class);
		application.setDefaultProperties(Map.of("spring.cloud.bootstrap.enabled", "false"));
		application.run(args);
	}
}
