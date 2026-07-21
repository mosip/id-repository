package io.mosip.idrepository.vid.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.core.entity.UinHashSalt;

/**
 * VID hash salts in {@code mosip_idmap} (separate from identity {@code mosip_idrepo} salts).
 */
public interface VidUinHashSaltRepo extends JpaRepository<UinHashSalt, Integer> {

	@Cacheable(cacheNames = "vid_uin_hash_salt", unless = "#result == null")
	@Query("select salt from UinHashSalt where id = :id")
	String retrieveSaltById(@Param("id") int id);
}
