package io.mosip.credential.request.generator.repositary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;


@Repository
public interface CredentialRepositary<T extends CredentialEntity, E> extends BaseRepository<T, E> {


	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode=:statusCode")
	Page<CredentialEntity> findCredentialByStatusCode(@Param("statusCode")String statusCode, Pageable pageable);
}
