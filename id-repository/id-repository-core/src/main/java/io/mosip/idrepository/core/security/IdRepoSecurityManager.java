package io.mosip.idrepository.core.security;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CACHE_UPDATE_DEFAULT_INTERVAL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDREPO_CACHE_UPDATE_INTERVAL;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SaltUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.HMACUtils2;
import lombok.NoArgsConstructor;

/**
 * Central security and crypto helper for ID Repository.
 *
 * <p>
 * Single entry point for kernel-cryptomanager encrypt/decrypt, salted HMAC hashing for
 * UIN/VID/handle storage lookups, the {@code id_attributes} cache, and resolving the
 * current audit principal. Registered as a {@code @Primary} Spring bean from identity
 * security configuration (not annotated {@code @Component} on this class).
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ol>
 *   <li><b>Cryptomanager integration</b> — {@link #encrypt(byte[], String)},
 *       {@link #decrypt(byte[], String)}, and salt-enveloped variants call
 *       kernel-cryptomanager over HTTP via {@link RestHelper}. {@code refId} selects the
 *       key category (UIN, demographic, bio documents, etc.) using keys such as
 *       {@link IdRepoConstants#UIN_REFID}.</li>
 *   <li><b>Salting and hashing</b> — {@link #hash(byte[])},
 *       {@link #hashwithSalt(byte[], byte[])}, and
 *       {@link #getIdHashAndAttributes(String, IntFunction)} produce digests for DB
 *       lookups. Bucket selection uses {@link SaltUtil} with salts from
 *       {@link UinHashSaltRepo} / {@link UinEncryptSaltRepo} (PU1 {@code mosip_idrepo}
 *       only — do not mix with idmap VID salts).</li>
 *   <li><b>ID attribute cache</b> — {@link #getIdHashAndAttributes(String, IntFunction)}
 *       is {@code @Cacheable("id_attributes")}.
 *       {@link #evictIdAttributeCacheAtInterval()} clears the region on a schedule so
 *       pods pick up new salts after the salt-generator Job.</li>
 *   <li><b>Audit identity</b> — static {@link #getUser()} reads the Spring Security
 *       principal for structured logging and audit across modules.</li>
 * </ol>
 *
 * <h2>Salt routing variants</h2>
 * <ul>
 *   <li>{@link #getSaltKeyForId(String)} — bucket from plain ID (identity/VID
 *       interceptors)</li>
 *   <li>{@link #getSaltKeyForHashOfId(String)} — bucket from HMAC(id) (credential
 *       pipeline where modulo must not depend on the raw identifier)</li>
 * </ul>
 *
 * <h2>Result map keys</h2>
 * <p>
 * {@link #getIdHashAndAttributes(String, IntFunction)} and related methods return a map
 * with {@link #ID_HASH}, {@link #SALT}, and {@link #MODULO}. Optional
 * {@link #ID_TYPE} is used by credential issuance callers.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * // salted hash for DB lookup
 * Map&lt;String, String&gt; attrs = securityManager.getIdHashAndAttributes(
 *     uin, uinHashSaltRepo::retrieveSaltById);
 * String idHash = attrs.get(IdRepoSecurityManager.ID_HASH);
 *
 * // encrypt demographic blob
 * byte[] cipher = securityManager.encryptWithSalt(plain, saltBytes, uinRefId);
 *
 * // audit / log principal
 * mosipLogger.info(IdRepoSecurityManager.getUser(), ...);
 * </pre>
 *
 * <h2>Bean wiring</h2>
 * <p>
 * Constructed by {@code IdentitySecurityConfig} ({@code @Primary} with auth-capable
 * {@link RestHelper}) and historically by credential-store bean config. Only one instance
 * should be active in the consolidated deployable.
 * </p>
 *
 * <h2>IDA note</h2>
 * <p>
 * IDA does not call this class and does not use id-repo salt tables. Compatibility is via
 * hashed identifiers and event payloads produced by callers (WebSub {@code id_hash},
 * partner token IDs, etc.).
 * </p>
 *
 * @author Manoj SP
 * @see RestHelper
 * @see RestRequestBuilder
 * @see RestServicesConstants#CRYPTO_MANAGER_ENCRYPT
 * @see RestServicesConstants#CRYPTO_MANAGER_DECRYPT
 * @see SaltUtil
 * @see UinHashSaltRepo
 * @see UinEncryptSaltRepo
 * @see EnvUtil
 */
