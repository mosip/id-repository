package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinBiometricHistory;

/**
 * Spring Data JPA repository for `idrepo` UinBiometricHistory persistence operations.
 */
public interface UinBiometricHistoryRepo extends JpaRepository<UinBiometricHistory, String> {

}
