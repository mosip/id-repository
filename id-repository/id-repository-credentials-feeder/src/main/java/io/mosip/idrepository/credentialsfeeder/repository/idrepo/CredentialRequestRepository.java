package io.mosip.idrepository.credentialsfeeder.repository.idrepo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestEntity;

/**
 * The Interface CredentialRequestRepository.
 *
 * @author Manoj SP
 */
public interface CredentialRequestRepository extends JpaRepository<CredentialRequestEntity, Long> {

	/**
	 * Count by id in list of ids.
	 *
	 * @param ids the ids
	 * @return the long
	 */
	Long countByIdIn(List<Long> ids);
}
