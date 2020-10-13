package io.mosip.idrepository.saltgenerator.repository.idrepo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;

/**
 * The Interface SaltRepository.
 *
 * @author Manoj SP
 */
public interface IdentityHashSaltRepository extends JpaRepository<IdentityHashSaltEntity, Long> {

	/**
	 * Count by id in list of ids.
	 *
	 * @param ids the ids
	 * @return the long
	 */
	Long countByIdIn(List<Long> ids);
}
