package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.BIO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ADAPTER_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_BUCKET_NAME;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.mosip.kernel.core.logger.spi.Logger;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;

/**
 * Helper bean supporting identity service operations (ObjectStoreHelper).
 */
@Component
public class ObjectStoreHelper {
	
	@Value("${" + BIO_DATA_REFID + "}")
	/** Bio data ref id. */
	private String bioDataRefId;
	
	@Value("${" + DEMO_DATA_REFID + "}")
	/** Demo data ref id. */
	private String demoDataRefId;

	/** The Constant SLASH. */
	private static final String SLASH = "/";

	/** The Constant BIOMETRICS. */
	private static final String BIOMETRICS = "Biometrics";

	/** The Constant DEMOGRAPHICS. */
	private static final String DEMOGRAPHICS = "Demographics";

	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	/** Object store account name. */
	private String objectStoreAccountName;

	@Value("${" + OBJECT_STORE_BUCKET_NAME + "}")
	/** Object store bucket name. */
	private String objectStoreBucketName;

	@Value("${" + OBJECT_STORE_ADAPTER_NAME + "}")
	/** Object store adapter name. */
	private String objectStoreAdapterName;
	
	/** Object store. */
	private ObjectStoreAdapter objectStore;

	/** Mosip logger. */
	private Logger mosipLogger = IdRepoLogger.getLogger(ObjectStoreHelper.class);

	@Autowired
	/**
	 * @param context context
	 */
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	/** Security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/**
	 * Demographic object exists.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @return boolean
	 */
	public boolean demographicObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, false, fileRefId);
	}

	/**
	 * Biometric object exists.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @return boolean
	 */
	public boolean biometricObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, true, fileRefId);
	}

	/**
	 * Put demographic object.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @param data data
	 */
	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	/**
	 * Put biometric object.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @param data data
	 */
	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	/**
	 * Get demographic object.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @return byte[]
	 */
	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.demographicObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	/**
	 * Get biometric object.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @return byte[]
	 */
	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.biometricObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}
	
	/**
	 * Delete biometric object.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 */
	public void deleteBiometricObject(String uinHash, String fileRefId) {
		if (this.biometricObjectExists(uinHash, fileRefId)) {
			String objectName = uinHash + SLASH + BIOMETRICS + SLASH + fileRefId;
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		}
	}

	private boolean exists(String uinHash, boolean isBio, String fileRefId) {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId)
			throws IdRepoAppException {
		try {
			String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
			long encryptStartTime = System.currentTimeMillis();
			InputStream encryptData = new ByteArrayInputStream(securityManager.encrypt(data, refId));
			long startTime = System.currentTimeMillis();
			objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName, encryptData);
		} catch (AmazonS3Exception | FSAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		} catch (Throwable e) {
			mosipLogger.error("Exception in connection>>>", e);
		}
	}

	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId) throws IdRepoAppException {
		try {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return securityManager.decrypt(IOUtils.toByteArray(
				objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName)), refId);
		} catch (AmazonS3Exception | FSAdapterException | IOException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR);
		}
	}
}
