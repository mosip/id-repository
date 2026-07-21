package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinBiometricDraft;

/**
 * Spring Data JPA repository for `idrepo` UinBiometricDraft persistence operations.
 */
public interface UinBiometricDraftRepo extends JpaRepository<UinBiometricDraft, String> {
}
