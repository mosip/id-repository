package io.mosip.idrepository.saltgenerator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.mosip.idrepository.saltgenerator.service.DatabaseRouter;

/**
 * Root Spring configuration for the salt-generator Job.
 *
 * <p>
 * Imports {@link DatabaseRouter}, which registers the {@code primaryDataSource}
 * ({@code mosip_idrepo}) and {@code secondaryDataSource} ({@code mosip_idmap}) beans used by
 * {@link io.mosip.idrepository.saltgenerator.service.SaltJdbcWriter}.
 * </p>
 *
 * <h2>Why a separate config class</h2>
 * Keeps datasource wiring explicit and importable from
 * {@link io.mosip.idrepository.saltgenerator.SaltGeneratorBootApplication} without scanning
 * HTTP-service packages.
 *
 * @author MOSIP
 * @see DatabaseRouter
 * @see io.mosip.idrepository.saltgenerator.SaltGeneratorBootApplication
 */
@Configuration
@Import(DatabaseRouter.class)
public class SaltGeneratorConfiguration {

}
