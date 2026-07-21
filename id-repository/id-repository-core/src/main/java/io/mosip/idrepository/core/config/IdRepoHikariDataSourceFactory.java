package io.mosip.idrepository.core.config;

import com.zaxxer.hikari.HikariDataSource;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.util.EnvUtil;

/**
 * Factory for HikariCP {@link HikariDataSource} instances used by the three
 * ID-Repository persistence units.
 *
 * <p>
 * Centralizes JDBC URL, credentials, schema, and pool sizing so PU1 (identity), PU2
 * (VID / idmap), and PU3 (credential) share the same connection-pool defaults and
 * property-key conventions from {@link IdRepoConstants}. Callers pass an
 * {@link EnvUtil} (or equivalent property source) already loaded from Spring Cloud
 * Config.
 * </p>
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Replace ad-hoc {@code DriverManagerDataSource} / duplicated Hikari setup</li>
 *   <li>Apply consistent max/min pool size, connection timeout, idle timeout, and max
 *       lifetime across PUs</li>
 *   <li>Register JMX MBeans ({@code registerMbeans=true}) for pool metrics</li>
 * </ul>
 *
 * <h2>Beans / wiring</h2>
 * <p>
 * This class is <strong>not</strong> a Spring {@code @Configuration}. Static factory
 * methods are invoked from:
 * </p>
 * <ul>
 *   <li>{@link IdRepoDataSourceConfig#dataSource()} → {@link #identityPool(EnvUtil)}</li>
 *   <li>VID / idmap configuration → {@link #vidPool(EnvUtil)}</li>
 *   <li>Credential / library config → {@link #credentialPool(EnvUtil)}</li>
 *   <li>Salt-generator Job (reuses identity/idmap helpers without HTTP service code)</li>
 * </ul>
 *
 * <h2>Multi-datasource / salt notes</h2>
 * <table border="1" summary="Pools produced by this factory">
 *   <tr><th>Method</th><th>Pool name</th><th>Database</th><th>Schema</th></tr>
 *   <tr>
 *     <td>{@link #identityPool}</td>
 *     <td>{@code idrepo-pool}</td>
 *     <td>{@code mosip_idrepo}</td>
 *     <td>{@code idrepo} (explicit)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #vidPool}</td>
 *     <td>{@code idmap-pool}</td>
 *     <td>{@code mosip_idmap}</td>
 *     <td>omitted — resolved by JPA entities</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #credentialPool}</td>
 *     <td>{@code credential-pool}</td>
 *     <td>{@code mosip_credential}</td>
 *     <td>omitted</td>
 *   </tr>
 * </table>
 * <p>
 * Identity and idmap each have their own {@code uin_hash_salt} /
 * {@code uin_encrypt_salt} tables. Use the matching pool/PU; never share one datasource
 * across both salt schemas. Budget PostgreSQL {@code max_connections} as approximately
 * {@code replicas × (identity.max + vid.max + credential.max)} per database host —
 * pool sizes are <strong>per JVM (per pod)</strong>.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * HikariDataSource idrepo = IdRepoHikariDataSourceFactory.identityPool(env);
 * HikariDataSource idmap  = IdRepoHikariDataSourceFactory.vidPool(env);
 * HikariDataSource cred   = IdRepoHikariDataSourceFactory.credentialPool(env);
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link IdRepoDataSourceConfig} (PU1 primary datasource)</li>
 *   <li>Service-module VID and credential datasource beans</li>
 *   <li>{@code id-repository-salt-generator} Job (identity / idmap pools only)</li>
 * </ul>
 *
 * @see IdRepoConstants
 * @see EnvUtil
 * @see IdRepoDataSourceConfig
 * @see HikariDataSource
 */
public final class IdRepoHikariDataSourceFactory {

	/**
	 * Prevents instantiation; use static factory methods only.
	 */
	private IdRepoHikariDataSourceFactory() {
	}

