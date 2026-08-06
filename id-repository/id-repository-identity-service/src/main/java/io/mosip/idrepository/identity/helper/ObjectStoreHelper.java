package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.*;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.mosip.commons.khazana.dto.ObjectStoreReference;
import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;

@Component
public class ObjectStoreHelper {

	@Value("${" + BIO_DATA_REFID + "}")
	private String bioDataRefId;

	@Value("${" + DEMO_DATA_REFID + "}")
	private String demoDataRefId;

	private static final String SLASH = "/";
	private static final String BIOMETRICS = "Biometrics";
	private static final String DEMOGRAPHICS = "Demographics";
	private static final String DRAFT = "_draft";

	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	private String objectStoreAccountName;

	@Value("${" + OBJECT_STORE_BUCKET_NAME + "}")
	private String objectStoreBucketName;

	@Value("${" + OBJECT_STORE_ADAPTER_NAME + "}")
	private String objectStoreAdapterName;

	@Value("${mosip.idrepo.objectstore.max-object-size-bytes:10485760}")
	private long maxObjectSizeBytes = 10 * 1024 * 1024L; // 10 MB; also configurable via property

	private ObjectStoreAdapter objectStore;

	private static final Logger mosipLogger = IdRepoLogger.getLogger(ObjectStoreHelper.class);

	@Autowired
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	@Autowired
	private IdRepoSecurityManager securityManager;

	public String getRidHash(String registrationId) {
		try {
			return HMACUtils2.digestAsPlainText(registrationId.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("Failed to compute rid hash for: " + registrationId, e);
		}
	}

	public boolean biometricObjectExists(String uinHash, String fileRefId)  {
		return exists(uinHash, true, fileRefId);
	}

	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException {
		// No pre-flight exists() check — getObject() already throws FILE_NOT_FOUND when
		// the stream is null, so the extra round-trip to the object store is unnecessary.
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		// No pre-flight exists() check — same reasoning as getDemographicObject above.
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}

	public void deleteBiometricObject(String uinHash, String fileRefId)  {
		if (this.biometricObjectExists(uinHash, fileRefId)) {
			String objectName = uinHash + SLASH + BIOMETRICS + SLASH + fileRefId;
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		}
	}

	// ── Draft path: {pathPrefix}/_draft/Biometrics/{fileRefId}  ──────────────────

	public void putDraftBiometricObject(String pathPrefix, String fileRefId, byte[] data) throws IdRepoAppException {
		putDraftObject(pathPrefix, true, fileRefId, data, bioDataRefId);
	}

	public void putDraftDemographicObject(String pathPrefix, String fileRefId, byte[] data) throws IdRepoAppException {
		putDraftObject(pathPrefix, false, fileRefId, data, demoDataRefId);
	}

	public boolean draftBiometricObjectExists(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, true, fileRefId);
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	public boolean draftDemographicObjectExists(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, false, fileRefId);
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	public byte[] getDraftBiometricObject(String ridHash, String fileRefId) throws IdRepoAppException {
		return getDraftObject(ridHash, true, fileRefId, bioDataRefId);
	}

	public byte[] getDraftDemographicObject(String ridHash, String fileRefId) throws IdRepoAppException {
		return getDraftObject(ridHash, false, fileRefId, demoDataRefId);
	}

	public void deleteDraftBiometricObject(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, true, fileRefId);
		try {
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		} catch (Exception e) {
			mosipLogger.debug("Draft biometric object not found or already deleted, skipping: {}", objectName);
		}
	}

	public void deleteDraftDemographicObject(String pathPrefix, String fileRefId) {
		String objectName = buildDraftObjectName(pathPrefix, false, fileRefId);
		try {
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		} catch (Exception e) {
			mosipLogger.debug("Draft demographic object not found or already deleted, skipping: {}", objectName);
		}
	}

	public void moveBiometricDraftToLive(String srcPrefix, String destPrefix, String fileRefId)
			throws IdRepoAppException {
		String srcKey = buildDraftObjectName(srcPrefix, true, fileRefId);
		String destKey = buildObjectName(destPrefix, true, fileRefId);
		moveRaw(srcKey, destKey);
	}

	public void moveDemographicDraftToLive(String srcPrefix, String destPrefix, String fileRefId)
			throws IdRepoAppException {
		String srcKey = buildDraftObjectName(srcPrefix, false, fileRefId);
		String destKey = buildObjectName(destPrefix, false, fileRefId);
		moveRaw(srcKey, destKey);
	}

