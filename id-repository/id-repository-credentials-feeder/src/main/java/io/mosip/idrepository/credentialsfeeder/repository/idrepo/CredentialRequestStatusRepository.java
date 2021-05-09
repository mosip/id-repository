package io.mosip.idrepository.credentialsfeeder.repository.idrepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestStatusEntity;

/**
 * The Interface CredentialRequestStatusRepository.
 *
 * @author Loganathan Sekar
 */
public interface CredentialRequestStatusRepository extends JpaRepository<CredentialRequestStatusEntity, Long> {
	
	@Query(value = "select new CredentialRequestStatusEntity(individualId, idExpiryDtimes, idTransactionLimit) where status = 'REQUESTED' order by createDtimes")
	Page<String> findByRequestedStatusOrderByCrdtimes(Pageable pageable);
}
