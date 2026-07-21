package io.mosip.idrepository.saltgenerator.entity.idmap;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.mosip.idrepository.saltgenerator.entity.ISaltEntity;
import lombok.Data;

/**
 * JPA mapping for {@code idmap.uin_encrypt_salt} on {@code mosip_idmap}.
 *
 * <p>
 * Reference entity documenting the VID encrypt-salt table (schema {@code idmap}). Runtime
 * inserts use JDBC rather than this entity.
 * </p>
 *
 * @author MOSIP
 * @see ISaltEntity
 * @see VidHashSaltEntity
 */
@Entity
@Data
@Table(name = "uin_encrypt_salt", schema = "idmap")
public class VidEncryptSaltEntity implements ISaltEntity {

	/** Salt bucket id (primary key). */
	@Id
	@Column(updatable = false, nullable = false, unique = true)
	private Long id;

	/** Base64-encoded encrypt salt. */
	@Column(updatable = true, nullable = false, unique = true)
	private String salt;

	/** Create-audit user ({@code cr_by}). */
	@Column(name = "cr_by", updatable = true, nullable = false, unique = false)
	private String createdBy;

	/** Create timestamp ({@code cr_dtimes}). */
	@Column(name = "cr_dtimes", updatable = true, nullable = false, unique = false)
	private LocalDateTime createDtimes;

	/** Update-audit user ({@code upd_by}). */
	@Column(name = "upd_by", updatable = true, nullable = true, unique = false)
	private String updatedBy;

	/** Update timestamp ({@code upd_dtimes}). */
	@Column(name = "upd_dtimes", updatable = true, nullable = true, unique = true)
	private LocalDateTime updatedDtimes;

}
