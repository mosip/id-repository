package io.mosip.idrepository.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the {@code idrepo.handle} table.
 *
 * <p>
 * Maps a user-chosen handle (e.g. {@code @phone}) to a UIN via salted hashes.
 * Handles are created during identity registration/update and resolved during
 * credential issuance when a request identifies the individual by handle rather
 * than UIN/VID.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Persist handle metadata separately from the main UIN identity row while still
 * participating in the shared {@link HandleInfo} / {@link UinInfo} interceptor
 * contract. Plaintext UIN and encrypted UIN bytes are <em>not</em> stored on
 * this table; only {@link #uinHash}, {@link #handle}, and {@link #handleHash}
 * link the handle to an individual.
 * </p>
 *
 * <h2>Table / persistence unit</h2>
 * <ul>
 *   <li>Schema / table: {@code mosip_idrepo.handle}
 *       ({@code @Table(..., schema = "idrepo")})</li>
 *   <li>Persistence unit: PU1 ({@code mosip_idrepo}) via
 *       {@code idRepoDataSource}</li>
 *   <li>Primary key: {@link #id} — UUID assigned at insert time</li>
 *   <li>Entity is registered only when bean {@code idRepoDataSource} exists
 *       ({@link ConditionalOnBean})</li>
 * </ul>
 *
 * <h2>Salt routing notes</h2>
 * <p>
 * {@link #uinHash} and {@link #handleHash} are produced with idrepo hash salts
 * ({@code mosip_idrepo.uin_hash_salt}) via
 * {@link io.mosip.idrepository.core.security.IdRepoSecurityManager} /
 * {@link io.mosip.idrepository.core.util.SaltUtil}. Do <strong>not</strong> use
 * idmap ({@code mosip_idmap}) VID salt repositories for these columns —
 * mis-routing causes silent lookup failures. The plaintext {@link #handle}
 * value is encrypted by
 * {@link io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor}
 * before flush.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link io.mosip.idrepository.core.repository.HandleRepo} — CRUD / lookup</li>
 *   <li>{@code IdRepoServiceImpl} — create/update handles on identity write</li>
 *   <li>{@code CredentialServiceManager} — resolve handle → individual for issuance</li>
 *   <li>{@code IdRepoEntityInterceptor} — encrypt handle / apply {@link HandleInfo} contract</li>
 * </ul>
 *
 * <h2>IDA note</h2>
 * <p>
 * IDA does <strong>not</strong> read {@code idrepo.handle} or id-repo salt
 * tables. Handle resolution is an id-repository concern before credential /
 * Datashare payloads are published for authentication partners.
 * </p>
 *
 * @see HandleInfo
 * @see UinInfo
 * @see io.mosip.idrepository.core.repository.HandleRepo
 * @see io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor
 */
@NoArgsConstructor
@Data
@Table(name = "handle", schema = "idrepo")
@Entity
@ConditionalOnBean(name = { "idRepoDataSource" })
public class Handle implements HandleInfo {

	/**
	 * Primary key — UUID assigned when the handle row is created.
	 */
	@Id
	@Column(name = "id")
	private String id;

	/**
	 * Salted hash of the linked UIN.
	 * <p>
	 * Used to find all handles belonging to an individual without storing
	 * plaintext UIN on this table.
	 * </p>
	 */
	@Column(name = "uin_hash")
	private String uinHash;

	/**
	 * Plaintext handle value in memory; encrypted by the entity interceptor
	 * before flush to the {@code handle} column.
	 */
	@Column(name = "handle")
	private String handle;

	/**
	 * Salted hash of the handle.
	 * <p>
	 * Used for uniqueness checks and reverse lookups (handle → individual).
	 * </p>
	 */
	@Column(name = "handle_hash")
	private String handleHash;

	/**
	 * Audit: user or system that created this row.
	 */
	@Column(name = "cr_by")
	private String createdBy;

	/**
	 * Audit: UTC timestamp when this row was created.
	 */
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDateTime;

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns the in-memory handle value (plaintext before encrypt-on-save /
	 * decrypted after load via the interceptor).
	 * </p>
	 */
	@Override
	public String getHandle() {
		return handle;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Sets the in-memory handle; persistence encryption is applied by
	 * {@code IdRepoEntityInterceptor}.
	 * </p>
	 */
	@Override
	public void setHandle(String handle) {
		this.handle = handle;
	}

	/**
	 * Not stored on the handle table; plaintext UIN lives on
	 * {@link io.mosip.idrepository.identity.entity.Uin}.
	 *
	 * @return always {@code null}
	 */
	@Override
	public String getUin() {
		return null;
	}

	/**
	 * No-op — handle rows do not persist plaintext UIN.
	 *
	 * @param uin ignored; present only to satisfy {@link UinInfo}
	 */
	@Override
	public void setUin(String uin) {
	}

	/**
	 * Not stored on the handle table.
	 *
	 * @return always {@code null}
	 */
	@Override
	public byte[] getUinData() {
		return null;
	}

	/**
	 * No-op — handle rows do not persist encrypted UIN bytes.
	 *
	 * @param uinData ignored; present only to satisfy {@link UinInfo}
	 */
	@Override
	public void setUinData(byte[] uinData) {
	}

	/**
	 * No-op — handle rows do not track update audit via {@link UinInfo} setters.
	 *
	 * @param updatedBy ignored
	 */
	@Override
	public void setUpdatedBy(String updatedBy) {
	}

	/**
	 * No-op — handle rows do not track update audit via {@link UinInfo} setters.
	 *
	 * @param updatedDTimes ignored
	 */
	@Override
	public void setUpdatedDateTime(LocalDateTime updatedDTimes) {
	}
}
