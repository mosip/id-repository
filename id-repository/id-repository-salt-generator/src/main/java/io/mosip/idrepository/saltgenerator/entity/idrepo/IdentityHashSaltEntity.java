package io.mosip.idrepository.saltgenerator.entity.idrepo;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import io.mosip.idrepository.saltgenerator.entity.ISaltEntity;
import lombok.Data;

/**
 * The Class SaltEntity.
 *
 * @author Manoj SP
 */
@Entity
@Table(name = "uin_hash_salt")

/**
 * Instantiates a new salt entity.
 */
@Data
public class IdentityHashSaltEntity implements ISaltEntity{

	/** The id. */
	@Id
	@Column(updatable = false, nullable = false, unique = true)
	private Long id;

	/** The salt. */
	@Column(updatable = true, nullable = false, unique = true)
	private String salt;

	/** The created by. */
	@Column(name = "cr_by", updatable = true, nullable = false, unique = false)
	private String createdBy;

	/** The create dtimes. */
	@Column(name = "cr_dtimes", updatable = true, nullable = false, unique = true)
	private LocalDateTime createDtimes;

	/** The updated by. */
	@Column(name = "upd_by", updatable = true, nullable = true, unique = false)
	private String updatedBy;

	/** The updated dtimes. */
	@Column(name = "upd_dtimes", updatable = true, nullable = true, unique = true)
	private LocalDateTime updatedDtimes;

}
