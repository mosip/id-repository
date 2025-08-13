package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.BIO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ADAPTER_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_BUCKET_NAME;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import io.mosip.kernel.core.exception.ExceptionUtils;

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

	@Value("${object.store.connection.max.retry:20}") // Aligned with S3Adapter
	private int maxRetry;

	@Value("${object.store.chunk.size:1048576}") // 1MB chunks for streaming
	private int chunkSize;

	@Value("${object.store.operation.timeout:15000}") // 15s timeout for S3 operations
	private int operationTimeout;

	private ObjectStoreAdapter objectStore;

	private Logger mosipLogger = IdRepoLogger.getLogger(ObjectStoreHelper.class);

	@Autowired
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	@Autowired
	private IdRepoSecurityManager securityManager;

	public boolean demographicObjectExists(String uinHash, String fileRefId) throws IdRepoAppException {
		return exists(uinHash, false, fileRefId);
	}

	public boolean biometricObjectExists(String uinHash, String fileRefId) throws IdRepoAppException {
		return exists(uinHash, true, fileRefId);
	}

	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.demographicObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.biometricObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}

	public void deleteBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (this.biometricObjectExists(uinHash, fileRefId)) {
			String objectName = uinHash + SLASH + BIOMETRICS + SLASH + fileRefId;
			mosipLogger.info("Attempting to delete biometric object: " + objectName);
			retry(() -> objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName),
					"deleteObject");
			mosipLogger.info("Successfully deleted biometric object: " + objectName);
		}
	}

	private boolean exists(String uinHash, boolean isBio, String fileRefId) throws IdRepoAppException {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		mosipLogger.info("Checking existence of object: " + objectName);
		try {
			boolean exists = retry(() -> objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName),
					"exists");
			mosipLogger.info("Object exists check for {}: {}", objectName, exists);
			return exists;
		} catch (ObjectStoreAdapterException e) {
			mosipLogger.error("ObjectStoreAdapterException during exists check for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "Failed to check object existence: " + e.getErrorCode(), e);
		}
	}

	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId) throws IdRepoAppException {
		if (data == null || data.length == 0) {
			mosipLogger.error("Invalid input data for putObject: uinHash=" + uinHash + ", fileRefId=" + fileRefId);
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "Input data is null or empty");
		}
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		mosipLogger.info("Putting object: " + objectName);
		try {
			long encryptStartTime = System.currentTimeMillis();
			ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			try (InputStream input = new ByteArrayInputStream(data)) {
				byte[] buffer = new byte[chunkSize];
				int bytesRead;
				while ((bytesRead = input.read(buffer)) != -1) {
					byte[] chunk = bytesRead == chunkSize ? buffer : Arrays.copyOf(buffer, bytesRead);
					encryptedStream.write(securityManager.encrypt(chunk, refId));
				}
			}
			mosipLogger.debug("Encryption time for {}: {} ms", objectName, System.currentTimeMillis() - encryptStartTime);
			try (InputStream encryptData = new ByteArrayInputStream(encryptedStream.toByteArray())) {
				retry(() -> objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName, encryptData),
						"putObject");
			}
			mosipLogger.info("Successfully put object: " + objectName);
		} catch (IOException e) {
			mosipLogger.error("IOException during putObject for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		} catch (ObjectStoreAdapterException e) {
			mosipLogger.error("ObjectStoreAdapterException during putObject for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "S3 error: " + e.getErrorCode(), e);
		} catch (Exception e) {
			mosipLogger.error("Unexpected exception during putObject for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		}
	}

	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId) throws IdRepoAppException {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		mosipLogger.info("Getting object: " + objectName);
		try {
			long startTime = System.currentTimeMillis();
			try (InputStream s3Stream = retry(() -> objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName),
					"getObject")) {
				if (s3Stream == null) {
					mosipLogger.error("Object not found: " + objectName);
					throw new IdRepoAppException(FILE_NOT_FOUND);
				}
				mosipLogger.debug("S3 getObject time for {}: {} ms", objectName, System.currentTimeMillis() - startTime);
				long decryptStartTime = System.currentTimeMillis();
				ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[chunkSize];
				int bytesRead;
				while ((bytesRead = s3Stream.read(buffer)) != -1) {
					byte[] chunk = bytesRead == chunkSize ? buffer : Arrays.copyOf(buffer, bytesRead);
					decryptedStream.write(securityManager.decrypt(chunk, refId));
				}
				mosipLogger.debug("Decryption time for {}: {} ms", objectName, System.currentTimeMillis() - decryptStartTime);
				return decryptedStream.toByteArray();
			}
		} catch (IOException e) {
			mosipLogger.error("IOException during getObject for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		} catch (ObjectStoreAdapterException e) {
			mosipLogger.error("ObjectStoreAdapterException during getObject for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "S3 error: " + e.getErrorCode(), e);
		} catch (Exception e) {
			mosipLogger.error("Unexpected exception during getObject for: " + objectName, ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		}
	}

	private <T> T retry(Callable<T> operation, String operationName) throws IdRepoAppException {
		for (int attempt = 1; attempt <= maxRetry; attempt++) {
			try {
				long startTime = System.currentTimeMillis();
				T result = operation.call();
				mosipLogger.debug("Operation {} completed in {} ms", operationName, System.currentTimeMillis() - startTime);
				return result;
			} catch (Exception e) {
				mosipLogger.warn("Error during {} attempt {} for: {}", operationName, attempt, objectStoreBucketName, ExceptionUtils.getStackTrace(e));
				if (attempt == maxRetry) {
					throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "Operation " + operationName + " failed after " + maxRetry + " attempts", e);
				}
				try {
					Thread.sleep(1000L * attempt); // Exponential backoff
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR.getErrorCode(), "Interrupted during retry", ie);
				}
			}
		}
		return null;
	}
}