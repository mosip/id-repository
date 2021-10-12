package io.mosip.credential.request.generator.repositary;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import org.springframework.stereotype.Repository;


/**
 * The Interface CredentialRepositary.
 *
 * @author Sowmya
 * 
 *         The Interface CredentialRepositary.
 * @param <T> the generic type
 * @param <E> the element type
 */
@Repository
public interface CredentialRepositary<T extends CredentialEntity, E> extends BaseRepository<T, E> {

}