@NoArgsConstructor
public class IdRepoSecurityManager {
	
	/** JSON response wrapper field holding the cryptomanager payload object. */
	private static final String RESPONSE = "response";

	/** JSON request field: prepend partner cert thumbprint to ciphertext. */
	private static final String PREPEND_THUMBPRINT = "prependThumbprint";

	/** JSON request field: cryptomanager reference id (key category). */
	private static final String REFERENCE_ID = "referenceId";

	/** JSON request/response field: Base64-encoded payload. */
	private static final String DATA = "data";

	/** JSON request field: request timestamp string. */
	private static final String TIME_STAMP = "timeStamp";

	/** JSON request field: MOSIP application id for cryptomanager. */
	private static final String APPLICATIONID = "applicationId";

	/** Placeholder request id sent on cryptomanager wrapper requests. */
	private static final String STRING = "string";

	/**
	 * Map key for the hash salt value in {@link #getIdHashAndAttributes} results.
	 * <p>
	 * Value is the salt string loaded from {@code uin_hash_salt}.
	 * </p>
	 */
	public static final String SALT = "SALT";

	/**
	 * Map key for the salt modulo / bucket index in {@link #getIdHashAndAttributes} results.
	 * <p>
	 * Matches the row index used in {@code uin_hash_salt} / {@code uin_encrypt_salt}.
	 * </p>
	 */
	public static final String MODULO = "MODULO";

	/**
	 * Map key for the salted ID hash in {@link #getIdHashAndAttributes} results.
	 * <p>
	 * Stored in {@code uin.uin_hash}, {@code credential_request_status.individual_id_hash},
	 * and similar columns.
	 * </p>
	 */
	public static final String ID_HASH = "id_hash";

	/**
	 * Map key for the ID type in hash attribute maps used by credential issuance
	 * (e.g. {@code UIN}, {@code VID}).
	 */
	public static final String ID_TYPE = "id_type";

	/** Structured logger for security manager operations. */
	private Logger mosipLogger = IdRepoLogger.getLogger(IdRepoSecurityManager.class);

	/** Log method name for cryptomanager encrypt/decrypt calls. */
	private static final String ENCRYPT_DECRYPT_DATA = "encryptDecryptData";

	/** Log class name identifier for structured logging. */
	private static final String ID_REPO_SECURITY_MANAGER = "IdRepoSecurityManager";

	/** REST request builder for cryptomanager service calls. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/**
	 * REST client for synchronous cryptomanager calls.
	 * <p>
	 * Set via {@link #IdRepoSecurityManager(RestHelper)} or resolved from the application
	 * context in {@link #init()} when the no-arg constructor is used.
	 * </p>
	 */
	private RestHelper restHelper;

	/** JSON mapper for building cryptomanager {@link RequestWrapper} bodies. */
	@Autowired
	private ObjectMapper mapper;

	/** Application context used to resolve {@link RestHelper} in {@link #init()}. */
	@Autowired
	private ApplicationContext ctx;

	/**
	 * Cache manager backing the {@code id_attributes} region cleared by
	 * {@link #evictIdAttributeCacheAtInterval()}.
	 */
	@Autowired
	private CacheManager cacheManager;

	/**
	 * Creates a security manager with an explicit {@link RestHelper} for cryptomanager
	 * calls.
	 * <p>
	 * Preferred when the bean factory supplies an auth-capable (self-token) RestHelper
	 * distinct from the default bean.
	 * </p>
	 *
	 * @param restHelper REST client for kernel-cryptomanager communication; must not be
	 *                   {@code null} for encrypt/decrypt to work without {@link #init()}
	 */
	public IdRepoSecurityManager(RestHelper restHelper) {
		this.restHelper = restHelper;
	}