	// Copies live biometric/demographic files into the draft path so that
	// updateDraft can read and merge them (UPDATE/ACTIVATED/DEACTIVATED packets).
	public void copyBiometricLiveToDraft(String livePrefix, String ridHash, String fileRefId)
			throws IdRepoAppException {
		copyRaw(buildObjectName(livePrefix, true, fileRefId), buildDraftObjectName(ridHash, true, fileRefId), false);
	}

	public void copyDemographicLiveToDraft(String livePrefix, String ridHash, String fileRefId)
			throws IdRepoAppException {
		copyRaw(buildObjectName(livePrefix, false, fileRefId), buildDraftObjectName(ridHash, false, fileRefId), false);
	}

	private boolean exists(String uinHash, boolean isBio, String fileRefId) {
		String objectName = buildObjectName(uinHash, isBio, fileRefId);
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId)
			throws IdRepoAppException {
		if (data == null || data.length == 0) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Input data is null or empty");
		}

		String objectName = buildObjectName(uinHash, isBio, fileRefId);

		try (InputStream encryptData = new ByteArrayInputStream(securityManager.encrypt(data, refId))) {
			objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName, encryptData);
			mosipLogger.debug("Uploaded object: {} ({} bytes)", objectName, data.length);
		} catch (IOException | ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to store object: " + e.getMessage(), e);
		}
	}

	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId)
			throws IdRepoAppException {
		String objectName = buildObjectName(uinHash, isBio, fileRefId);

		// Separate the store fetch from stream processing so a store-level failure
		// (e.g. auth error) surfaces as FILE_STORAGE_ACCESS_ERROR, not FILE_NOT_FOUND.
		InputStream rawStream;
		try {
			rawStream = objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		} catch (ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to fetch object: " + objectName, e);
		}

		if (rawStream == null) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}

		// BoundedInputStream limits read to maxObjectSizeBytes+1 so we can detect oversized objects
		// without reading the entire stream, preventing unbounded heap allocation.
		try (InputStream s3Stream = new BoundedInputStream(new BufferedInputStream(rawStream), maxObjectSizeBytes + 1)) {
			byte[] encryptedData = IOUtils.toByteArray(s3Stream);
			if (encryptedData.length > maxObjectSizeBytes) {
				throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
						"Object size exceeds allowed limit (" + maxObjectSizeBytes + " bytes): " + objectName);
			}
			byte[] decryptedData = securityManager.decrypt(encryptedData, refId);
			encryptedData = null; // release encrypted copy; decryptedData is the only live reference now
			return decryptedData;
		} catch (IOException | ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to retrieve object: " + e.getMessage(), e);
		}
	}

	private String buildObjectName(String uinHash, boolean isBio, String fileRefId) {
		return uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
	}

	private String buildDraftObjectName(String ridHash, boolean isBio, String fileRefId) {
		return DRAFT + SLASH + ridHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
	}

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
	 * Performs a server-side copy from srcKey to destKey.
	 * Pass deleteSourceAfterCopy=true only when the source is draft data being
	 * promoted to live (draft→live), so the draft file is removed as part of the
	 * move. Pass false when copying live data into the draft path (live→draft) to
	 * preserve the live record for authentication and failure recovery.
	 */
	private void moveRaw(String srcKey, String destKey) throws IdRepoAppException {
		ObjectStoreReference src = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, srcKey);
		ObjectStoreReference dst = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, destKey);
		try {
			boolean moved = objectStore.moveObject(src, dst, true);
			if (!moved) {
				mosipLogger.debug("Draft object not found, skipping move: {}", srcKey);
			}
		} catch (ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to move object: " + srcKey + " -> " + destKey, e);
		}
	}

	private void copyRaw(String srcKey, String destKey, boolean deleteSourceAfterCopy) throws IdRepoAppException {
		ObjectStoreReference src = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, srcKey);
		ObjectStoreReference dst = new ObjectStoreReference(
				objectStoreAccountName, objectStoreBucketName, null, null, destKey);
		try {
			boolean copied = objectStore.moveObject(src, dst, deleteSourceAfterCopy);
			if (!copied) {
				throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
						"Server-side copy returned false: " + srcKey + " -> " + destKey);
			}
			mosipLogger.debug("Copied object: {} -> {}", srcKey, destKey);
		} catch (ObjectStoreAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
					"Failed to copy object: " + srcKey + " -> " + destKey, e);
		}
	}
}
