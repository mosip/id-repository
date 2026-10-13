package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.BIO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ADAPTER_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_BUCKET_NAME;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.mosip.kernel.core.logger.spi.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.mosip.commons.khazana.dto.ObjectStoreReference;
import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * Reads and writes encrypted identity files (biometric CBEFF and demographic
 * documents) in the Khazana object store.
 * <p>
 * Live files are stored under {@code {uinHash}/Biometrics/{fileRefId}} and
 * {@code {uinHash}/Demographics/{fileRefId}}. V2 drafts keep their files apart
 * from live data under {@code _draft/{ridHash}/Biometrics/{fileRefId}} and
 * {@code _draft/{ridHash}/Demographics/{fileRefId}}, where {@code ridHash} is
 * {@link #getRidHash(String)}. Publishing a V2 draft moves its files to the live
 * path; discarding deletes them.
 * <p>
 * Every object is encrypted with {@link IdRepoSecurityManager} using the
 * biometric or demographic reference id before it is written, and decrypted
 * after it is read. The adapter bean is selected by
 * {@code mosip.idrepo.objectstore.adapter-name}. Draft move, copy and list
 * operations need an adapter that implements
 * {@link ObjectStoreAdapter#moveObject} and
 * {@link ObjectStoreAdapter#listObjectsByPrefix} (the Khazana S3 adapter).
 */
@Component
public class ObjectStoreHelper {
	
	@Value("${" + BIO_DATA_REFID + "}")
	/**
	 * Key reference id used to encrypt and decrypt biometric files
	 * ({@code mosip.idrepo.crypto.refId.bio-doc-data}).
	 */
	private String bioDataRefId;
	
	@Value("${" + DEMO_DATA_REFID + "}")
	/**
	 * Key reference id used to encrypt and decrypt demographic documents
	 * ({@code mosip.idrepo.crypto.refId.demo-doc-data}).
	 */
	private String demoDataRefId;

	/** Separator between object key segments. */
	private static final String SLASH = "/";

	/** Key segment under which biometric (CBEFF) files are stored. */
	private static final String BIOMETRICS = "Biometrics";

	/** Key segment under which demographic documents are stored. */
	private static final String DEMOGRAPHICS = "Demographics";

	/** First path segment of every V2 draft object, which keeps drafts apart from live UIN paths. */
	private static final String DRAFT = "_draft";
	
	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	/**
	 * Object-store account passed on every adapter call
	 * ({@code mosip.idrepo.objectstore.account-name}). Used as the bucket when the
	 * Khazana S3 adapter runs with {@code object.store.s3.use.account.as.bucketname=true}.
	 */
	private String objectStoreAccountName;

	@Value("${" + OBJECT_STORE_BUCKET_NAME + "}")
	/**
	 * Object-store container passed on every adapter call
	 * ({@code mosip.idrepo.objectstore.bucket-name}). Used as the bucket by default, or as
	 * the first key segment in account-as-bucket mode.
	 */
	private String objectStoreBucketName;

	@Value("${" + OBJECT_STORE_ADAPTER_NAME + "}")
	/**
	 * Spring bean name of the Khazana adapter to use
	 * ({@code mosip.idrepo.objectstore.adapter-name}), for example {@code S3Adapter}.
	 */
	private String objectStoreAdapterName;
	
	@Value("${mosip.idrepo.objectstore.max-object-size-bytes:10485760}")
	/**
	 * Largest encrypted draft object, in bytes, that {@link #getDraftObject} will read
	 * ({@code mosip.idrepo.objectstore.max-object-size-bytes}, default 10 MiB).
	 * Larger objects are rejected so a corrupt or oversized file cannot exhaust heap.
	 */
	private long maxObjectSizeBytes = 10 * 1024 * 1024L;

	/** Khazana adapter bean named by {@link #objectStoreAdapterName}; set by {@link #setObjectStore}. */
	private ObjectStoreAdapter objectStore;

	/** Logger for object-store warnings and errors. */
	private Logger mosipLogger = IdRepoLogger.getLogger(ObjectStoreHelper.class);

	@Autowired
	/**
	 * Looks up the configured Khazana adapter by bean name, so the adapter can be
	 * switched by configuration without code changes.
	 *
	 * @param context application context holding the adapter beans
	 * @throws org.springframework.beans.BeansException when no adapter bean has the
	 *         configured name
	 */
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	/** Encrypts files before they are written and decrypts them after they are read. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/**
	 * Computes the hash of a registration id. The hash is the second path segment of
	 * every V2 draft object ({@code _draft/{ridHash}/...}), so the raw registration id
	 * never appears in object-store keys.
	 *
	 * @param registrationId registration id of the draft
	 * @return plain-text HMAC digest of {@code registrationId}
	 * @throws IdRepoAppException {@code UNKNOWN_ERROR} when the digest cannot be computed
	 */
	public String getRidHash(String registrationId) throws IdRepoAppException {
		try {
			return HMACUtils2.digestAsPlainText(registrationId.getBytes());
		} catch (Exception e) {
			throw new IdRepoAppException(UNKNOWN_ERROR.getErrorCode(),
					"Failed to compute rid hash for regId=" + registrationId, e);
		}
	}

	/**
	 * Reports whether a live demographic document exists.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the document
	 * @return {@code true} when {@code {uinHash}/Demographics/{fileRefId}} exists
	 */
	public boolean demographicObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, false, fileRefId);
	}

	/**
	 * Reports whether a live biometric file exists.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the biometric file
	 * @return {@code true} when {@code {uinHash}/Biometrics/{fileRefId}} exists
	 */
	public boolean biometricObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, true, fileRefId);
	}

	/**
	 * Encrypts a demographic document with {@link #demoDataRefId} and stores it under
	 * {@code {uinHash}/Demographics/{fileRefId}}, replacing any existing object.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the document
	 * @param data      plain document bytes
	 * @throws IdRepoAppException {@code FILE_STORAGE_ACCESS_ERROR} when the write fails
	 */
	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	/**
	 * Encrypts a biometric file with {@link #bioDataRefId} and stores it under
	 * {@code {uinHash}/Biometrics/{fileRefId}}, replacing any existing object.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the biometric file
	 * @param data      plain CBEFF bytes
	 * @throws IdRepoAppException {@code FILE_STORAGE_ACCESS_ERROR} when the write fails
	 */
	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	/**
	 * Reads and decrypts a live demographic document.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the document
	 * @return decrypted document bytes
	 * @throws IdRepoAppException {@code FILE_NOT_FOUND} when the object is absent;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} for any other read failure
	 */
	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException {
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	/**
	 * Reads and decrypts a live biometric file.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the biometric file
	 * @return decrypted CBEFF bytes
	 * @throws IdRepoAppException {@code FILE_NOT_FOUND} when the object is absent;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} for any other read failure
	 */
	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}
	
	/**
	 * Deletes a live biometric file when it exists. Used to remove previously extracted
	 * template files before extracting them again.
	 *
	 * @param uinHash   uin hash (without salt id) of the identity
	 * @param fileRefId file reference id of the biometric file
	 */
	public void deleteBiometricObject(String uinHash, String fileRefId) {
		if (this.biometricObjectExists(uinHash, fileRefId)) {
			String objectName = uinHash + SLASH + BIOMETRICS + SLASH + fileRefId;
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		}
	}

	/**
	 * Encrypts and stores a biometric file under {@code _draft/{pathPrefix}/Biometrics/{fileRefId}}.
	 *
	 * @param pathPrefix rid hash of the draft ({@link #getRidHash(String)})
	 * @param fileRefId  file reference id of the biometric file
	 * @param data       plain CBEFF bytes; must not be null or empty
	 * @throws IdRepoAppException {@code FILE_STORAGE_ACCESS_ERROR} when the data is empty
	 *         or the object store rejects the write
	 */
	public void putDraftBiometricObject(String pathPrefix, String fileRefId, byte[] data) throws IdRepoAppException {
		putDraftObject(pathPrefix, true, fileRefId, data, bioDataRefId);
	}

	/**
	 * Encrypts and stores a demographic document under {@code _draft/{pathPrefix}/Demographics/{fileRefId}}.
	 *
	 * @param pathPrefix rid hash of the draft ({@link #getRidHash(String)})
	 * @param fileRefId  file reference id of the document
	 * @param data       plain document bytes; must not be null or empty
	 * @throws IdRepoAppException {@code FILE_STORAGE_ACCESS_ERROR} when the data is empty
	 *         or the object store rejects the write
	 */
	public void putDraftDemographicObject(String pathPrefix, String fileRefId, byte[] data) throws IdRepoAppException {
		putDraftObject(pathPrefix, false, fileRefId, data, demoDataRefId);
	}

	/**
	 * Reports whether a draft biometric file exists.
	 *
	 * @param pathPrefix rid hash of the draft
	 * @param fileRefId  file reference id of the biometric file
	 * @return {@code true} when {@code _draft/{pathPrefix}/Biometrics/{fileRefId}} exists
	 */
	public boolean draftBiometricObjectExists(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, true, fileRefId);
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	/**
	 * Reads and decrypts a draft biometric file.
	 *
	 * @param ridHash   rid hash of the draft
	 * @param fileRefId file reference id of the biometric file
	 * @return decrypted CBEFF bytes
	 * @throws IdRepoAppException {@code FILE_NOT_FOUND} when the object is absent;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} when it cannot be read or exceeds
	 *         {@link #maxObjectSizeBytes}
	 */
	public byte[] getDraftBiometricObject(String ridHash, String fileRefId) throws IdRepoAppException {
		return getDraftObject(ridHash, true, fileRefId, bioDataRefId);
	}

	/**
	 * Reads and decrypts a draft demographic document.
	 *
	 * @param ridHash   rid hash of the draft
	 * @param fileRefId file reference id of the document
	 * @return decrypted document bytes
	 * @throws IdRepoAppException {@code FILE_NOT_FOUND} when the object is absent;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} when it cannot be read or exceeds
	 *         {@link #maxObjectSizeBytes}
	 */
	public byte[] getDraftDemographicObject(String ridHash, String fileRefId) throws IdRepoAppException {
		return getDraftObject(ridHash, false, fileRefId, demoDataRefId);
	}

	/**
	 * Deletes one draft biometric file. A failure is logged and not thrown, because a
	 * leftover draft file is harmless and is removed when the draft is discarded.
	 *
	 * @param pathPrefix rid hash of the draft
	 * @param fileRefId  file reference id of the biometric file
	 */
	public void deleteDraftBiometricObject(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, true, fileRefId);
		try {
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		} catch (ObjectStoreAdapterException e) {
			mosipLogger.warn("Failed to delete draft biometric object: {} | error={}", objectName, e.getMessage());
		}
	}

	/**
	 * Deletes one draft demographic document. A failure is logged and not thrown, because
	 * a leftover draft file is harmless and is removed when the draft is discarded.
	 *
	 * @param pathPrefix rid hash of the draft
	 * @param fileRefId  file reference id of the document
	 */
	public void deleteDraftDemographicObject(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, false, fileRefId);
		try {
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		} catch (ObjectStoreAdapterException e) {
			mosipLogger.warn("Failed to delete draft demographic object: {} | error={}", objectName, e.getMessage());
		}
	}

	/**
	 * Copies a live biometric file into the draft path so a V2 update can read and
	 * merge it (UPDATE, ACTIVATED and DEACTIVATED packets). The live file is kept.
	 * The copy is server-side, so the encrypted bytes are not decrypted.
	 *
	 * @param livePrefix uin hash (without salt id) of the live identity
	 * @param ridHash    rid hash of the draft
	 * @param fileRefId  file reference id of the biometric file
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_COPY_FAILED} when the copy fails or
	 *         the adapter does not support {@link ObjectStoreAdapter#moveObject}
	 */
	public void copyBiometricLiveToDraft(String livePrefix, String ridHash, String fileRefId)
			throws IdRepoAppException {
		copyRaw(buildObjectName(livePrefix, true, fileRefId), buildDraftObjectName(ridHash, true, fileRefId), false);
	}

	/**
	 * Copies a live demographic document into the draft path. The live file is kept.
	 *
	 * @param livePrefix uin hash (without salt id) of the live identity
	 * @param ridHash    rid hash of the draft
	 * @param fileRefId  file reference id of the document
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_COPY_FAILED} when the copy fails or
	 *         the adapter does not support {@link ObjectStoreAdapter#moveObject}
	 */
	public void copyDemographicLiveToDraft(String livePrefix, String ridHash, String fileRefId)
			throws IdRepoAppException {
		copyRaw(buildObjectName(livePrefix, false, fileRefId), buildDraftObjectName(ridHash, false, fileRefId), false);
	}

	/**
	 * Moves every object under {@code _draft/{ridHash}/Biometrics/} to
	 * {@code {uinHash}/Biometrics/}, keeping the file reference id. Keys come from
	 * {@link ObjectStoreAdapter#listObjectsByPrefix} because
	 * {@link ObjectStoreAdapter#getAllObjects} returns truncated names. Nested keys are skipped.
	 * <p>
	 * A per-file failure is thrown, never swallowed: the V2 publish deletes the draft
	 * database rows right after this call returns, so a skipped file would end up in
	 * neither the draft nor the live path. A source already moved by an earlier publish
	 * attempt is skipped (see {@link #moveRaw}).
	 *
	 * @param ridHash rid hash of the draft
	 * @param uinHash uin hash (without salt id) of the live identity
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_MOVE_FAILED} when listing or any move fails
	 */
	public void moveAllDraftBiometricsToLive(String ridHash, String uinHash) throws IdRepoAppException {
		String draftBioPrefix = DRAFT + SLASH + ridHash + SLASH + BIOMETRICS + SLASH;
		try {
			List<String> keys = objectStore.listObjectsByPrefix(objectStoreAccountName, objectStoreBucketName, draftBioPrefix);
			for (String srcKey : keys) {
				String fileRefId = srcKey.substring(draftBioPrefix.length());
				if (fileRefId.isEmpty() || fileRefId.contains(SLASH)) {
					continue;
				}
				String destKey = buildObjectName(uinHash, true, fileRefId);
				moveRaw(srcKey, destKey);
			}
		} catch (IdRepoAppException e) {
			throw e;
		} catch (Exception e) {
			throw new IdRepoAppException(DRAFT_OBJECT_MOVE_FAILED.getErrorCode(),
					String.format(DRAFT_OBJECT_MOVE_FAILED.getErrorMessage(), "ridHash=" + ridHash), e);
		}
	}

	/**
	 * Moves every object under {@code _draft/{ridHash}/Demographics/} to
	 * {@code {uinHash}/Demographics/}, keeping the file reference id. Nested keys are skipped.
	 * <p>
	 * A per-file failure is thrown, never swallowed, for the same reason as
	 * {@link #moveAllDraftBiometricsToLive}.
	 *
	 * @param ridHash rid hash of the draft
	 * @param uinHash uin hash (without salt id) of the live identity
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_MOVE_FAILED} when listing or any move fails
	 */
	public void moveAllDraftDemographicsToLive(String ridHash, String uinHash) throws IdRepoAppException {
		String draftDemoPrefix = DRAFT + SLASH + ridHash + SLASH + DEMOGRAPHICS + SLASH;
		try {
			List<String> keys = objectStore.listObjectsByPrefix(objectStoreAccountName, objectStoreBucketName, draftDemoPrefix);
			for (String srcKey : keys) {
				String fileRefId = srcKey.substring(draftDemoPrefix.length());
				if (fileRefId.isEmpty() || fileRefId.contains(SLASH)) {
					continue;
				}
				String destKey = buildObjectName(uinHash, false, fileRefId);
				moveRaw(srcKey, destKey);
			}
		} catch (IdRepoAppException e) {
			throw e;
		} catch (Exception e) {
			throw new IdRepoAppException(DRAFT_OBJECT_MOVE_FAILED.getErrorCode(),
					String.format(DRAFT_OBJECT_MOVE_FAILED.getErrorMessage(), "ridHash=" + ridHash), e);
		}
	}

	/**
	 * Deletes every object under {@code _draft/{ridHash}/Biometrics/}. Used when a V2
	 * draft is discarded and to sweep leftovers after publish. A missing key is not an
	 * error. Any other failure is thrown so the caller can keep the draft rows and retry.
	 *
	 * @param ridHash rid hash of the draft
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_DELETE_FAILED} when listing or a delete fails
	 */
	public void deleteAllDraftBiometrics(String ridHash) throws IdRepoAppException {
		String draftBioPrefix = DRAFT + SLASH + ridHash + SLASH + BIOMETRICS + SLASH;
		try {
			List<String> keys = objectStore.listObjectsByPrefix(objectStoreAccountName, objectStoreBucketName, draftBioPrefix);
			if (keys == null) {
				return;
			}
			for (String key : keys) {
				objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, key);
			}
		} catch (Exception e) {
			throw new IdRepoAppException(DRAFT_OBJECT_DELETE_FAILED.getErrorCode(),
					String.format(DRAFT_OBJECT_DELETE_FAILED.getErrorMessage(), "ridHash=" + ridHash), e);
		}
	}

	/**
	 * Deletes every object under {@code _draft/{ridHash}/Demographics/}. A missing key is
	 * not an error. Any other failure is thrown so the caller can keep the draft rows and retry.
	 *
	 * @param ridHash rid hash of the draft
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_DELETE_FAILED} when listing or a delete fails
	 */
	public void deleteAllDraftDemographics(String ridHash) throws IdRepoAppException {
		String draftDemoPrefix = DRAFT + SLASH + ridHash + SLASH + DEMOGRAPHICS + SLASH;
		try {
			List<String> keys = objectStore.listObjectsByPrefix(objectStoreAccountName, objectStoreBucketName, draftDemoPrefix);
			if (keys == null) {
				return;
			}
			for (String key : keys) {
				objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, key);
			}
		} catch (Exception e) {
			throw new IdRepoAppException(DRAFT_OBJECT_DELETE_FAILED.getErrorCode(),
					String.format(DRAFT_OBJECT_DELETE_FAILED.getErrorMessage(), "ridHash=" + ridHash), e);
		}
	}

	/**
	 * Reports whether a live object exists.
	 *
	 * @param uinHash   uin hash (without salt id)
	 * @param isBio     {@code true} for a biometric file, {@code false} for a document
	 * @param fileRefId file reference id
	 * @return {@code true} when {@code {uinHash}/Biometrics|Demographics/{fileRefId}} exists
	 */
	private boolean exists(String uinHash, boolean isBio, String fileRefId) {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	/**
	 * Encrypts {@code data} with {@code refId} and writes it to the live path. No
	 * exists-check is made first; an existing object is overwritten.
	 *
	 * @param uinHash   uin hash (without salt id)
	 * @param isBio     {@code true} for a biometric file, {@code false} for a document
	 * @param fileRefId file reference id
	 * @param data      plain bytes
	 * @param refId     encryption reference id (biometric or demographic)
	 * @throws IdRepoAppException an encryption error as thrown by the security manager;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} for any storage or unexpected failure
	 */
	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId)
			throws IdRepoAppException {
		try {
			String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
			InputStream encryptData = new ByteArrayInputStream(securityManager.encrypt(data, refId));
			objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName, encryptData);
		} catch (AmazonS3Exception | FSAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		} catch (IdRepoAppException e) {
			throw e;
		} catch (Throwable e) {
			mosipLogger.error("Exception in connection>>>", e);
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Reads a live object and decrypts it. No exists-check is made first: a missing object
	 * is detected from a {@code null} stream or a 404 / {@code NoSuchKey} error, which saves
	 * one object-store round trip per read.
	 *
	 * @param uinHash   uin hash (without salt id)
	 * @param isBio     {@code true} for a biometric file, {@code false} for a document
	 * @param fileRefId file reference id
	 * @param refId     decryption reference id (biometric or demographic)
	 * @return decrypted bytes
	 * @throws IdRepoAppException {@code FILE_NOT_FOUND} when the object is absent;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} for any other read failure
	 */
	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId) throws IdRepoAppException {
		try {
			String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
			InputStream stream = objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null,
					objectName);
			if (stream == null) {
				throw new IdRepoAppException(FILE_NOT_FOUND);
			}
			return securityManager.decrypt(IOUtils.toByteArray(stream), refId);
		} catch (IdRepoAppException e) {
			throw e;
		} catch (AmazonS3Exception | FSAdapterException | IOException e) {
			if (isMissingObject(e)) {
				throw new IdRepoAppException(FILE_NOT_FOUND, e);
			}
			throw new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Reports whether a storage failure means the object does not exist. An
	 * {@link AmazonS3Exception} is checked by HTTP status 404 or error code
	 * {@code NoSuchKey}; any other exception by its message, because other adapters do
	 * not expose a status code.
	 *
	 * @param e storage failure
	 * @return {@code true} when the failure is a missing-object error
	 */
	private static boolean isMissingObject(Exception e) {
		if (e instanceof AmazonS3Exception s3) {
			return s3.getStatusCode() == 404 || "NoSuchKey".equals(s3.getErrorCode());
		}
		String message = e.getMessage();
		return message != null && (message.contains("NoSuchKey") || message.contains("404"));
	}

	/**
	 * Builds a live object key.
	 *
	 * @param uinHash   uin hash (without salt id)
	 * @param isBio     {@code true} for {@code Biometrics}, {@code false} for {@code Demographics}
	 * @param fileRefId file reference id
	 * @return {@code {uinHash}/Biometrics|Demographics/{fileRefId}}
	 */
	private String buildObjectName(String uinHash, boolean isBio, String fileRefId) {
		return uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
	}

	/**
	 * Builds a V2 draft object key.
	 *
	 * @param ridHash   rid hash of the draft
	 * @param isBio     {@code true} for {@code Biometrics}, {@code false} for {@code Demographics}
	 * @param fileRefId file reference id
	 * @return {@code _draft/{ridHash}/Biometrics|Demographics/{fileRefId}}
	 */
	private String buildDraftObjectName(String ridHash, boolean isBio, String fileRefId) {
		return DRAFT + SLASH + ridHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
	}

	/**
	 * Encrypts {@code data} with {@code refId} and writes it to the draft path.
	 *
	 * @param pathPrefix rid hash of the draft
	 * @param isBio      {@code true} for a biometric file, {@code false} for a document
	 * @param fileRefId  file reference id
	 * @param data       plain bytes; must not be null or empty
	 * @param refId      encryption reference id (biometric or demographic)
	 * @throws IdRepoAppException {@code FILE_STORAGE_ACCESS_ERROR} when the data is empty
	 *         or the write fails
	 */
	private void putDraftObject(String pathPrefix, boolean isBio, String fileRefId, byte[] data, String refId)
			throws IdRepoAppException {
		if (data == null || data.length == 0) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "Input data is null or empty");
		}
		String objectName = buildDraftObjectName(pathPrefix, isBio, fileRefId);
		try (InputStream encryptData = new ByteArrayInputStream(securityManager.encrypt(data, refId))) {
			objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName, encryptData);
			mosipLogger.debug("Uploaded draft object: {} ({} bytes)", objectName, data.length);
		} catch (IOException | ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to store draft object: " + e.getMessage(), e);
		}
	}

	/**
	 * Reads a draft object through a bounded stream and decrypts it. At most
	 * {@link #maxObjectSizeBytes} + 1 bytes are read, so an oversized object is
	 * detected without loading all of it.
	 *
	 * @param pathPrefix rid hash of the draft
	 * @param isBio      {@code true} for a biometric file, {@code false} for a document
	 * @param fileRefId  file reference id
	 * @param refId      decryption reference id (biometric or demographic)
	 * @return decrypted bytes
	 * @throws IdRepoAppException {@code FILE_NOT_FOUND} when the object is absent;
	 *         {@code FILE_STORAGE_ACCESS_ERROR} when it cannot be read or is too large
	 */
	private byte[] getDraftObject(String pathPrefix, boolean isBio, String fileRefId, String refId)
			throws IdRepoAppException {
		String objectName = buildDraftObjectName(pathPrefix, isBio, fileRefId);
		InputStream rawStream;
		try {
			rawStream = objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		} catch (ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to fetch draft object: " + objectName, e);
		}
		if (rawStream == null) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		try (InputStream s3Stream = new BoundedInputStream(new BufferedInputStream(rawStream), maxObjectSizeBytes + 1)) {
			byte[] encryptedData = IOUtils.toByteArray(s3Stream);
			if (encryptedData.length > maxObjectSizeBytes) {
				throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
						"Draft object size exceeds allowed limit: " + objectName);
			}
			byte[] decryptedData = securityManager.decrypt(encryptedData, refId);
			encryptedData = null;
			return decryptedData;
		} catch (IOException | ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to retrieve draft object: " + e.getMessage(), e);
		}
	}

	/**
	 * Moves a draft object to the live path (server-side copy, then delete of the source).
	 * A source that is already gone is skipped: a retried publish can find files that an
	 * earlier attempt moved before a later step failed, and that must not block the retry.
	 *
	 * @param srcKey  full draft key
	 * @param destKey full live key
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_MOVE_FAILED} for any other object-store failure
	 */
	private void moveRaw(String srcKey, String destKey) throws IdRepoAppException {
		ObjectStoreReference src = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, srcKey);
		ObjectStoreReference dst = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, destKey);
		try {
			// S3Adapter.moveObject never returns false for a missing source key — it throws
			// ObjectStoreAdapterException instead, so the return value is not checked here.
			objectStore.moveObject(src, dst, true);
		} catch (ObjectStoreAdapterException e) {
			if (isMissingSourceKey(e)) {
				mosipLogger.debug("Draft object not found (already moved), skipping move: {} | error={}",
						srcKey, e.getMessage());
				return;
			}
			throw new IdRepoAppException(DRAFT_OBJECT_MOVE_FAILED.getErrorCode(),
					String.format(DRAFT_OBJECT_MOVE_FAILED.getErrorMessage(), srcKey + " -> " + destKey), e);
		}
	}

	/**
	 * Reports whether an adapter failure was caused by a missing key (HTTP 404 or
	 * {@code NoSuchKey}).
	 *
	 * @param e adapter exception thrown by {@link ObjectStoreAdapter#moveObject}
	 * @return {@code true} when the cause is a missing-object error
	 */
	private boolean isMissingSourceKey(ObjectStoreAdapterException e) {
		return e.getCause() instanceof Exception cause && isMissingObject(cause);
	}

	/**
	 * Server-side copy from {@code srcKey} to {@code destKey}. Pass
	 * {@code deleteSourceAfterCopy=false} when copying live data into a draft, so the
	 * live record stays available for authentication and failure recovery.
	 *
	 * @param srcKey                full source key
	 * @param destKey               full destination key
	 * @param deleteSourceAfterCopy {@code true} to delete the source after the copy
	 * @throws IdRepoAppException {@code DRAFT_OBJECT_COPY_FAILED} when the adapter fails or
	 *         returns {@code false} (adapter does not support copy)
	 */
	private void copyRaw(String srcKey, String destKey, boolean deleteSourceAfterCopy) throws IdRepoAppException {
		ObjectStoreReference src = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, srcKey);
		ObjectStoreReference dst = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, destKey);
		try {
			boolean copied = objectStore.moveObject(src, dst, deleteSourceAfterCopy);
			if (!copied) {
				throw new IdRepoAppException(DRAFT_OBJECT_COPY_FAILED.getErrorCode(),
						String.format(DRAFT_OBJECT_COPY_FAILED.getErrorMessage(), srcKey + " -> " + destKey));
			}
			mosipLogger.debug("Copied object: {} -> {}", srcKey, destKey);
		} catch (ObjectStoreAdapterException e) {
			throw new IdRepoAppException(DRAFT_OBJECT_COPY_FAILED.getErrorCode(),
					String.format(DRAFT_OBJECT_COPY_FAILED.getErrorMessage(), srcKey + " -> " + destKey), e);
		}
	}
}
