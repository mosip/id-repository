package io.mosip.idrepository.core.constant;

import static io.mosip.kernel.biometrics.constant.BiometricType.FACE;
import static io.mosip.kernel.biometrics.constant.BiometricType.FINGER;
import static io.mosip.kernel.biometrics.constant.BiometricType.IRIS;

import java.util.List;

import io.mosip.kernel.biometrics.constant.BiometricType;

/**
 * The Enum IdRepoConstants - contains constants used internally by the
 * application.
 *
 * @author Manoj SP
 */
public class IdRepoConstants {

	/** The cbeff format. */
	public static final String CBEFF_FORMAT = "cbeff";

	/** The identity file name format. */
	public static final String FILE_FORMAT_ATTRIBUTE = "format";

	/** The identity file name key. */
	public static final String FILE_NAME_ATTRIBUTE = "value";
	
	public static final String VID_TYPE_PATH = "vidPolicies.*.vidType";
	
	public static final String VID_POLICY_PATH = "vidPolicies.*.vidPolicy";

	/** The root path. */
	public static final String ROOT_PATH = "identity";

	/** The version pattern. */
	public static final String VERSION_PATTERN = "mosip.idrepo.application.version.pattern";

	/** The datetime timezone. */
	public static final String DATETIME_TIMEZONE = "mosip.idrepo.datetime.timezone";
	
	/** datetime adjustment in minutes **/
	public static final String DATETIME_ADJUSTMENT = "mosip.idrepo.datetime.future-time-adjustment";

	/** The status registered. */
	public static final String ACTIVE_STATUS = "mosip.idrepo.identity.uin-status.registered";

	/** The datetime pattern. */
	public static final String DATETIME_PATTERN = "mosip.utc-datetime-pattern";

	/** The application version. */
	public static final String APPLICATION_VERSION = "mosip.idrepo.identity.application.version";
	
	/** The application version. */
	public static final String APPLICATION_VERSION_VID = "mosip.idrepo.vid.application.version";

	/** The application id. */
	public static final String APPLICATION_ID = "mosip.idrepo.app-id";

	/** The application name. */
	public static final String APPLICATION_NAME = "mosip.idrepo.application.name";

	/** The json schema file name. */
	public static final String JSON_SCHEMA_FILE_NAME = "mosip.idrepo.json-schema-fileName";
	
	/** The Json path value */
	public static final String MOSIP_KERNEL_IDREPO_JSON_PATH = "mosip.idrepo.identity.json.path";
	
	public static final String VID_ACTIVE_STATUS = "mosip.idrepo.vid.active-status";
	
	public static final String VID_ALLOWED_STATUS = "mosip.idrepo.vid.allowedStatus";
	
	public static final String VID_DB_URL = "mosip.idrepo.vid.db.url";
	
	public static final String VID_DB_USERNAME = "mosip.idrepo.vid.db.username";
	
	public static final String VID_DB_PASSWORD = "mosip.idrepo.vid.db.password";
	
	public static final String VID_DB_DRIVER_CLASS_NAME = "mosip.idrepo.vid.db.driverClassName";
	
	public static final String VID_POLICY_FILE_URL = "mosip.idrepo.vid.policy-file-url";
	
	public static final String VID_POLICY_SCHEMA_URL = "mosip.idrepo.vid.policy-schema-url";
	
	public static final String VID_UNLIMITED_TRANSACTION_STATUS = "mosip.idrepo.vid.unlimited-txn-status";
	
	public static final String VID_REGENERATE_ALLOWED_STATUS = "mosip.idrepo.vid.regenerate.allowed-status";
	
	public static final String VID_REGENERATE_ACTIVE_STATUS = "INVALIDATED";
	
	public static final String SALT_KEY_LENGTH = "mosip.identity.salt.key.length";
	
	public static final String SPLITTER = "_";
	
	public static final String VID_DEACTIVATED = "mosip.idrepo.vid.deactive-status";
	
	public static final String VID_REACTIVATED = "mosip.idrepo.vid.reactive-status";
	
