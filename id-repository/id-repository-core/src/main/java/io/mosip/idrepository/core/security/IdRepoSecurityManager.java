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

import javax.annotation.PostConstruct;

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
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SaltUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import lombok.NoArgsConstructor;

/**
 * The Class IdRepoSecurityManager - provides security related functionalities
 * such as hashing, encryption and decryption using kernel-cryptomanager and
 * providing user details.
 *
 * @author Manoj SP
 */
@NoArgsConstructor
public class IdRepoSecurityManager {
	
	private static final String RESPONSE = "response";

	private static final String PREPEND_THUMBPRINT = "prependThumbprint";

	private static final String REFERENCE_ID = "referenceId";

	private static final String DATA = "data";

	private static final String TIME_STAMP = "timeStamp";

	private static final String APPLICATIONID = "applicationId";

	private static final String STRING = "string";

	public static final String SALT = "SALT";

	public static final String MODULO = "MODULO";

	public static final String ID_HASH = "id_hash";

	/** The mosip logger. */
	private Logger mosipLogger = IdRepoLogger.getLogger(IdRepoSecurityManager.class);

	/** The Constant ENCRYPT_DECRYPT_DATA. */
	private static final String ENCRYPT_DECRYPT_DATA = "encryptDecryptData";

	/** The Constant ID_REPO_SECURITY_MANAGER. */
	private static final String ID_REPO_SECURITY_MANAGER = "IdRepoSecurityManager";

	/** The rest factory. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/** The rest helper. */
	private RestHelper restHelper;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ApplicationContext ctx;
	
	@Autowired
	private CacheManager cacheManager;
	
	public IdRepoSecurityManager(RestHelper restHelper) {
		this.restHelper = restHelper;
	}
	
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper))
			this.restHelper = ctx.getBean(RestHelper.class);
	}

	/**
	 * Hash - provides basic hash.
	 *
	 * @param data the identity info
	 * @return the string
	 */
	public String hash(final byte[] data) {
		try {
			return HMACUtils2.digestAsPlainText(data);
		} catch (NoSuchAlgorithmException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Hash with salt - provides hash value based on provided salt.
	 *
	 * @param data the identity info
	 * @param salt the salt
	 * @return the string
	 */
	public String hashwithSalt(final byte[] data, final byte[] salt) {
		try {
			return HMACUtils2.digestAsPlainTextWithSalt(data, salt);
		} catch (NoSuchAlgorithmException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * provides the user id.
	 *
	 * @return the user
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
	 * Encryption of data by making rest call to kernel-cryptomanager.
	 *
	 * @param dataToEncrypt the data to encrypt
	 * @return the byte[]
	 * @throws IdRepoAppException the id repo app exception
	 */
	public byte[] encrypt(final byte[] dataToEncrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(TIME_STAMP, DateUtils.formatDate(new Date(), EnvUtil.getDateTimePattern()));
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
	 * Encryption of data by making rest call to kernel-cryptomanager with salt.
	 *
	 * @param dataToEncrypt the data to encrypt
	 * @param saltToEncrypt the salt to encrypt
	 * @return the byte[]
	 * @throws IdRepoAppException the id repo app exception
	 */
	public byte[] encryptWithSalt(final byte[] dataToEncrypt, final byte[] saltToEncrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(TIME_STAMP, DateUtils.formatDate(new Date(), EnvUtil.getDateTimePattern()));
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
	 * Decryption of data by making rest call to kernel-cryptomanager.
	 *
	 * @param dataToDecrypt the data to decrypt
	 * @return the byte[]
	 * @throws IdRepoAppException the id repo app exception
	 */
	public byte[] decrypt(final byte[] dataToDecrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(REFERENCE_ID, refId);
			request.put(TIME_STAMP, DateUtils.formatDate(new Date(), EnvUtil.getDateTimePattern()));
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
	 * Decryption of data by making rest call to kernel-cryptomanager with salt.
	 *
	 * @param dataToDecrypt the data to decrypt
	 * @param saltToDecrypt the salt to decrypt
	 * @return the byte[]
	 * @throws IdRepoAppException the id repo app exception
	 */
	public byte[] decryptWithSalt(final byte[] dataToDecrypt, final byte[] saltToDecrypt, String refId) throws IdRepoAppException {
		try {
			RequestWrapper<ObjectNode> baseRequest = new RequestWrapper<>();
			baseRequest.setId(STRING);
			baseRequest.setRequesttime(DateUtils.getUTCCurrentDateTime());
			baseRequest.setVersion(EnvUtil.getAppVersion());
			ObjectNode request = new ObjectNode(mapper.getNodeFactory());
			request.put(APPLICATIONID, EnvUtil.getAppId());
			request.put(REFERENCE_ID, refId);
			request.put(TIME_STAMP, DateUtils.formatDate(new Date(), EnvUtil.getDateTimePattern()));
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
	 * Rest calls is made to kernel-cryptomanager and required data from response is
	 * extracted and handled.
	 *
	 * @param restRequest the rest request
	 * @return the byte[]
	 * @throws IdRepoAppException the id repo app exception
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
	
	public String getIdHash(String uin, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributes(uin, saltRetreivalFunction).get(ID_HASH);
	}
	
	public String getIdHashWithSaltModuloByPlainIdHash(String uin, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributesWithSaltModuloByPlainIdHash(uin, saltRetreivalFunction).get(ID_HASH);
	}
	
	@Cacheable(cacheNames = IdRepoConstants.ID_ATTRIBUTES_CACHE)
	public Map<String, String> getIdHashAndAttributes(String id, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributes(id, saltRetreivalFunction, this::getSaltKeyForId);
	}

	public Map<String, String> getIdHashAndAttributesWithSaltModuloByPlainIdHash(String id, IntFunction<String> saltRetreivalFunction) {
		return getIdHashAndAttributes(id, saltRetreivalFunction, this::getSaltKeyForHashOfId);
	}
	
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

	public int getSaltKeyForId(String id) {
		Integer saltKeyLength = EnvUtil.getIdrepoSaltKeyLength();
		return SaltUtil.getIdvidModulo(id, saltKeyLength);
	}
	
	public int getSaltKeyForHashOfId(String id) {
		Integer saltKeyLength = EnvUtil.getIdrepoSaltKeyLength();
		return SaltUtil.getIdvidHashModulo(id, saltKeyLength);
	}
}