	/**
	 * Creates a fully configured {@link HikariDataSource} from explicit JDBC and pool
	 * parameters.
	 * <p>
	 * Resolves max pool size, minimum idle, and connection timeout from {@code env}
	 * using the given property keys (with numeric defaults). Idle timeout and max
	 * lifetime use shared keys {@link IdRepoConstants#POOL_IDLE_TIMEOUT_MS} and
	 * {@link IdRepoConstants#POOL_MAX_LIFETIME_MS}. Driver defaults to
	 * {@code org.postgresql.Driver} when {@code driver} is {@code null}. Schema is set
	 * only when non-blank.
	 * </p>
	 *
	 * @param env         property accessor ({@link EnvUtil}) for pool sizing keys
	 * @param poolName    Hikari pool name (metrics / thread names)
	 * @param jdbcUrl     JDBC URL
	 * @param username    DB user
	 * @param password    DB password
	 * @param driver      driver class name, or {@code null} for PostgreSQL default
	 * @param schema      optional PostgreSQL schema; {@code null} or blank to omit
	 * @param maxProp     config key for maximum pool size
	 * @param maxDefault  default max when property unset
	 * @param minProp     config key for minimum idle connections
	 * @param minDefault  default min when property unset
	 * @param timeoutProp config key for connection acquire timeout (ms)
	 * @return configured pooled datasource with JMX MBeans enabled
	 * @throws NumberFormatException if a pool size or timeout property is not numeric
	 * @see #identityPool(EnvUtil)
	 * @see #vidPool(EnvUtil)
	 * @see #credentialPool(EnvUtil)
	 */
	public static HikariDataSource create(EnvUtil env, String poolName, String jdbcUrl, String username,
			String password, String driver, String schema, String maxProp, int maxDefault, String minProp,
			int minDefault, String timeoutProp) {
		HikariDataSource ds = new HikariDataSource();
		ds.setPoolName(poolName);
		ds.setJdbcUrl(jdbcUrl);
		ds.setUsername(username);
		ds.setPassword(password);
		ds.setDriverClassName(driver != null ? driver : "org.postgresql.Driver");
		if (schema != null && !schema.isBlank()) {
			ds.setSchema(schema);
		}
		ds.setMaximumPoolSize(Integer.parseInt(env.getProperty(maxProp, String.valueOf(maxDefault))));
		ds.setMinimumIdle(Integer.parseInt(env.getProperty(minProp, String.valueOf(minDefault))));
		ds.setConnectionTimeout(Long.parseLong(
				env.getProperty(timeoutProp, String.valueOf(IdRepoConstants.POOL_CONNECTION_TIMEOUT_DEFAULT_MS))));
		ds.setIdleTimeout(Long.parseLong(
				env.getProperty(IdRepoConstants.POOL_IDLE_TIMEOUT_MS,
						String.valueOf(IdRepoConstants.POOL_IDLE_TIMEOUT_DEFAULT_MS))));
		ds.setMaxLifetime(Long.parseLong(
				env.getProperty(IdRepoConstants.POOL_MAX_LIFETIME_MS,
						String.valueOf(IdRepoConstants.POOL_MAX_LIFETIME_DEFAULT_MS))));
		ds.setRegisterMbeans(true);
		return ds;
	}

