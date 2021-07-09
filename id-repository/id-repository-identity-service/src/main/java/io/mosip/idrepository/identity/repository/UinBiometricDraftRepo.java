package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinBiometricDraft;

/**
 * The Interface UinBiometricRepo.
 *
 * @author Manoj SP
 */
public interface UinBiometricDraftRepo extends JpaRepository<UinBiometricDraft, String> {
}
