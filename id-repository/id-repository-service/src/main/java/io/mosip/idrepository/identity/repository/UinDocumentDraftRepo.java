package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.identity.entity.UinDocumentDraft;

/**
 * Spring Data JPA repository for `idrepo` UinDocumentDraft persistence operations.
 */
public interface UinDocumentDraftRepo extends JpaRepository<UinDocumentDraft, String> {

	/**
	 * Deletes every demographic document draft row of a registration. Used when a V2
	 * draft is published or discarded. Runs in its own transaction when none is active.
	 *
	 * @param regId registration id of the draft
	 */
	@Transactional
	void deleteByRegId(String regId);
}
