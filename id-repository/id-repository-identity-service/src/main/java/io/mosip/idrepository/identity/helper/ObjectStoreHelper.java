package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ADAPTER_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_BUCKET_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

/**
 * @author Manoj SP
 *
 */
@Component
public class ObjectStoreHelper {
	
	@Value("${mosip.idrepo.crypto.refId.bio-doc-data}")
	private String bioDataRefId;
	
	@Value("${mosip.idrepo.crypto.refId.demo-doc-data}")
	private String demoDataRefId;

	/** The Constant SLASH. */
	private static final String SLASH = "/";

	/** The Constant BIOMETRICS. */
	private static final String BIOMETRICS = "Biometrics";

	/** The Constant DEMOGRAPHICS. */
	private static final String DEMOGRAPHICS = "Demographics";

	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	private String objectStoreAccountName;

	@Value("${" + OBJECT_STORE_BUCKET_NAME + "}")
	private String objectStoreBucketName;

	@Value("${" + OBJECT_STORE_ADAPTER_NAME + "}")
	private String objectStoreAdapterName;
	
	private ObjectStoreAdapter objectStore;

	@Autowired
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	public boolean demographicObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, false, fileRefId);
	}

	public boolean biometricObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, true, fileRefId);
	}

	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException, IOException {
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException, IOException {
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}

	private boolean exists(String uinHash, boolean isBio, String fileRefId) {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId) throws IdRepoAppException {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName,
				new ByteArrayInputStream(securityManager.encrypt(data, refId)));
	}

	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId) throws IdRepoAppException, IOException {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return securityManager.decrypt(IOUtils.toByteArray(
				objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName)), refId);
	}
}
