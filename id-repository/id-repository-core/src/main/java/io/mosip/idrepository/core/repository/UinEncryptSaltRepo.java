package io.mosip.idrepository.core.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.entity.UinEncryptSalt;

/**
 * The Interface UinEncryptSaltRepo.
 *
 * @author Prem Kumar
 */
public interface UinEncryptSaltRepo extends JpaRepository<UinEncryptSalt, Integer>{
	
	/**
	 * The Query to retrieve salt by passing id as parameter.
	 *
	 * @param id the id
	 * @return String salt
	 */
	@Cacheable(cacheNames = IdRepoConstants.UIN_ENCRYPT_SALT_CACHE , unless = "#result == null")
	@Query("select salt from UinEncryptSalt where id = :id")
	public String retrieveSaltById(@Param("id") int id);
}