	public static final String IDA_NOTIFY_REQ_ID = "ida.api.id.event.notify";
	
	public static final String IDA_NOTIFY_REQ_VER = "ida.api.version.event.notify";
	
	public static final String WEB_SUB_PUBLISH_URL = "websub.publish.url";
	
	public static final String WEB_SUB_HUB_URL = "websub.hub.url";
	
	public static final String OBJECT_STORE_ACCOUNT_NAME = "mosip.idrepo.objectstore.account-name";
	
	public static final String OBJECT_STORE_BUCKET_NAME = "mosip.idrepo.objectstore.bucket-name";
	
	public static final String OBJECT_STORE_ADAPTER_NAME = "mosip.idrepo.objectstore.adapter-name";
	
	public static final String PREPEND_THUMPRINT_STATUS = "mosip.credential.service.share.prependThumbprint";
	
	public static final String IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID = "idrepo-dummy-online-verification-partner-id";
	
	public static final String MOSIP_OLV_PARTNER = "MOSIP_OLV_PARTNER";
	
	public static final String CREDENTIAL_STATUS_JOB_DELAY = "mosip.idrepo.credential-status-update-job.fixed-delay-in-ms";
	
	public static final String UIN_REFID = "mosip.idrepo.crypto.refId.uin";
	
	public static final String UIN_DATA_REFID = "mosip.idrepo.crypto.refId.uin-data";
	
	public static final String BIO_DATA_REFID = "mosip.idrepo.crypto.refId.bio-doc-data";
	
	public static final String DEMO_DATA_REFID = "mosip.idrepo.crypto.refId.demo-doc-data";
	
	public static final String  VID_EVENT_TOPIC = "mosip.idrepo.websub.vid-credential-update.topic";
	
	public static final String  VID_EVENT_SECRET = "mosip.idrepo.websub.vid-credential-update.secret";
	
	public static final String  VID_EVENT_CALLBACK_URL = "mosip.idrepo.websub.vid-credential-update.callback-url";
	
	public static final String CREDENTIAL_STATUS_UPDATE_TOPIC = "mosip.idrepo.websub.credential-status-update.topic";

	public static final String REMOVE_ID_STATUS_EVENT_TOPIC = "mosip.idrepo.websub.remove-id-status.topic";

	public static final String REMOVE_ID_STATUS_EVENT_SECRET = "mosip.idrepo.websub.remove-id-status.secret";

	public static final String REMOVE_ID_STATUS_EVENT_CALLBACK_RELATIVE_URL = "idrepo.websub.callback.remove-id-status.relative.url";

	public static final String REMOVE_ID_STATUS_EVENT_CALLBACK_URL = "mosip.idrepo.websub.remove-id-status.callback-url";

	public static final String FACE_EXTRACTION_FORMAT = "faceExtractionFormat";

	public static final String IRIS_EXTRACTION_FORMAT = "irisExtractionFormat";

	public static final String FINGER_EXTRACTION_FORMAT = "fingerExtractionFormat";
	
	public static final String CREDENTIAL_CRYPTO_REF_ID = "mosip.credential.request.crypto-ref-id";
	
	public static final String IDENTITY_MAPPING_JSON = "mosip.identity.mapping-file";

	
	/** The value. */
	private final String value;

	/** The Constant TOKEN. */
	public static final String TOKEN = "TOKEN";

	/** The Constant ID_HASH. */
	public static final String ID_HASH = "id_hash";

	/** The Constant EXPIRY_TIMESTAMP. */
	public static final String EXPIRY_TIMESTAMP = "expiry_timestamp";

	/** The Constant TRANSACTION_LIMIT. */
	public static final String TRANSACTION_LIMIT = "transaction_limit";

	/** The Constant ID_REPO. */
	public static final String ID_REPO = "ID_REPO";
	
	public static final String DOT = ".";
	
	public static final String EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX = "ExtractionFormat";
	
	public static final String DRAFT_STATUS = "DRAFT";
	
	public static final String DEFAULT_VID_TYPE = "mosip.idrepo.draft-vid.default-type-to-create";
	
