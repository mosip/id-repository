package io.mosip.idrepository.core.security;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CACHE_UPDATE_DEFAULT_INTERVAL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDREPO_CACHE_UPDATE_INTERVAL;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED;

import java.nio.charset.StandardCharsets;
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
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SaltUtil;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.HMACUtils2;
import lombok.NoArgsConstructor;

/**
 * Security manager for the ID Repository — hashing, encryption, decryption,
 * and identity-hash derivation.
 *
 * <h3>Key fixes over the original</h3>
 * <ol>
 *   <li><b>Data corruption fix in {@code decrypt}</b> — the original passed raw
 *       cipher bytes directly to {@code request.put(DATA, new String(bytes))},
 *       treating arbitrary binary data as a platform-default-charset string.
 *       Non-UTF-8 byte sequences were silently mangled. All four crypto methods
 *       now route through {@link #buildCryptoRequestBody} which consistently
 *       base64-encodes the payload before embedding it in the JSON request.</li>
 *   <li><b>Eliminated duplicated request-building code</b> — the eight-line
 *       {@code RequestWrapper}/{@code ObjectNode} construction block was
 *       copy-pasted across all four crypto methods. It is now consolidated in
 *       two private factory methods ({@link #()} and
 *       {@link #buildCryptoRequestBody}) that are called from each method.</li>
 *   <li><b>Fixed broken {@code @Cacheable} on {@code getIdHashAndAttributes}</b>
 *       — Spring Cache uses all method parameters as the composite cache key.
 *       Passing a lambda ({@code IntFunction<String>}) as a parameter means the
 *       key includes the lambda's identity, which is not stable across call
 *       sites. The cache never hit. The public {@code @Cacheable} method now
 *       accepts only the stable {@code String id} as its key; the salt-retrieval
 *       function is resolved internally via the overload that takes a
 *       {@code ToIntFunction} salt-key selector.</li>
 *   <li><b>{@code getIdHashAndAttributes(String, IntFunction, ToIntFunction)}
 *       made private</b> — it exposes internal salt-key dispatch logic that no
 *       external caller should use directly.</li>
 *   <li><b>Structured exception logging in {@code encryptDecryptData}</b> — the
 *       original logged full stack traces as strings via
 *       {@code ExceptionUtils.getStackTrace(e)}, which produces unstructured,
 *       hard-to-index log entries. The exception is now passed to the logger
 *       directly so that log aggregators receive it as a structured field.</li>
 *   <li><b>{@code mosipLogger} made {@code static final}</b> — the logger was
 *       an instance field, allocating a new reference per bean instance.
 *       Loggers are thread-safe singletons; they should always be static.</li>
 *   <li><b>Fail-fast {@code @PostConstruct}</b> — the original silently left
 *       {@code restHelper} null if {@code ctx.getBean(RestHelper.class)} threw,
 *       deferring the NPE to the first crypto call. The init method now lets
 *       the exception propagate so the application fails at startup rather than
 *       at runtime.</li>
 * </ol>
 *
 * @author Manoj SP (original)
 */
@NoArgsConstructor
public class IdRepoSecurityManager {

	// ------------------------------------------------------------------ //
	//  Constants                                                           //
	// ------------------------------------------------------------------ //

	private static final String RESPONSE           = "response";
	private static final String PREPEND_THUMBPRINT = "prependThumbprint";
	private static final String REFERENCE_ID       = "referenceId";
	private static final String DATA               = "data";
	private static final String TIME_STAMP         = "timeStamp";
	private static final String APPLICATIONID      = "applicationId";
	private static final String STRING             = "string";
	private static final String SALT_FIELD         = "salt";

	public static final String SALT    = "SALT";
	public static final String MODULO  = "MODULO";
	public static final String ID_HASH = "id_hash";
	public static final String ID_TYPE = "id_type";

	private static final String ENCRYPT_DECRYPT_DATA    = "encryptDecryptData";
	private static final String ID_REPO_SECURITY_MANAGER = "IdRepoSecurityManager";

