package io.mosip.credential.request.generator.repositary;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.entity.CredentialFailedEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

@Repository
public interface CredentialFailedRepository<T extends CredentialFailedEntity, E> extends BaseRepository<T, E> {

    @Transactional
    @Lock(value = LockModeType.PESSIMISTIC_WRITE) // adds 'FOR UPDATE' statement
    @QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "1") })
    @Query("SELECT crdn FROM CredentialFailedEntity crdn WHERE crdn.request like %:type% ")
    Page<CredentialFailedEntity> findFailedCredential(@Param("type") String type, Pageable pageable);
}
