package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinBiometric;

/**
 * Spring Data JPA repository for `idrepo` UinBiometric persistence operations.
 */
public interface UinBiometricRepo extends JpaRepository<UinBiometric, String> {
}
