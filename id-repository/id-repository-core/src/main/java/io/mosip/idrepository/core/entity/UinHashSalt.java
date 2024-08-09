package io.mosip.idrepository.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Class UinHashSalt - Entity class for uin_hash_salt table.
 *
 * @author Prem Kumar.
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "uin_hash_salt")
public class UinHashSalt {
	
	/**  The Id value. */
	@Id
	@Column(name = "id")
	private int id;

	/**  The salt value. */
	@Column(name = "salt")
	private String salt;

	/**  The value to hold created By. */
	@Column(name = "cr_by")
	private String createdBy;

	/**  The value to hold created DTimes. */
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDTimes;

	/**  The value to hold updated By. */
	@Column(name = "upd_by")
	private String updatedBy;

	/**  The value to hold updated Time. */
	@Column(name = "upd_dtimes")
	private LocalDateTime updatedDTimes;
}
