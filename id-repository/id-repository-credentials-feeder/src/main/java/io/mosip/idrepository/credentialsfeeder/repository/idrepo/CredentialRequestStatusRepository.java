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
	
	@Query(value = "SELECT individual_id FROM ( "
					+ "	SELECT individual_id, MIN(individual_id_hash) "
					+ "		FROM CREDENTIAL_REQUEST_STATUS "
					+ "		WHERE STATUS = 'REQUESTED' "
					+ "		GROUP BY individual_id_hash "
					+ "		ORDER BY cr_dtimes ASC )", 
					nativeQuery = true)
	Page<String> findAllDistinctIndividualIdsWithRequestedStatus(Pageable pageable);
}
