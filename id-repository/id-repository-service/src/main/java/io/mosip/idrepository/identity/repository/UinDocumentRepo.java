package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinDocument;

/**
 * Spring Data JPA repository for `idrepo` UinDocument persistence operations.
 */
public interface UinDocumentRepo extends JpaRepository<UinDocument, String> {
}
