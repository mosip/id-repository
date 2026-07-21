package io.mosip.idrepository.saltgenerator.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Idempotent JDBC writer for salt tables on {@code mosip_idrepo} and {@code mosip_idmap}.
 *
 * <p>
 * Each chunk is written in two transactions (idrepo then idmap). Inserts use
 * {@code ON CONFLICT (id) DO NOTHING} so concurrent or repeated Job runs do not fail on
 * existing primary keys.
 * </p>
 *
 * <h2>Tables written</h2>
 * <table border="1" summary="Salt tables">
 *   <tr><th>Database</th><th>Table</th><th>Salt source on {@link SaltRow}</th></tr>
 *   <tr><td>idrepo</td><td>{@code idrepo.uin_hash_salt}</td><td>{@link SaltRow#hashSalt()}</td></tr>
 *   <tr><td>idrepo</td><td>{@code idrepo.uin_encrypt_salt}</td><td>{@link SaltRow#identityEncryptSalt()}</td></tr>
 *   <tr><td>idmap</td><td>{@code idmap.uin_hash_salt}</td><td>{@link SaltRow#hashSalt()}</td></tr>
 *   <tr><td>idmap</td><td>{@code idmap.uin_encrypt_salt}</td><td>{@link SaltRow#vidEncryptSalt()}</td></tr>
 * </table>
 *
 * <h2>Resume semantics</h2>
 * {@link #resolveResumeStart(long, long)} reads {@code MAX(id)} from both hash-salt tables and
 * resumes at {@code min(maxes) + 1} (or the configured start if tables are empty). On SQL
 * errors while querying max, returns {@code -1} for that DB so generation can still proceed
 * from the configured start.
 *
 * @author MOSIP
 * @see SaltGenerator
 * @see DatabaseRouter
 * @see SaltRow
 */
@Component
public class SaltJdbcWriter {

	private static final Logger LOGGER = IdRepoLogger.getLogger(SaltJdbcWriter.class);

	/** Audit {@code cr_by} value written for every generated salt row. */
	private static final String CREATED_BY = "System";

	private static final String INSERT_IDREPO_HASH = "INSERT INTO idrepo.uin_hash_salt (id, salt, cr_by, cr_dtimes) "
			+ "VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";

	private static final String INSERT_IDREPO_ENCRYPT = "INSERT INTO idrepo.uin_encrypt_salt (id, salt, cr_by, cr_dtimes) "
			+ "VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";

	private static final String INSERT_IDMAP_HASH = "INSERT INTO idmap.uin_hash_salt (id, salt, cr_by, cr_dtimes) "
			+ "VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";

	private static final String INSERT_IDMAP_ENCRYPT = "INSERT INTO idmap.uin_encrypt_salt (id, salt, cr_by, cr_dtimes) "
			+ "VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";

	private static final String MAX_ID_IDREPO_HASH = "SELECT COALESCE(MAX(id), -1) FROM idrepo.uin_hash_salt";

	private static final String MAX_ID_IDMAP_HASH = "SELECT COALESCE(MAX(id), -1) FROM idmap.uin_hash_salt";

	private final DataSource idRepoDataSource;
	private final DataSource idMapDataSource;

	/**
	 * @param idRepoDataSource primary pool ({@code mosip_idrepo}), bean name {@code primaryDataSource}
	 * @param idMapDataSource  secondary pool ({@code mosip_idmap}), bean name {@code secondaryDataSource}
	 */
	public SaltJdbcWriter(@Qualifier("primaryDataSource") DataSource idRepoDataSource,
			@Qualifier("secondaryDataSource") DataSource idMapDataSource) {
		this.idRepoDataSource = idRepoDataSource;
		this.idMapDataSource = idMapDataSource;
	}

	/**
	 * Computes the first id that still needs generation for the configured range.
	 *
	 * @param configuredStart inclusive start from config
	 * @param configuredEnd   inclusive end from config
	 * @return resume id; may be {@code > configuredEnd} when the range is already full
	 */
	public long resolveResumeStart(long configuredStart, long configuredEnd) {
		long idRepoMax = queryMaxId(idRepoDataSource, MAX_ID_IDREPO_HASH);
		long idMapMax = queryMaxId(idMapDataSource, MAX_ID_IDMAP_HASH);
		long resume = configuredStart;
		if (idRepoMax >= 0 || idMapMax >= 0) {
			long minMax = Math.min(idRepoMax >= 0 ? idRepoMax : Long.MAX_VALUE,
					idMapMax >= 0 ? idMapMax : Long.MAX_VALUE);
			if (minMax != Long.MAX_VALUE) {
				resume = Math.max(configuredStart, minMax + 1);
			}
		}
		if (resume > configuredEnd) {
			LOGGER.info("SALT_GENERATOR", "SaltJdbcWriter", "resolveResumeStart",
					"Salt range already populated through id=" + configuredEnd);
		} else if (resume > configuredStart) {
			LOGGER.info("SALT_GENERATOR", "SaltJdbcWriter", "resolveResumeStart",
					"Resuming salt generation from id=" + resume);
		}
		return resume;
	}

	/**
	 * Writes one chunk to all four salt tables (idrepo hash/encrypt, idmap hash/encrypt).
	 *
	 * @param chunk rows to insert; {@code null} or empty is a no-op
	 * @throws SQLException if either database transaction fails (caller should abort the Job)
	 */
	public void writeChunk(List<SaltRow> chunk) throws SQLException {
		if (chunk == null || chunk.isEmpty()) {
			return;
		}
		writeIdRepoChunk(chunk);
		writeIdMapChunk(chunk);
	}

	/**
	 * Commits hash + encrypt inserts for {@code mosip_idrepo} in one transaction.
	 *
	 * @param chunk non-empty rows
	 * @throws SQLException on batch or commit failure (rolls back first)
	 */
	private void writeIdRepoChunk(List<SaltRow> chunk) throws SQLException {
		try (Connection conn = idRepoDataSource.getConnection()) {
			conn.setAutoCommit(false);
			try {
				batchInsert(conn, INSERT_IDREPO_HASH, chunk, true, false);
				batchInsert(conn, INSERT_IDREPO_ENCRYPT, chunk, false, false);
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}
		}
	}

	/**
	 * Commits hash + encrypt inserts for {@code mosip_idmap} in one transaction.
	 *
	 * @param chunk non-empty rows
	 * @throws SQLException on batch or commit failure (rolls back first)
	 */
	private void writeIdMapChunk(List<SaltRow> chunk) throws SQLException {
		try (Connection conn = idMapDataSource.getConnection()) {
			conn.setAutoCommit(false);
			try {
				batchInsert(conn, INSERT_IDMAP_HASH, chunk, true, true);
				batchInsert(conn, INSERT_IDMAP_ENCRYPT, chunk, false, true);
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}
		}
	}

	/**
	 * Adds each row to a JDBC batch and executes it.
	 *
	 * @param conn        open connection (caller owns transaction)
	 * @param sql         insert with {@code ON CONFLICT DO NOTHING}
	 * @param chunk       rows
	 * @param hashColumn  {@code true} to bind {@link SaltRow#hashSalt()}
	 * @param vidEncrypt  when not hash: {@code true} binds VID encrypt salt, else identity encrypt
	 * @throws SQLException if prepare/bind/execute fails
	 */
	private void batchInsert(Connection conn, String sql, List<SaltRow> chunk, boolean hashColumn, boolean vidEncrypt)
			throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (SaltRow row : chunk) {
				ps.setLong(1, row.id());
				ps.setString(2, hashColumn ? row.hashSalt()
						: (vidEncrypt ? row.vidEncryptSalt() : row.identityEncryptSalt()));
				ps.setString(3, CREATED_BY);
				ps.setTimestamp(4, Timestamp.valueOf(row.createdAt()));
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	/**
	 * Returns {@code MAX(id)} for the given SQL, or {@code -1} when empty / on SQL error.
	 *
	 * @param dataSource idrepo or idmap pool
	 * @param sql        {@code SELECT COALESCE(MAX(id), -1) FROM ...}
	 * @return max id, or {@code -1} if unavailable
	 */
	private long queryMaxId(DataSource dataSource, String sql) {
		try {
			Connection conn = dataSource.getConnection();
			try {
				PreparedStatement ps = conn.prepareStatement(sql);
				try {
					ResultSet rs = ps.executeQuery();
					try {
						if (rs.next()) {
							return rs.getLong(1);
						}
					} finally {
						rs.close();
					}
				} finally {
					ps.close();
				}
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			LOGGER.error("SALT_GENERATOR", "SaltJdbcWriter", "queryMaxId", e.getMessage());
		}
		return -1;
	}
}
