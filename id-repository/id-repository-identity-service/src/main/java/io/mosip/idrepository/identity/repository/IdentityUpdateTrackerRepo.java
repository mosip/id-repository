package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.IdentityUpdateTracker;

/**
 * @author Manoj SP
 *
 */
public interface IdentityUpdateTrackerRepo extends JpaRepository<IdentityUpdateTracker, String> {

}
