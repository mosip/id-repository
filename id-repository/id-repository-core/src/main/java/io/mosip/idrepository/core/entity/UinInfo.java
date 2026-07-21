package io.mosip.idrepository.core.entity;

import java.time.LocalDateTime;

/**
 * Contract for JPA entities that store encrypted UIN payload data and related
 * salted hash / audit fields.
 *
 * <p>
 * Implemented by identity entities such as
 * {@link io.mosip.idrepository.identity.entity.Uin},
 * {@link io.mosip.idrepository.identity.entity.UinHistory}, and
 * {@link io.mosip.idrepository.identity.entity.UinDraft}, and extended by
 * {@link HandleInfo} for handle rows. The
 * {@link io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor}
 * uses this interface to encrypt {@link #getUin()} / {@link #getUinData()} on
 * persist and decrypt on load without entity-specific branching.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Decouple crypto and audit side-effects from concrete JPA types. Any entity
 * that participates in UIN encrypt-on-save / decrypt-on-load must expose
 * plaintext UIN (transient), encrypted UIN bytes, salted UIN hash, and update
 * audit setters through this contract.
 * </p>
 *
 * <h2>Table / persistence unit</h2>
 * <p>
 * Implementors typically map to tables under {@code mosip_idrepo} on PU1
 * ({@code idRepoDataSource}): e.g. {@code uin}, {@code uin_h}, {@code uin_draft},
 * and via {@link HandleInfo} the {@code handle} table. This interface itself is
 * not a JPA entity and has no table mapping.
 * </p>
 *
 * <h2>Salt routing notes</h2>
 * <p>
 * {@link #getUinHash()} / {@link #setUinHash(String)} values are produced with
 * idrepo {@code uin_hash_salt}; encryption of {@link #getUin()} into
 * {@link #getUinData()} uses idrepo {@code uin_encrypt_salt}. Both live on
 * {@code mosip_idrepo} (PU1). Do <strong>not</strong> use idmap VID salt
 * repositories ({@code VidUinHashSaltRepo} / {@code VidUinEncryptSaltRepo}) for
 * these fields — mis-routing causes silent crypto failure. Salt rows are
 * populated by {@code id-repository-salt-generator}, not by HTTP request paths.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code IdRepoEntityInterceptor} — encrypt/decrypt via this contract</li>
 *   <li>Identity service entities ({@code Uin}, {@code UinHistory},
 *       {@code UinDraft})</li>
 *   <li>{@link Handle} / {@link HandleInfo} — partial implementors (UIN payload
 *       accessors may be no-ops)</li>
 *   <li>{@link io.mosip.idrepository.core.security.IdRepoSecurityManager} —
 *       underlying hash/encrypt helpers</li>
 * </ul>
 *
 * <h2>IDA note</h2>
 * <p>
 * IDA does <strong>not</strong> load these JPA entities or id-repo salt tables.
 * It consumes identity/credential outcomes through REST, Datashare, and WebSub.
 * Keep the published {@code io.mosip.idrepository.core.*} API stable for IDA;
 * changes to this internal persistence contract should not alter external
 * payload shapes IDA depends on.
 * </p>
 *
 * @see HandleInfo
 * @see Handle
 * @see io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor
 * @see io.mosip.idrepository.identity.entity.Uin
 * @see UinHashSalt
 * @see UinEncryptSalt
 */
public interface UinInfo {

	/**
	 * Returns the plaintext UIN held in memory for encrypt-on-save flows.
	 * <p>
	 * Typically cleared or unused after encryption; not the durable DB form.
	 * </p>
	 *
	 * @return plaintext UIN (transient; cleared after encryption on save)
	 */
	String getUin();

	/**
	 * Sets the plaintext UIN to encrypt before persistence.
	 *
	 * @param uin plaintext UIN to encrypt before persistence
	 */
	void setUin(String uin);

	/**
	 * Returns the encrypted UIN payload bytes stored (or about to be stored)
	 * in the database column.
	 *
	 * @return encrypted UIN payload bytes
	 */
	byte[] getUinData();

	/**
	 * Sets the encrypted UIN payload bytes.
	 *
	 * @param uinData encrypted UIN payload bytes
	 */
	void setUinData(byte[] uinData);

	/**
	 * Returns the salted hash of the UIN used for indexed lookups.
	 * <p>
	 * Derived with idrepo {@code uin_hash_salt} via
	 * {@code IdRepoSecurityManager} / {@code SaltUtil}.
	 * </p>
	 *
	 * @return salted hash of the UIN used for indexed lookups
	 */
	String getUinHash();

	/**
	 * Sets the salted hash of the UIN.
	 *
	 * @param hash salted UIN hash
	 */
	void setUinHash(String hash);

	/**
	 * Sets the audit user or system identifier for the last update.
	 *
	 * @param updatedBy audit user or system identifier for the last update
	 */
	void setUpdatedBy(String updatedBy);

	/**
	 * Sets the UTC timestamp of the last update.
	 *
	 * @param updatedDTimes UTC timestamp of the last update
	 */
	void setUpdatedDateTime(LocalDateTime updatedDTimes);
}
