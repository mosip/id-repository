package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinDocumentDraft;

/**
 * The Interface UinDocumentRepo.
 *
 * @author Manoj SP
 */
public interface UinDocumentDraftRepo extends JpaRepository<UinDocumentDraft, String> {
}
