package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.identity.entity.UinBiometricDraft;

/**
 * The Interface UinBiometricRepo.
 *
 * @author Manoj SP
 */
public interface UinBiometricDraftRepo extends JpaRepository<UinBiometricDraft, String> {


	@Transactional
	void deleteByRegId(String regId);
}
