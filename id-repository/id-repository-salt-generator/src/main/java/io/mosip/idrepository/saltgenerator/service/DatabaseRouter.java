package io.mosip.idrepository.saltgenerator.service;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import io.mosip.idrepository.core.config.IdRepoHikariDataSourceFactory;
import io.mosip.idrepository.core.util.EnvUtil;

/**
 * Registers dedicated Hikari datasources for the salt-generator Job.
 *
 * <p>
 * Reuses {@link IdRepoHikariDataSourceFactory} for JDBC URL / credentials / defaults, then
 * shrinks pool size for a short-lived Job (max 2, min idle 1) and renames pools for metrics.
 * Identity and idmap salts must never share one datasource.
 * </p>
 *
 * <h2>Beans</h2>
 * <table border="1" summary="Salt-generator datasources">
 *   <tr><th>Bean name</th><th>Database</th><th>Pool name</th><th>Factory method</th></tr>
 *   <tr>
 *     <td>{@code primaryDataSource}</td>
 *     <td>{@code mosip_idrepo}</td>
 *     <td>{@code saltgen-idrepo-pool}</td>
 *     <td>{@link IdRepoHikariDataSourceFactory#identityPool(EnvUtil)}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code secondaryDataSource}</td>
 *     <td>{@code mosip_idmap}</td>
 *     <td>{@code saltgen-idmap-pool}</td>
 *     <td>{@link IdRepoHikariDataSourceFactory#vidPool(EnvUtil)}</td>
 *   </tr>
 * </table>
 *
 * @author MOSIP
 * @see SaltJdbcWriter
 * @see io.mosip.idrepository.saltgenerator.config.SaltGeneratorConfiguration
 */
@Configuration
public class DatabaseRouter {

	private final EnvUtil env;

	/**
	 * @param env property accessor loaded from Spring Cloud Config
	 */
	public DatabaseRouter(EnvUtil env) {
		this.env = env;
	}

	/**
	 * Identity DB pool used for {@code idrepo.uin_*_salt} inserts.
	 *
	 * @return Hikari datasource with Job-sized pool settings
	 */
	@Bean(name = "primaryDataSource")
	public DataSource primaryDataSource() {
		HikariDataSource ds = IdRepoHikariDataSourceFactory.identityPool(env);
		ds.setMaximumPoolSize(2);
		ds.setMinimumIdle(1);
		ds.setPoolName("saltgen-idrepo-pool");
		return ds;
	}

	/**
	 * VID / idmap DB pool used for {@code idmap.uin_*_salt} inserts.
	 *
	 * @return Hikari datasource with Job-sized pool settings
	 */
	@Bean(name = "secondaryDataSource")
	public DataSource secondaryDataSource() {
		HikariDataSource ds = IdRepoHikariDataSourceFactory.vidPool(env);
		ds.setMaximumPoolSize(2);
		ds.setMinimumIdle(1);
		ds.setPoolName("saltgen-idmap-pool");
		return ds;
	}
}
