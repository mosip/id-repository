package io.mosip.idrepository.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the {@code uin_hash_salt} table on the idrepo schema.
 *
 * <p>
 * Each row holds one hash-salt bucket used when computing salted hashes of UIN
 * (and related identifiers such as handles). Callers derive a bucket index via
 * {@link io.mosip.idrepository.core.util.SaltUtil} /
 * {@link io.mosip.idrepository.core.security.IdRepoSecurityManager} and load
 * the salt string through {@code UinHashSaltRepo}.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Shard HMAC / hash inputs across many salt values so stored identifier hashes
 * ({@code uin_hash}, {@code handle_hash}, {@code individual_id_hash}, etc.)
 * are not derived from a single global salt. The {@link #id} column is the
 * bucket index; {@link #salt} is the salt material for hashing.
 * </p>
 *
 * <h2>Table / persistence unit</h2>
 * <ul>
 *   <li>Schema / table: {@code mosip_idrepo.uin_hash_salt}
 *       (mapped as {@code @Table(name = "uin_hash_salt")} on PU1)</li>
 *   <li>Persistence unit: PU1 ({@code mosip_idrepo}) via
 *       {@code IdRepoDataSourceConfig} / {@code UinHashSaltRepo}</li>
 *   <li>Primary key: {@link #id} (integer bucket index)</li>
 * </ul>
 *
 * <h2>Salt routing notes</h2>
 * <p>
 * This entity is the <strong>idrepo</strong> hash-salt table. A parallel table
 * exists on {@code mosip_idmap} for VID crypto
 * ({@code VidUinHashSalt} / {@code VidUinHashSaltRepo}). Always use idrepo
 * repositories for identity/UIN/handle hashing and idmap repositories for VID.
 * Mixing PUs causes silent hash mismatches (lookups miss, credentials fail).
 * Rows are <em>populated</em> by {@code id-repository-salt-generator} (K8s
 * Job), not by the long-lived HTTP service at runtime. Hot-path reads are
 * cached in region {@code uin_hash_salt}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code UinHashSaltRepo#retrieveSaltById(int)} — cached read path</li>
 *   <li>{@link io.mosip.idrepository.core.security.IdRepoSecurityManager} —
 *       {@code getIdHashAndAttributes} and related hash helpers</li>
 *   <li>Identity / handle / credential-status writers that persist salted hashes</li>
 *   <li>{@code id-repository-salt-generator} — inserts salt rows after DB deploy</li>
 * </ul>
 *
 * <h2>IDA note</h2>
 * <p>
 * ID Authentication does <strong>not</strong> read id-repo
 * {@code uin_hash_salt} tables. IDA uses its own salt schema where required.
 * {@link io.mosip.idrepository.core.util.SaltUtil} remains part of the published
 * core API surface for tooling; do not rename public salt helpers without an
 * IDA-coordinated release, even though IDA does not query these entity rows.
 * </p>
 *
 * @author Prem Kumar
 * @see UinEncryptSalt
 * @see io.mosip.idrepository.core.repository.UinHashSaltRepo
 * @see io.mosip.idrepository.core.util.SaltUtil
 * @see io.mosip.idrepository.core.security.IdRepoSecurityManager
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "uin_hash_salt")
public class UinHashSalt {

	/**
	 * Salt-table bucket index (primary key).
	 * <p>
	 * Derived from the individual identifier via {@code SaltUtil} /
	 * {@code IdRepoSecurityManager} using {@code mosip.idrepo.salt.key.length}.
	 * </p>
	 */
	@Id
	@Column(name = "id")
	private int id;

	/**
	 * Hash salt material for this bucket.
	 * <p>
	 * Applied when computing salted HMAC/hashes for identifiers that map to
	 * {@link #id}.
	 * </p>
	 */
	@Column(name = "salt")
	private String salt;

	/**
	 * Audit: user or system that created this salt row
	 * (typically the salt-generator Job).
	 */
	@Column(name = "cr_by")
	private String createdBy;

	/**
	 * Audit: UTC timestamp when this salt row was created.
	 */
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDTimes;

	/**
	 * Audit: user or system that last updated this salt row.
	 */
	@Column(name = "upd_by")
	private String updatedBy;

	/**
	 * Audit: UTC timestamp of the last update.
	 */
	@Column(name = "upd_dtimes")
	private LocalDateTime updatedDTimes;
}
