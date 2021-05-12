package io.mosip.idrepository.core.repository;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.core.entity.CredentialRequestStatus;

/**
 * @author Manoj SP
 *
 */
@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface CredentialRequestStatusRepo extends JpaRepository<CredentialRequestStatus, String> {

	Optional<CredentialRequestStatus> findByIndividualIdAndIsDeleted(String individualId, boolean isDeleted);

	default Optional<CredentialRequestStatus> findByIndividualId(String individualId) {
		return this.findByIndividualIdAndIsDeleted(individualId, false);
	}

	Optional<CredentialRequestStatus> findByIndividualIdHashAndIsDeleted(String idHash, boolean isDeleted);

	default Optional<CredentialRequestStatus> findByIndividualIdHash(String individualIdHash) {
		return this.findByIndividualIdHashAndIsDeleted(individualIdHash, false);
	}

}
