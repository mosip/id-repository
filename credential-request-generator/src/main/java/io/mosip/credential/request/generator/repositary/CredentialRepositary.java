package io.mosip.credential.request.generator.repositary;

import org.springframework.stereotype.Repository;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

@Repository
public interface CredentialRepositary<T extends CredentialEntity, E> extends BaseRepository<T, E> {

}
