package io.mosip.idrepository.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the {@code uin_encrypt_salt} table on the idrepo schema.
 *
 * <p>
 * Each row holds one encryption-salt bucket used when encrypting UIN (and
 * related) payloads. Callers derive a bucket index via
 * {@link io.mosip.idrepository.core.util.SaltUtil} /
 * {@link io.mosip.idrepository.core.security.IdRepoSecurityManager} and load
 * the salt string through {@code UinEncryptSaltRepo}.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Shard encryption keys across many salt values so ciphertext is not derived
 * from a single global secret. The {@link #id} column is the bucket index;
 * {@link #salt} is the salt material applied during encrypt/decrypt.
 * </p>
 *
 * <h2>Table / persistence unit</h2>
 * <ul>
 *   <li>Schema / table: {@code mosip_idrepo.uin_encrypt_salt}
 *       (mapped as {@code @Table(name = "uin_encrypt_salt")} on PU1)</li>
 *   <li>Persistence unit: PU1 ({@code mosip_idrepo}) via
 *       {@code IdRepoDataSourceConfig} / {@code UinEncryptSaltRepo}</li>
 *   <li>Primary key: {@link #id} (integer bucket index)</li>
 * </ul>
 *
 * <h2>Salt routing notes</h2>
 * <p>
 * This entity is the <strong>idrepo</strong> encrypt-salt table. A parallel
 * table exists on {@code mosip_idmap} for VID crypto
 * ({@code VidUinEncryptSalt} / {@code VidUinEncryptSaltRepo}). Always use
 * idrepo repositories for identity/UIN encryption and idmap repositories for
 * VID. Mixing PUs causes silent decrypt failures at scale. Rows are
 * <em>populated</em> by {@code id-repository-salt-generator} (K8s Job), not by
 * the long-lived HTTP service at runtime.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code UinEncryptSaltRepo#retrieveSaltById(int)} — cached read path</li>
 *   <li>{@link io.mosip.idrepository.core.security.IdRepoSecurityManager} —
 *       encrypt/decrypt using the salt for the derived bucket</li>
 *   <li>{@code id-repository-salt-generator} — inserts salt rows after DB deploy</li>
 * </ul>
 *
 * <h2>IDA note</h2>
 * <p>
 * ID Authentication does <strong>not</strong> read id-repo
 * {@code uin_encrypt_salt} tables. IDA maintains its own salt schema where
 * needed. Do not rename this entity or break {@code UinEncryptSaltRepo} without
 * coordinating id-repository crypto consumers; IDA remains unaffected by
 * idrepo salt DDL as long as external REST/WebSub contracts stay stable.
 * </p>
 *
 * @author Prem Kumar
 * @see UinHashSalt
 * @see io.mosip.idrepository.core.repository.UinEncryptSaltRepo
 * @see io.mosip.idrepository.core.util.SaltUtil
 * @see io.mosip.idrepository.core.security.IdRepoSecurityManager
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "uin_encrypt_salt")
public class UinEncryptSalt {

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
	 * Encryption salt material for this bucket.
	 * <p>
	 * Applied when encrypting/decrypting UIN-related payloads for identifiers
	 * that map to {@link #id}.
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
