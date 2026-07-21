package io.mosip.idrepository.core.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.core.entity.UinEncryptSalt;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SaltUtil;

/**
 * Spring Data repository for {@link UinEncryptSalt} rows in
 * {@code mosip_idrepo.uin_encrypt_salt}.
 *
 * <p>
 * Provides cached lookup of encrypt-salt values by bucket index. Used when identity
 * demographic / biometric blobs are encrypted with per-row salts via
 * {@link IdRepoSecurityManager#encryptWithSalt(byte[], byte[], String)} and decrypted
 * with the matching salt.
 * </p>
 *
 * <h2>Persistence unit</h2>
 * <p>
 * Bound to PU1 ({@code mosip_idrepo}) via {@code IdRepoDataSourceConfig}. VID encrypt
 * salts on {@code mosip_idmap} use a separate repository — do not mix.
 * </p>
 *
 * <h2>Caching</h2>
 * <p>
 * Results are cached in region {@code uin_encrypt_salt} ({@code unless = "#result == null"}).
 * Refresh caches after salt-generator runs so new buckets are visible without restart.
 * </p>
 *
 * <h2>Writes</h2>
 * <p>
 * Rows are populated by {@code id-repository-salt-generator}. This repository is
 * read-heavy at runtime.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * int bucket = securityManager.getSaltKeyForId(uin);
 * String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(bucket);
 * byte[] cipher = securityManager.encryptWithSalt(plain, encryptSalt.getBytes(), refId);
 * </pre>
 *
 * @author Prem Kumar
 * @see UinEncryptSalt
 * @see UinHashSaltRepo
 * @see IdRepoSecurityManager#encryptWithSalt(byte[], byte[], String)
 * @see SaltUtil
 * @see EnvUtil#getIdrepoSaltKeyLength()
 */
public interface UinEncryptSaltRepo extends JpaRepository<UinEncryptSalt, Integer>{
	
	/**
	 * Retrieves the encrypt salt string for the given salt-table bucket index.
	 * <p>
	 * JPQL selects only the {@code salt} column. Cached under {@code uin_encrypt_salt};
	 * {@code null} results are not cached.
	 * </p>
	 *
	 * @param id salt bucket index (modulo from {@link SaltUtil}); must match a row
	 *           populated by salt-generator
	 * @return salt string for envelope encryption/decryption, or {@code null} if no row
	 *         exists for {@code id}
	 */
	@Cacheable(cacheNames = "uin_encrypt_salt" , unless = "#result == null")
	@Query("select salt from UinEncryptSalt where id = :id")
	public String retrieveSaltById(@Param("id") int id);
}
