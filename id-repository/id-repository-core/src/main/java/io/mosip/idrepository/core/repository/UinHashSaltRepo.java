package io.mosip.idrepository.core.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.core.entity.UinHashSalt;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SaltUtil;

/**
 * Spring Data repository for {@link UinHashSalt} rows in {@code mosip_idrepo.uin_hash_salt}.
 *
 * <p>
 * Provides cached lookup of hash-salt values by bucket index. Hot-path callers
 * ({@link IdRepoSecurityManager#getIdHashAndAttributes}) use
 * {@link #retrieveSaltById(int)} after deriving the index via {@link SaltUtil} and
 * {@link EnvUtil#getIdrepoSaltKeyLength()}.
 * </p>
 *
 * <h2>Persistence unit</h2>
 * <p>
 * Bound to PU1 ({@code mosip_idrepo}) via {@code IdRepoDataSourceConfig}. Do
 * <strong>not</strong> use this repository for VID crypto — VID salts live on
 * {@code mosip_idmap} with separate repositories. Mixing PUs causes silent hash
 * mismatches.
 * </p>
 *
 * <h2>Caching</h2>
 * <p>
 * Results are cached in region {@code uin_hash_salt} ({@code unless = "#result == null"}).
 * After the salt-generator Job populates new buckets, related caches should be refreshed
 * (see {@link IdRepoSecurityManager#evictIdAttributeCacheAtInterval()} for the related
 * {@code id_attributes} region).
 * </p>
 *
 * <h2>Writes</h2>
 * <p>
 * Salt <em>rows</em> are inserted by {@code id-repository-salt-generator}, not by this
 * repository at runtime. This interface is read-heavy.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * int bucket = securityManager.getSaltKeyForId(uin);
 * String salt = uinHashSaltRepo.retrieveSaltById(bucket);
 * </pre>
 *
 * @author Prem Kumar
 * @see UinHashSalt
 * @see UinEncryptSaltRepo
 * @see IdRepoSecurityManager
 * @see SaltUtil
 */
public interface UinHashSaltRepo extends JpaRepository<UinHashSalt, Integer> {
	
	/**
	 * Retrieves the hash salt string for the given salt-table bucket index.
	 * <p>
	 * JPQL selects only the {@code salt} column. Cached under {@code uin_hash_salt};
	 * {@code null} results are not cached.
	 * </p>
	 *
	 * @param id salt bucket index (modulo from {@link SaltUtil#getIdvidModulo} or
	 *           {@link SaltUtil#getIdvidHashModulo}); must match a row populated by
	 *           salt-generator
	 * @return salt string for HMAC hashing, or {@code null} if no row exists for
	 *         {@code id}
	 */
	@Cacheable(cacheNames = "uin_hash_salt" , unless = "#result == null")
	@Query("select salt from UinHashSalt where id = :id")
	public String retrieveSaltById(@Param("id") int id);
}