	public static final String VID_CREATE_ID = "mosip.idrepo.vid.id.create";
	
	public static final String VID_UPDATE_ID = "mosip.idrepo.vid.id.update";
	
	public static final int DEFAULT_SALT_KEY_LENGTH = 3;
	
	public static final String IDREPO_CACHE_UPDATE_INTERVAL = "mosip.idrepo.cache.update.interval.milli.seconds";
	
	public static final int CACHE_UPDATE_DEFAULT_INTERVAL = 7200000;
  
	public static final String ERRORS = "errors";
	
	public static final List<BiometricType> SUPPORTED_MODALITIES = List.of(FINGER, IRIS, FACE);

	/** The Constant GET_FILES. */
	public static final String GET_FILES = "getFiles";

	/** The Constant UPDATE_IDENTITY. */
	public static final String UPDATE_IDENTITY = "updateIdentity";

	/** The Constant MOSIP_ID_UPDATE. */
	public static final String MOSIP_ID_UPDATE = "mosip.id.update";

	/** The Constant ADD_IDENTITY. */
	public static final String ADD_IDENTITY = "addIdentity";

	/** The Constant RETRIEVE_IDENTITY. */
	public static final String RETRIEVE_IDENTITY = "retrieveIdentity";

	/** The Constant BIOMETRICS. */
	public static final String BIOMETRICS = "Biometrics";

	/** The Constant BIO. */
	public static final String BIO = "bio";

	/** The Constant DEMO. */
	public static final String DEMO = "demo";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	public static final String ID_REPO_SERVICE_IMPL = "IdRepoServiceImpl";

	/** The Constant CREATE. */
	public static final String CREATE = "create";

	/** The Constant UPDATE. */
	public static final String UPDATE = "update";

	/** The Constant READ. */
	public static final String READ = "read";

	/** The Constant ALL. */
	public static final String ALL = "all";

	/** The Constant DEMOGRAPHICS. */
	public static final String DEMOGRAPHICS = "Demographics";

	public static final String DRAFT_RECORD_NOT_FOUND = "DRAFT RECORD NOT FOUND";

	public static final String VERIFIED_ATTRIBUTES = "verifiedAttributes";

	public static final String GET_DRAFT = "getDraft";

	public static final String DISCARD_DRAFT = "discardDraft";

	public static final String PUBLISH_DRAFT = "publishDraft";

	public static final String DRAFTED = "DRAFTED";

	public static final String UPDATE_DRAFT = "UpdateDraft";

	public static final String GENERATE_UIN = "generateUin";

	public static final String CREATE_DRAFT = "createDraft";

	public static final String ID_REPO_DRAFT_SERVICE_IMPL = "IdRepoDraftServiceImpl";

	public static final String AUTH_TYPE_SEPERATOR = "-";

	public static final String GET_DRAFT_UIN_ID = "mosip.identity.get.drafts.id";

	public static final String GET_DRAFT_UIN_VERSION = "mosip.identity.get.drafts.version";

	public static final String EXCLUDED_ATTRIBUTE_LIST = "mosip.identity.get.excluded.attribute.list";
	
	/** Cache Names in IDRepo */
	
	public static final String CREDENTIAL_TRANSACTION_CACHE = "credential_transaction";
	
	public static final String PARTNER_EXTRACTOR_FORMATS_CACHE = "partner_extractor_formats";

	public static final String DATASHARE_POLICIES_CACHE = "datashare_policies";
	
	public static final String WEBSUB_TOPICS_CACHE = "topics";
	
	public static final String ONLINE_VERIFICATION_PARTNERS_CACHE = "online_verification_partners";
	
	public static final String UIN_ENCRYPT_SALT_CACHE = "uin_encrypt_salt";

	public static final String UIN_HASH_SALT_CACHE = "uin_hash_salt";

	public static final String ID_ATTRIBUTES_CACHE = "id_attributes";


	

	/**
	 * Instantiates a new id repo constants.
	 *
	 * @param value the value
	 */
	private IdRepoConstants(String value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
}
