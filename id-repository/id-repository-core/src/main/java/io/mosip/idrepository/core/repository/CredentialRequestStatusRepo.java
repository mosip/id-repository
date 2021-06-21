package io.mosip.idrepository.core.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.core.entity.CredentialRequestStatus;

/**
 * @author Manoj SP
 *
 */
@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface CredentialRequestStatusRepo extends JpaRepository<CredentialRequestStatus, String> {

	List<CredentialRequestStatus> findByIndividualIdAndIsDeleted(String individualId, boolean isDeleted);

	default List<CredentialRequestStatus> findByIndividualId(String individualId) {
		return this.findByIndividualIdAndIsDeleted(individualId, false);
	}

	List<CredentialRequestStatus> findByIndividualIdHashAndIsDeleted(String individualIdHash, boolean isDeleted);

	default List<CredentialRequestStatus> findByIndividualIdHash(String individualIdHash) {
		return this.findByIndividualIdHashAndIsDeleted(individualIdHash, false);
	}

	Optional<CredentialRequestStatus> findByIndividualIdHashAndPartnerIdAndIsDeleted(String idHash, String partnerId, boolean isDeleted);

	default Optional<CredentialRequestStatus> findByIndividualIdHashAndPartnerId(String individualIdHash, String partnerId) {
		return this.findByIndividualIdHashAndPartnerIdAndIsDeleted(individualIdHash, partnerId, false);
	}
	
	List<CredentialRequestStatus> findByStatus(String status);
	
	List<CredentialRequestStatus> findByIdExpiryTimestampBefore(LocalDateTime idExpiryTimestamp);
	
	@Query(value = "SELECT new CredentialRequestStatus( individualId, idExpiryTimestamp, idTransactionLimit, tokenId, partnerId ) "
			+ "FROM CredentialRequestStatus crs "
			+ "WHERE crs.crDTimes < :beforeCreateDtimes AND crs.status=:status")
	Page<CredentialRequestStatus> findByRequestedStatusBeforeCrDtimes(
			@Param("beforeCreateDtimes") LocalDateTime beforeCreateDtimes, @Param("status") String status,
			Pageable pageable);

}
