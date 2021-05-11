package io.mosip.idrepository.credentialsfeeder.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.credentialsfeeder.entity.CredentialRequestStatusEntity;

/**
 * The Interface CredentialRequestStatusRepository.
 *
 * @author Loganathan Sekar
 */
public interface CredentialRequestStatusRepository extends JpaRepository<CredentialRequestStatusEntity, Long> {
	
	@Query(value = "SELECT new CredentialRequestStatusEntity( individualId, idExpiryDtimes, idTransactionLimit, tokenId, partnerId ) "
			+ "FROM CredentialRequestStatusEntity crs "
			+ "WHERE crs.createDtimes < :beforeCreateDtimes AND crs.status=:status")
	Page<CredentialRequestStatusEntity> findByRequestedStatusBeforeCrDtimesOrderByCrdtimes(
			@Param("beforeCreateDtimes") LocalDateTime beforeCreateDtimes, @Param("status") String status,
			Pageable pageable);
}
