package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinDocumentHistory;

/**
 * Spring Data JPA repository for `idrepo` UinDocumentHistory persistence operations.
 */
public interface UinDocumentHistoryRepo extends JpaRepository<UinDocumentHistory, String> {
}
