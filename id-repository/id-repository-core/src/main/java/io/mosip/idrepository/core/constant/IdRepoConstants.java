package io.mosip.idrepository.core.constant;

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
	
	public static final String MODULO_VALUE = "mosip.idrepo.modulo-value";
	
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