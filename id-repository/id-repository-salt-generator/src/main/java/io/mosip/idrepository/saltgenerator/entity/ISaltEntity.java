package io.mosip.idrepository.saltgenerator.entity;

import java.time.LocalDateTime;

/**
 * Common contract for salt-table JPA entities (identity and VID schemas).
 *
 * <p>
 * Maps the shared columns {@code id}, {@code salt}, {@code cr_by}, {@code cr_dtimes},
 * {@code upd_by}, and {@code upd_dtimes}. Implementations are reference models; the Job
 * persists via JDBC in {@link io.mosip.idrepository.saltgenerator.service.SaltJdbcWriter}.
 * </p>
 *
 * @author MOSIP
 * @see io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity
 * @see io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityEncryptSaltEntity
 * @see io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity
 * @see io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity
 */
public interface ISaltEntity {

	/** @return salt bucket primary key */
	Long getId();

	/** @return Base64-encoded salt value */
	String getSalt();

	/** @return create-audit user ({@code cr_by}) */
	String getCreatedBy();

	/** @return create timestamp ({@code cr_dtimes}) */
	LocalDateTime getCreateDtimes();

	/** @return update-audit user ({@code upd_by}), may be {@code null} */
	String getUpdatedBy();

	/** @return update timestamp ({@code upd_dtimes}), may be {@code null} */
	LocalDateTime getUpdatedDtimes();

	/** @param id salt bucket primary key */
	void setId(Long id);

	/** @param salt Base64-encoded salt value */
	void setSalt(String salt);

	/** @param createdBy create-audit user */
	void setCreatedBy(String createdBy);

	/** @param createdDtimes create timestamp */
	void setCreateDtimes(LocalDateTime createdDtimes);

	/** @param updatedBy update-audit user */
	void setUpdatedBy(String updatedBy);

	/** @param updatedDtimes update timestamp */
	void setUpdatedDtimes(LocalDateTime updatedDtimes);

}
