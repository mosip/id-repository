package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.IdentityUpdateTracker;

/**
 * Spring Data JPA repository for `idrepo` IdentityUpdateTracker persistence operations.
 */
public interface IdentityUpdateTrackerRepo extends JpaRepository<IdentityUpdateTracker, String> {

}
