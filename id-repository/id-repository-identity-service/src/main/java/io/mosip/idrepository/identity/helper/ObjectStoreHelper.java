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

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class ObjectStoreHelper {

	@Value("${" + BIO_DATA_REFID + "}")
	private String bioDataRefId;

	@Value("${" + DEMO_DATA_REFID + "}")
	private String demoDataRefId;

	private static final String SLASH = "/";
	private static final String BIOMETRICS = "Biometrics";
	private static final String DEMOGRAPHICS = "Demographics";

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

	public boolean demographicObjectExists(String uinHash, String fileRefId)  {
		return exists(uinHash, false, fileRefId);
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
}