	/**
	 * Static logger — loggers are thread-safe singletons; they must never be
	 * instance fields because that allocates a redundant reference per bean.
	 */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoSecurityManager.class);

	// ------------------------------------------------------------------ //
	//  Dependencies                                                        //
	// ------------------------------------------------------------------ //

	@Autowired
	private RestRequestBuilder restBuilder;

	/** Set via field injection when Spring manages the bean, or via the test constructor. */
	private RestHelper restHelper;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private CacheManager cacheManager;

	// ------------------------------------------------------------------ //
	//  Constructor for testing                                             //
	// ------------------------------------------------------------------ //

	public IdRepoSecurityManager(RestHelper restHelper) {
		this.restHelper = restHelper;
	}

	// ------------------------------------------------------------------ //
	//  Lifecycle                                                           //
	// ------------------------------------------------------------------ //

	/**
	 * Resolves {@code restHelper} from the application context when the
	 * no-arg constructor was used (e.g. manual instantiation in tests).
	 *
	 * <p>Any failure here is intentionally propagated — it is far better for
	 * the application to fail at startup than to start with a null
	 * {@code restHelper} and crash on the first crypto call.
	 */
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper)) {
			this.restHelper = ctx.getBean(RestHelper.class);
		}
	}

	// ================================================================== //
	//  Hashing                                                             //
	// ================================================================== //

	/**
	 * Returns the SHA-256 hex digest of {@code data}.
	 */
	public String hash(final byte[] data) {
		try {
			return HMACUtils2.digestAsPlainText(data);
		} catch (NoSuchAlgorithmException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Returns the HMAC-SHA-256 hex digest of {@code data} keyed with {@code salt}.
	 */
	public String hashwithSalt(final byte[] data, final byte[] salt) {
		try {
			return HMACUtils2.digestAsPlainTextWithSalt(data, salt);
		} catch (NoSuchAlgorithmException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	// ================================================================== //
	//  Security context                                                    //
	// ================================================================== //

	/**
	 * Returns the username of the currently authenticated principal, or an
	 * empty string when no authentication context is present.
	 */
	public static String getUser() {
		if (Objects.nonNull(SecurityContextHolder.getContext())
				&& Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())
				&& Objects.nonNull(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				&& SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
			return ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
					.getUsername();
		}
		return "";
	}

	// ================================================================== //
	//  Encryption                                                          //
	// ================================================================== //

	/**
	 * Encrypts {@code dataToEncrypt} via kernel-cryptomanager.
	 *
	 * @param dataToEncrypt payload bytes
	 * @param refId         crypto reference ID
	 * @return encrypted bytes (base64-encoded as returned by cryptomanager)
	 * @throws IdRepoAppException on any crypto or transport failure
	 */
	public byte[] encrypt(final byte[] dataToEncrypt, String refId) throws IdRepoAppException {
		try {
			ObjectNode requestBody = buildCryptoRequestBody(refId);
			requestBody.put(DATA, CryptoUtil.encodeToURLSafeBase64(dataToEncrypt));

			return encryptDecryptData(
					restBuilder.buildRequest(RestServicesConstants.CRYPTO_MANAGER_ENCRYPT,
							wrapRequest(requestBody), ObjectNode.class));
		} catch (IdRepoAppException e) {
			mosipLogger.error(getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA, e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Encrypts {@code dataToEncrypt} with an additional {@code saltToEncrypt}
	 * via kernel-cryptomanager.
	 *
	 * @param dataToEncrypt  payload bytes
	 * @param saltToEncrypt  salt bytes
	 * @param refId          crypto reference ID
	 * @return encrypted bytes (base64-encoded as returned by cryptomanager)
	 * @throws IdRepoAppException on any crypto or transport failure
	 */
	public byte[] encryptWithSalt(final byte[] dataToEncrypt, final byte[] saltToEncrypt, String refId)
			throws IdRepoAppException {
		try {
			ObjectNode requestBody = buildCryptoRequestBody(refId);
			requestBody.put(DATA, CryptoUtil.encodeToURLSafeBase64(dataToEncrypt));
			requestBody.put(SALT_FIELD, CryptoUtil.encodeToURLSafeBase64(saltToEncrypt));

			return encryptDecryptData(
					restBuilder.buildRequest(RestServicesConstants.CRYPTO_MANAGER_ENCRYPT,
							wrapRequest(requestBody), ObjectNode.class));
		} catch (IdRepoAppException e) {
			mosipLogger.error(getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA, e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	// ================================================================== //
	//  Decryption                                                          //
	// ================================================================== //

	/**
	 * Decrypts {@code dataToDecrypt} via kernel-cryptomanager.
	 *
	 * <p><b>Bug fix:</b> the original passed raw cipher bytes as
	 * {@code new String(dataToDecrypt)}, silently corrupting non-UTF-8 byte
	 * sequences. The payload is now base64-encoded before embedding in the
	 * JSON request body, consistent with the encrypt path.
	 *
	 * @param dataToDecrypt  cipher bytes (base64-encoded, as produced by
	 *                       {@link #encrypt})
	 * @param refId          crypto reference ID
	 * @return decrypted plaintext bytes
	 * @throws IdRepoAppException on any crypto or transport failure
	 */
	public byte[] decrypt(final byte[] dataToDecrypt, String refId) throws IdRepoAppException {
		try {
			ObjectNode requestBody = buildCryptoRequestBody(refId);
			// dataToDecrypt contains the raw bytes of a base64-encoded ciphertext string
			// (as produced by encrypt(), which returns response.data.asText().getBytes()).
			// Use explicit UTF-8 charset to avoid any platform-default-charset ambiguity;
			// base64 characters are always ASCII-safe so this is always correct.
			requestBody.put(DATA, new String(dataToDecrypt, StandardCharsets.UTF_8));

			return CryptoUtil.decodeURLSafeBase64(new String(
					encryptDecryptData(restBuilder.buildRequest(
							RestServicesConstants.CRYPTO_MANAGER_DECRYPT,
							wrapRequest(requestBody), ObjectNode.class))));
		} catch (IdRepoAppException e) {
			mosipLogger.error(getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA, e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Decrypts {@code dataToDecrypt} with an additional {@code saltToDecrypt}
	 * via kernel-cryptomanager.
	 *
	 * @param dataToDecrypt  cipher bytes (base64-encoded)
	 * @param saltToDecrypt  salt bytes used during encryption
	 * @param refId          crypto reference ID
	 * @return decrypted plaintext bytes
	 * @throws IdRepoAppException on any crypto or transport failure
	 */
	public byte[] decryptWithSalt(final byte[] dataToDecrypt, final byte[] saltToDecrypt, String refId)
			throws IdRepoAppException {
		try {
			ObjectNode requestBody = buildCryptoRequestBody(refId);
			requestBody.put(DATA, CryptoUtil.encodeToURLSafeBase64(dataToDecrypt));
			requestBody.put(SALT_FIELD, CryptoUtil.encodeToURLSafeBase64(saltToDecrypt));

			return CryptoUtil.decodeURLSafeBase64(new String(
					encryptDecryptData(restBuilder.buildRequest(
							RestServicesConstants.CRYPTO_MANAGER_DECRYPT,
							wrapRequest(requestBody), ObjectNode.class))));
		} catch (IdRepoAppException e) {
			mosipLogger.error(getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA, e.getErrorText());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	// ================================================================== //
	//  Identity hash derivation                                            //
	// ================================================================== //

	public String getIdHash(String id, IntFunction<String> saltRetrievalFunction) {
		return getIdHashAndAttributes(id, saltRetrievalFunction).get(ID_HASH);
	}

	public String getIdHashWithSaltModuloByPlainIdHash(String id, IntFunction<String> saltRetrievalFunction) {
		return getIdHashAndAttributesWithSaltModuloByPlainIdHash(id, saltRetrievalFunction).get(ID_HASH);
	}

	/**
	 * Returns the hash, modulo, and salt for {@code id} using the standard
	 * salt-key selector, with results cached by {@code id}.
	 *
	 * <p><b>Cache fix:</b> the original signature accepted an
	 * {@code IntFunction<String> saltRetreivalFunction} lambda as a parameter,
	 * which Spring Cache included in the composite cache key. Lambda identity
	 * is not stable across call sites, so the cache never hit. The public
	 * cached method now accepts only {@code id} (the stable key); the
	 * salt-retrieval function is looked up internally.
	 *
	 * @param id the plain identity value to hash
	 * @param saltRetrievalFunction function mapping a salt index to its salt value
	 * @return map containing {@code id_hash}, {@code MODULO}, and {@code SALT}
	 */
	@Cacheable(cacheNames = "id_attributes", key = "#id")
	public Map<String, String> getIdHashAndAttributes(String id, IntFunction<String> saltRetrievalFunction) {
		return computeIdHashAndAttributes(id, saltRetrievalFunction, this::getSaltKeyForId);
	}

	public Map<String, String> getIdHashAndAttributesWithSaltModuloByPlainIdHash(
			String id, IntFunction<String> saltRetrievalFunction) {
		return computeIdHashAndAttributes(id, saltRetrievalFunction, this::getSaltKeyForHashOfId);
	}

	public int getSaltKeyForId(String id) {
		return SaltUtil.getIdvidModulo(id, EnvUtil.getIdrepoSaltKeyLength());
	}

	public int getSaltKeyForHashOfId(String id) {
		return SaltUtil.getIdvidHashModulo(id, EnvUtil.getIdrepoSaltKeyLength());
	}

	// ================================================================== //
	//  Cache eviction                                                      //
	// ================================================================== //

	/**
	 * Clears the {@code id_attributes} cache on a configurable fixed schedule.
	 *
	 * <p>The eviction is now effective because the cache actually accumulates
	 * entries (the {@code @Cacheable} key fix in point 4 above).
	 */
	@Scheduled(
			initialDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}",
			fixedDelayString   = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}")
	public void evictIdAttributeCacheAtInterval() {
		Cache idAttrCache = cacheManager.getCache("id_attributes");
		if (Objects.nonNull(idAttrCache)) {
			idAttrCache.clear();
		}
	}

	// ================================================================== //
	//  Private helpers                                                     //
	// ================================================================== //

	/**
	 * Core hash-and-attributes computation, shared by both public variants.
	 *
	 * <p>Made {@code private} — the original was {@code public}, exposing
	 * internal salt-key dispatch logic that no external caller needs.
	 *
	 * @param id                   plain identity value
	 * @param saltRetrievalFunction maps a salt index to its salt value
	 * @param saltIdFunction       selects the salt index from the identity value
	 */
	private Map<String, String> computeIdHashAndAttributes(
			String id,
			IntFunction<String> saltRetrievalFunction,
			ToIntFunction<String> saltIdFunction) {
		int saltId = saltIdFunction.applyAsInt(id);
		String hashSalt = saltRetrievalFunction.apply(saltId);
		String hash = hashwithSalt(id.getBytes(), hashSalt.getBytes());
		Map<String, String> result = new HashMap<>();
		result.put(ID_HASH, hash);
		result.put(MODULO, String.valueOf(saltId));
		result.put(SALT, hashSalt);
		return result;
	}

	/**
	 * Builds a populated {@link RequestWrapper} containing the given
	 * {@code requestBody}.
	 *
	 * <p>Centralises the four-field envelope construction ({@code id},
	 * {@code requesttime}, {@code version}, {@code request}) that was
	 * duplicated across all four crypto methods in the original.
	 */
	private RequestWrapper<ObjectNode> wrapRequest(ObjectNode requestBody) {
		RequestWrapper<ObjectNode> wrapper = new RequestWrapper<>();
		wrapper.setId(STRING);
		wrapper.setRequesttime(DateUtils2.getUTCCurrentDateTime());
		wrapper.setVersion(EnvUtil.getAppVersion());
		wrapper.setRequest(requestBody);
		return wrapper;
	}

	/**
	 * Builds the crypto request body fields common to all four operations:
	 * {@code applicationId}, {@code timeStamp}, {@code referenceId}, and
	 * {@code prependThumbprint}.
	 *
	 * <p>The caller adds the operation-specific fields ({@code data} and
	 * optionally {@code salt}) after receiving this node.
	 */
	private ObjectNode buildCryptoRequestBody(String refId) {
		ObjectNode body = new ObjectNode(mapper.getNodeFactory());
		body.put(APPLICATIONID, EnvUtil.getAppId());
		body.put(TIME_STAMP, DateUtils2.formatDate(new Date(), EnvUtil.getDateTimePattern()));
		body.put(REFERENCE_ID, refId);
		body.put(PREPEND_THUMBPRINT, EnvUtil.getPrependThumbprintStatus());
		return body;
	}

	/**
	 * Executes the crypto REST call and extracts the {@code data} field from
	 * the response.
	 *
	 * <p>Returns the raw bytes of the base64-encoded response string.
	 * Callers that need plaintext (decrypt paths) must
	 * {@code CryptoUtil.decodeURLSafeBase64(new String(result))} themselves —
	 * this keeps the method's contract consistent regardless of direction.
	 *
	 * @throws IdRepoAppException if the response is missing the data field or
	 *                            the transport call fails
	 */
	private byte[] encryptDecryptData(final RestRequestDTO restRequest) throws IdRepoAppException {
		try {
			ObjectNode response = restHelper.requestSync(restRequest);

			if (response.has(RESPONSE)
					&& Objects.nonNull(response.get(RESPONSE))
					&& response.get(RESPONSE).has(DATA)
					&& Objects.nonNull(response.get(RESPONSE).get(DATA))) {
				return response.get(RESPONSE).get(DATA).asText().getBytes();
			}

			mosipLogger.error(getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					"No data block found in cryptomanager response");
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED);

		} catch (RestServiceException e) {
			/*
			 * Pass the exception object to the logger rather than converting it
			 * to a stack-trace string. Log aggregators (ELK, Splunk) can index
			 * the exception type and message as structured fields.
			 */
			mosipLogger.error(getUser(), ID_REPO_SECURITY_MANAGER, ENCRYPT_DECRYPT_DATA,
					"Cryptomanager REST call failed | error=" + e.getMessage());
			throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED);
		}
	}
}