	/**
	 * Resolves the {@link RestHelper} bean from the application context when not
	 * constructor-injected.
	 * <p>
	 * Invoked by Spring via {@link PostConstruct}. No-op when
	 * {@link #IdRepoSecurityManager(RestHelper)} already set {@link #restHelper}.
	 * </p>
	 */
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper))
			this.restHelper = ctx.getBean(RestHelper.class);
	}

	/**
	 * Computes an HMAC-SHA256 digest of the given data as a plain-text hex string.
	 * <p>
	 * Used as the first step in {@link #getSaltKeyForHashOfId(String)} before modulo
	 * bucket selection. Does <strong>not</strong> apply a row salt — use
	 * {@link #hashwithSalt(byte[], byte[])} for storage hashes.
	 * </p>
	 *
	 * @param data raw bytes to hash; must not be {@code null}
	 * @return hex-encoded HMAC digest
	 * @throws IdRepoAppUncheckedException with {@link IdRepoErrorConstants#UNKNOWN_ERROR}
	 *                                     if the HMAC algorithm is unavailable
	 */
	public String hash(final byte[] data) {
		try {
			return HMACUtils2.digestAsPlainText(data);
		} catch (NoSuchAlgorithmException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Computes an HMAC-SHA256 digest of data combined with the provided salt.
	 * <p>
	 * Produces the {@link #ID_HASH} value stored in the database. Salt bytes come from
	 * {@code uin_hash_salt} via the retrieval function passed to
	 * {@link #getIdHashAndAttributes(String, IntFunction)}.
	 * </p>
	 *
	 * @param data raw identity bytes (e.g. UIN or VID UTF-8 bytes)
	 * @param salt salt bytes retrieved from {@code uin_hash_salt}
	 * @return hex-encoded salted HMAC digest
	 * @throws IdRepoAppUncheckedException with {@link IdRepoErrorConstants#UNKNOWN_ERROR}
	 *                                     if the HMAC algorithm is unavailable
	 */
	public String hashwithSalt(final byte[] data, final byte[] salt) {
		try {
			return HMACUtils2.digestAsPlainTextWithSalt(data, salt);
		} catch (NoSuchAlgorithmException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Returns the authenticated username from the Spring Security context.
	 * <p>
	 * Used as the user identifier in structured logging and audit across all ID Repository
	 * modules. Returns an empty string when no authenticated {@link UserDetails} principal
	 * is present (anonymous / unauthenticated / non-UserDetails principal).
	 * </p>
	 *
	 * @return authenticated username, or empty string if anonymous/unauthenticated
	 */
	public static String getUser() {
		if (Objects.nonNull(SecurityContextHolder.getContext())
				&& Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())
				&& Objects.nonNull(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				&& SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {	
			return ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
					.getUsername();
		} else {
			return "";
		}
	}

	/**
	 * Encrypts data via kernel-cryptomanager REST API (no per-row salt in the request).
	 * <p>
	 * Builds a MOSIP {@link RequestWrapper} with application id, timestamp, reference id,
	 * URL-safe Base64 plaintext, and thumbprint-prepend flag from
	 * {@link EnvUtil#getPrependThumbprintStatus()}. Posts to
	 * {@link RestServicesConstants#CRYPTO_MANAGER_ENCRYPT}.
	 * </p>
	 *
	 * @param dataToEncrypt plaintext bytes to encrypt (UIN JSON, document bytes, etc.)
	 * @param refId         cryptomanager reference ID (e.g. {@link IdRepoConstants#UIN_REFID})
	 * @return encrypted ciphertext bytes (Base64 text from response, as byte array)
	 * @throws IdRepoAppException wrapping {@link IdRepoErrorConstants#ENCRYPTION_DECRYPTION_FAILED}
	 *                            on REST or response parsing failure
	 * @see #encryptWithSalt(byte[], byte[], String)
	 */
	public byte[] encrypt(final byte[] dataToEncrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils2.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(TIME_STAMP, DateUtils2.formatDate(new Date(), EnvUtil.getDateTimePattern()));
			request.put(DATA, CryptoUtil.encodeToURLSafeBase64(dataToEncrypt));
			request.put(REFERENCE_ID, refId);
			request.put(PREPEND_THUMBPRINT, EnvUtil.getPrependThumbprintStatus());
			baseRequest.setRequest(request);
			return encryptDecryptData(restBuilder.buildRequest(RestServicesConstants.CRYPTO_MANAGER_ENCRYPT,
					baseRequest, ObjectNode.class));
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Encrypts data with an explicit per-row salt via kernel-cryptomanager REST API.
	 * <p>
	 * Same as {@link #encrypt(byte[], String)} but includes a {@code salt} field in the
	 * request body. Used when UIN demographic or biometric blobs are encrypted with
	 * row-specific salts from {@code uin_encrypt_salt}.
	 * </p>
	 *
	 * @param dataToEncrypt plaintext bytes to encrypt
	 * @param saltToEncrypt salt bytes for envelope encryption
	 * @param refId         cryptomanager reference ID
	 * @return encrypted ciphertext bytes
	 * @throws IdRepoAppException wrapping {@link IdRepoErrorConstants#ENCRYPTION_DECRYPTION_FAILED}
	 * @see #decryptWithSalt(byte[], byte[], String)
	 */
	public byte[] encryptWithSalt(final byte[] dataToEncrypt, final byte[] saltToEncrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils2.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(TIME_STAMP, DateUtils2.formatDate(new Date(), EnvUtil.getDateTimePattern()));
			request.put(DATA, CryptoUtil.encodeToURLSafeBase64(dataToEncrypt));
			request.put("salt", CryptoUtil.encodeToURLSafeBase64(saltToEncrypt));
			request.put(REFERENCE_ID, refId);
			request.put(PREPEND_THUMBPRINT, EnvUtil.getPrependThumbprintStatus());
			baseRequest.setRequest(request);
			return encryptDecryptData(restBuilder.buildRequest(RestServicesConstants.CRYPTO_MANAGER_ENCRYPT,
					baseRequest, ObjectNode.class));
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Decrypts data via kernel-cryptomanager REST API (no per-row salt in the request).
	 * <p>
	 * Expects {@code dataToDecrypt} as URL-safe Base64 ciphertext (as stored in the DB).
	 * Posts to {@link RestServicesConstants#CRYPTO_MANAGER_DECRYPT} and decodes the
	 * {@code response.data} field from the wrapper response.
	 * </p>
	 *
	 * @param dataToDecrypt URL-safe Base64-encoded ciphertext bytes
	 * @param refId         cryptomanager reference ID matching the encryption key
	 * @return decrypted plaintext bytes
	 * @throws IdRepoAppException wrapping {@link IdRepoErrorConstants#ENCRYPTION_DECRYPTION_FAILED}
	 * @see #encrypt(byte[], String)
	 */
	public byte[] decrypt(final byte[] dataToDecrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils2.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(REFERENCE_ID, refId);
			request.put(TIME_STAMP, DateUtils2.formatDate(new Date(), EnvUtil.getDateTimePattern()));
			request.put(DATA, new String(dataToDecrypt));
			request.put(PREPEND_THUMBPRINT, EnvUtil.getPrependThumbprintStatus());
			baseRequest.setRequest(request);
			return CryptoUtil.decodeURLSafeBase64(new String(encryptDecryptData(restBuilder
					.buildRequest(RestServicesConstants.CRYPTO_MANAGER_DECRYPT, baseRequest, ObjectNode.class))));
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Decrypts salt-enveloped data via kernel-cryptomanager REST API.
	 * <p>
	 * Includes the same {@code salt} value used during
	 * {@link #encryptWithSalt(byte[], byte[], String)} so cryptomanager can derive the
	 * data encryption key.
	 * </p>
	 *
	 * @param dataToDecrypt URL-safe Base64-encoded ciphertext bytes
	 * @param saltToDecrypt salt bytes used during encryption
	 * @param refId         cryptomanager reference ID
	 * @return decrypted plaintext bytes
	 * @throws IdRepoAppException wrapping {@link IdRepoErrorConstants#ENCRYPTION_DECRYPTION_FAILED}
	 * @see #encryptWithSalt(byte[], byte[], String)
	 */
	public byte[] decryptWithSalt(final byte[] dataToDecrypt, final byte[] saltToDecrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils2.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(REFERENCE_ID, refId);
			request.put(TIME_STAMP, DateUtils2.formatDate(new Date(), EnvUtil.getDateTimePattern()));
			request.put(DATA, CryptoUtil.encodeToURLSafeBase64(dataToDecrypt));
			request.put("salt", CryptoUtil.encodeToURLSafeBase64(saltToDecrypt));
			request.put(PREPEND_THUMBPRINT, EnvUtil.getPrependThumbprintStatus());
			baseRequest.setRequest(request);
			return CryptoUtil.decodeURLSafeBase64(new String(encryptDecryptData(restBuilder
					.buildRequest(RestServicesConstants.CRYPTO_MANAGER_DECRYPT, baseRequest, ObjectNode.class))));
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Executes a cryptomanager REST call and extracts the encrypted/decrypted payload.
	 * <p>
	 * Expects a MOSIP wrapper response with {@code response.data} as a Base64 string.
	 * Logs and wraps {@link RestServiceException} and missing-data responses as
	 * {@link IdRepoErrorConstants#ENCRYPTION_DECRYPTION_FAILED}.
	 * </p>
	 *
	 * @param restRequest pre-built request for encrypt or decrypt endpoint
	 * @return raw {@code data} field bytes from the cryptomanager response body
	 * @throws IdRepoAppException if the response is empty, malformed, or the REST call fails
	 */
	private byte[] encryptDecryptData(final RestRequestDTO restRequest) throws IdRepoAppException {
		try {
			ObjectNode response = restHelper.requestSync(restRequest);

			if (response.has(RESPONSE) && Objects.nonNull(response.get(RESPONSE))
					&& response.get(RESPONSE).has(DATA) && Objects.nonNull(response.get(RESPONSE).get(DATA))) {
				return response.get(RESPONSE).get(DATA).asText().getBytes();
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
						"No data block found in response");
				throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED);
			}
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED);
		}
	}
	
	/**
	 * Returns the salted ID hash for the given plain-text ID using modulo-based salt
	 * lookup from the plain ID.
	 * <p>
	 * Convenience wrapper over {@link #getIdHashAndAttributes(String, IntFunction)}
	 * returning only {@link #ID_HASH}.
	 * </p>
	 *
	 * @param uin                   plain UIN, VID, or handle value
	 * @param saltRetreivalFunction function to retrieve hash salt by salt key index
	 *                              (typically {@code uinHashSaltRepo::retrieveSaltById})
	 * @return salted HMAC hash string for DB lookup and storage
	 * @see #getIdHashAndAttributes(String, IntFunction)
	 */
	public String getIdHash(String uin, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributes(uin, saltRetreivalFunction).get(ID_HASH);
	}
	
	/**
	 * Returns the salted ID hash using salt modulo derived from the plain ID hash (not
	 * the plain ID).
	 * <p>
	 * Convenience wrapper over
	 * {@link #getIdHashAndAttributesWithSaltModuloByPlainIdHash(String, IntFunction)}.
	 * Used by credential issuance where salt routing is based on the pre-computed hash
	 * modulo.
	 * </p>
	 *
	 * @param uin                   plain ID value
	 * @param saltRetreivalFunction function to retrieve hash salt by salt key index
	 * @return salted HMAC hash string
	 * @see #getIdHashAndAttributesWithSaltModuloByPlainIdHash(String, IntFunction)
	 */
	public String getIdHashWithSaltModuloByPlainIdHash(String uin, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributesWithSaltModuloByPlainIdHash(uin, saltRetreivalFunction).get(ID_HASH);
	}
	
	/**
	 * Computes and caches salted hash attributes for an ID (hash, salt, modulo).
	 * <p>
	 * Salt key is derived from the plain ID via {@link #getSaltKeyForId(String)}. The
	 * result is cached in the {@code id_attributes} region keyed by method arguments;
	 * evicted periodically by {@link #evictIdAttributeCacheAtInterval()}.
	 * </p>
	 * <p>
	 * Typical callers: identity entity interceptor, identity service create/update paths.
	 * </p>
	 *
	 * @param id                    plain ID value (UIN, VID, or handle)
	 * @param saltRetreivalFunction function to retrieve hash salt by salt key index
	 *                              (usually {@code uinHashSaltRepo::retrieveSaltById})
	 * @return map with {@link #ID_HASH}, {@link #SALT}, and {@link #MODULO} entries
	 * @see #getIdHashAndAttributes(String, IntFunction, ToIntFunction)
	 */
	@Cacheable(cacheNames = "id_attributes")
	public Map<String, String> getIdHashAndAttributes(String id, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributes(id, saltRetreivalFunction, this::getSaltKeyForId);
	}

	/**
	 * Computes salted hash attributes using salt modulo derived from the plain ID hash.
	 * <p>
	 * Uses {@link #getSaltKeyForHashOfId(String)} instead of {@link #getSaltKeyForId(String)}
	 * so the salt bucket depends on {@code HMAC(id)} modulo, not the raw identifier.
	 * <strong>Not</strong> {@code @Cacheable} — credential pipeline callers invoke per
	 * request.
	 * </p>
	 *
	 * @param id                    plain ID value
	 * @param saltRetreivalFunction function to retrieve hash salt by salt key index
	 * @return map with {@link #ID_HASH}, {@link #SALT}, and {@link #MODULO} entries
	 */
	public Map<String, String> getIdHashAndAttributesWithSaltModuloByPlainIdHash(String id, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributes(id, saltRetreivalFunction, this::getSaltKeyForHashOfId);
	}
	
	/**
	 * Computes salted hash attributes with a custom salt-key derivation function.
	 * <p>
	 * Core implementation shared by {@link #getIdHashAndAttributes(String, IntFunction)}
	 * and {@link #getIdHashAndAttributesWithSaltModuloByPlainIdHash(String, IntFunction)}.
	 * Steps: derive salt index → load salt → {@link #hashwithSalt(byte[], byte[])} →
	 * populate result map.
	 * </p>
	 *
	 * @param id                    plain ID value
	 * @param saltRetreivalFunction function to retrieve hash salt by salt key index
	 * @param saltIdFunction        function to derive the salt key index from the ID
	 *                              ({@link #getSaltKeyForId} or {@link #getSaltKeyForHashOfId})
	 * @return map with {@link #ID_HASH}, {@link #SALT}, and {@link #MODULO} entries
	 */
	public Map<String, String> getIdHashAndAttributes(String id, IntFunction<String> saltRetreivalFunction, ToIntFunction<String> saltIdFunction) {
		Map<String, String> hashWithAttributes = new HashMap<>();
		int saltId = saltIdFunction.applyAsInt(id);
		String hashSalt = saltRetreivalFunction.apply(saltId);
		String hash = hashwithSalt(id.getBytes(), hashSalt.getBytes());
		hashWithAttributes.put(ID_HASH, hash);
		hashWithAttributes.put(MODULO, String.valueOf(saltId));
		hashWithAttributes.put(SALT, hashSalt);
		return hashWithAttributes;
	}

	/**
	 * Derives the hash salt key index from a plain ID using configured salt key length.
	 * <p>
	 * Delegates to {@link SaltUtil#getIdvidModulo(String, int)} with
	 * {@link EnvUtil#getIdrepoSaltKeyLength()}. Used for identity and VID entity
	 * interceptors when persisting {@code uin_hash}.
	 * </p>
	 *
	 * @param id plain UIN, VID, or handle
	 * @return salt table index (modulo bucket) in {@code uin_hash_salt}
	 * @see #getSaltKeyForHashOfId(String)
	 */
	public int getSaltKeyForId(String id) {
		Integer saltKeyLength = EnvUtil.getIdrepoSaltKeyLength();
		return SaltUtil.getIdvidModulo(id, saltKeyLength);
	}
	
	/**
	 * Derives the hash salt key index from a plain ID hash (two-step modulo).
	 * <p>
	 * First hashes the ID with {@link #hash(byte[])}, then applies modulo using
	 * {@link SaltUtil#getIdvidHashModulo(String, int)}. Used when salt routing must be
	 * based on the hash of the ID rather than the ID itself (credential issuance and
	 * partner token flows).
	 * </p>
	 *
	 * @param id plain ID value
	 * @return salt table index derived from ID hash modulo
	 * @see #getSaltKeyForId(String)
	 */
	public int getSaltKeyForHashOfId(String id) {
		Integer saltKeyLength = EnvUtil.getIdrepoSaltKeyLength();
		return SaltUtil.getIdvidHashModulo(id, saltKeyLength);
	}
	
	/**
	 * Periodically evicts the {@code id_attributes} Spring cache region.
	 * <p>
	 * Scheduled on both initial delay and fixed delay using
	 * {@link IdRepoConstants#IDREPO_CACHE_UPDATE_INTERVAL}
	 * ({@code mosip.idrepo.cache.update.interval}), default
	 * {@link IdRepoConstants#CACHE_UPDATE_DEFAULT_INTERVAL} ms. Ensures identity pods pick
	 * up new hash salts after the salt-generator K8s Job without restart.
	 * </p>
	 * <p>
	 * Partner OLV policy cache uses a separate scheduler in
	 * {@code PartnerCacheUpdateSchedulerConfig}. In the consolidated service, only one
	 * instance of this eviction should run.
	 * </p>
	 *
	 * @see IdRepoConstants#IDREPO_CACHE_UPDATE_INTERVAL
	 * @see #getIdHashAndAttributes(String, IntFunction)
	 */
	@Scheduled(initialDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}", fixedDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}")
	public void evictIdAttributeCacheAtInterval() {
		Cache idAttrCache = cacheManager.getCache("id_attributes");
		if (Objects.nonNull(idAttrCache))
			idAttrCache.clear();
	}
}
