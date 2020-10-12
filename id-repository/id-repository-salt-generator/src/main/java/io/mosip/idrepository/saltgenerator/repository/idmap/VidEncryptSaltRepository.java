package io.mosip.idrepository.saltgenerator.repository.idmap;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity;

/**
 * The Interface SaltRepository.
 *
 * @author Manoj SP
 */
public interface VidEncryptSaltRepository extends JpaRepository<VidEncryptSaltEntity, Long> {

	/**
	 * Count by id in list of ids.
	 *
	 * @param ids the ids
	 * @return the long
	 */
	Long countByIdIn(List<Long> ids);
}
