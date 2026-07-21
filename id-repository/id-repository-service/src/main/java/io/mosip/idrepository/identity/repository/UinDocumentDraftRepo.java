package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinDocumentDraft;

/**
 * Spring Data JPA repository for `idrepo` UinDocumentDraft persistence operations.
 */
public interface UinDocumentDraftRepo extends JpaRepository<UinDocumentDraft, String> {
}