	/**
	 * Builds the identity ({@code mosip_idrepo}) pool with PostgreSQL schema
	 * {@code idrepo}.
	 * <p>
	 * JDBC settings from {@link IdRepoConstants#IDENTITY_DB_URL} (and username /
	 * password / driver). Pool sizing from {@link IdRepoConstants#IDENTITY_POOL_MAX},
	 * {@link IdRepoConstants#IDENTITY_POOL_MIN}, and
	 * {@link IdRepoConstants#IDENTITY_POOL_TIMEOUT_MS}.
	 * </p>
	 *
	 * @param env property accessor with identity DB and pool keys loaded
	 * @return Hikari pool named {@code idrepo-pool}
	 * @see IdRepoDataSourceConfig#dataSource()
	 */
	public static HikariDataSource identityPool(EnvUtil env) {
		return create(env, "idrepo-pool",
				env.getProperty(IdRepoConstants.IDENTITY_DB_URL),
				env.getProperty(IdRepoConstants.IDENTITY_DB_USERNAME),
				env.getProperty(IdRepoConstants.IDENTITY_DB_PASSWORD),
				env.getProperty(IdRepoConstants.IDENTITY_DB_DRIVER_CLASS_NAME),
				"idrepo",
				IdRepoConstants.IDENTITY_POOL_MAX, IdRepoConstants.IDENTITY_POOL_MAX_DEFAULT,
				IdRepoConstants.IDENTITY_POOL_MIN, IdRepoConstants.IDENTITY_POOL_MIN_DEFAULT,
				IdRepoConstants.IDENTITY_POOL_TIMEOUT_MS);
	}

	/**
	 * Builds the VID ({@code mosip_idmap}) pool; schema is left unset so JPA entities
	 * resolve it.
	 * <p>
	 * JDBC settings from {@link IdRepoConstants#VID_DB_URL} (and username / password /
	 * driver). Pool sizing from {@link IdRepoConstants#VID_POOL_MAX},
	 * {@link IdRepoConstants#VID_POOL_MIN}, and
	 * {@link IdRepoConstants#VID_POOL_TIMEOUT_MS}. Holds idmap salt tables separately
	 * from identity salts.
	 * </p>
	 *
	 * @param env property accessor with VID DB and pool keys loaded
	 * @return Hikari pool named {@code idmap-pool}
	 */
	public static HikariDataSource vidPool(EnvUtil env) {
		return create(env, "idmap-pool",
				env.getProperty(IdRepoConstants.VID_DB_URL),
				env.getProperty(IdRepoConstants.VID_DB_USERNAME),
				env.getProperty(IdRepoConstants.VID_DB_PASSWORD),
				env.getProperty(IdRepoConstants.VID_DB_DRIVER_CLASS_NAME),
				null,
				IdRepoConstants.VID_POOL_MAX, IdRepoConstants.VID_POOL_MAX_DEFAULT,
				IdRepoConstants.VID_POOL_MIN, IdRepoConstants.VID_POOL_MIN_DEFAULT,
				IdRepoConstants.VID_POOL_TIMEOUT_MS);
	}

	/**
	 * Builds the credential ({@code mosip_credential}) pool for PU3 and Spring Batch.
	 * <p>
	 * JDBC settings from {@code mosip.credential.service.jdbc.*} properties. Pool sizing
	 * from {@link IdRepoConstants#CREDENTIAL_POOL_MAX},
	 * {@link IdRepoConstants#CREDENTIAL_POOL_MIN}, and
	 * {@link IdRepoConstants#CREDENTIAL_POOL_TIMEOUT_MS}. Used for credential entities
	 * and {@code BATCH_*} metadata — not for identity/VID salt access.
	 * </p>
	 *
	 * @param env property accessor with credential JDBC and pool keys loaded
	 * @return Hikari pool named {@code credential-pool}
	 */
	public static HikariDataSource credentialPool(EnvUtil env) {
		return create(env, "credential-pool",
				env.getProperty("mosip.credential.service.jdbc.url"),
				env.getProperty("mosip.credential.service.jdbc.user"),
				env.getProperty("mosip.credential.service.jdbc.password"),
				env.getProperty("mosip.credential.service.jdbc.driver", "org.postgresql.Driver"),
				null,
				IdRepoConstants.CREDENTIAL_POOL_MAX, IdRepoConstants.CREDENTIAL_POOL_MAX_DEFAULT,
				IdRepoConstants.CREDENTIAL_POOL_MIN, IdRepoConstants.CREDENTIAL_POOL_MIN_DEFAULT,
				IdRepoConstants.CREDENTIAL_POOL_TIMEOUT_MS);
	}
}
