package io.mosip.credential.request.generator.repositary;

import io.mosip.credential.request.generator.entity.CredentialIssuedEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialIssuedRepository<T extends CredentialIssuedEntity, E> extends BaseRepository<T, E> {
}
