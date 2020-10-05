package io.mosip.idrepository.saltgenerator.entity;

import java.time.LocalDateTime;

public interface ISaltEntity {

	Long getId();

	String getSalt();

	String getCreatedBy();

	LocalDateTime getCreateDtimes();

	String getUpdatedBy();

	LocalDateTime getUpdatedDtimes();

	void setId(Long id);

	void setSalt(String salt);

	void setCreatedBy(String createdBy);

	void setCreateDtimes(LocalDateTime createdDtimes);

	void setUpdatedBy(String updatedBy);

	void setUpdatedDtimes(LocalDateTime updatedDtimes);

}
