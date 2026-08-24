package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.identity.entity.UinDocumentDraft;

/**
 * The Interface UinDocumentRepo.
 *
 * @author Manoj SP
 */
public interface UinDocumentDraftRepo extends JpaRepository<UinDocumentDraft, String> {

	@Transactional
	void deleteByRegId(String regId);
}
