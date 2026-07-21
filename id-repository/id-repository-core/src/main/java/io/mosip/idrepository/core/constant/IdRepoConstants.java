package io.mosip.idrepository.core.constant;

import static io.mosip.kernel.biometrics.constant.BiometricType.FACE;
import static io.mosip.kernel.biometrics.constant.BiometricType.FINGER;
import static io.mosip.kernel.biometrics.constant.BiometricType.IRIS;

import java.util.List;

import io.mosip.kernel.biometrics.constant.BiometricType;

/**
 * Shared configuration keys, JSON field names, and operational literals for ID Repository.
 *
 * <p>
 * String constants whose names are uppercase with underscores are either Spring Cloud Config
 * property keys (resolved via {@link io.mosip.idrepository.core.util.EnvUtil}) or fixed
 * literal values used in identity JSON, VID policy, credential pipelines, WebSub, caching,
 * and HikariCP pool sizing. Integer/long defaults accompany many keys for local and
 * Kubernetes deployments when config is absent.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Acts as the single catalogue of property key names and domain literals so identity, VID,
 * credential-store, credential-request, and core helpers do not hard-code config strings.
 * {@link io.mosip.idrepository.core.util.EnvUtil} is the primary runtime resolver for the
 * config-key subset; other classes import literals (JSON paths, cache region names, status
 * strings) directly.
 * </p>
 *
 * <h2>Field groups (overview)</h2>
 * <ul>
 *   <li><b>Identity JSON / CBEFF</b> — file attributes, root path, schema/version/datetime keys</li>
 *   <li><b>VID configuration</b> — status, DB, policy, regenerate, salt key length</li>
 *   <li><b>IDA / WebSub</b> — notify IDs, credential type/recipient, event namespace, hub URLs</li>
 *   <li><b>Object store &amp; partners</b> — account/bucket/adapter, OLV partner, thumbprint</li>
 *   <li><b>HikariCP</b> — per-PU pool max/min/timeout defaults</li>
 *   <li><b>Crypto reference IDs</b> — UIN, demographic, biometric, credential ref-ids</li>
 *   <li><b>Pipeline / identity runtime</b> — force-merge, extract pools, DB keys</li>
 *   <li><b>Credential store / WebSub</b> — MVEL, vercred, datashare, subscription delays</li>
 *   <li><b>Token / REST controller / EnvUtil bootstrap</b> — Keycloak token keys, RID/metadata IDs</li>
 *   <li><b>Biometric extraction &amp; caches</b> — modality formats, Spring cache region names</li>
 *   <li><b>Service method / JSON literals</b> — method names, bio/demo labels, draft statuses</li>
 * </ul>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * <strong>Critical:</strong> This class is part of the published {@code id-repository-core}
 * API surface referenced by ID Authentication tooling and shared helpers. IDA does
 * <strong>not</strong> read id-repo salt tables, but it does depend on stable WebSub and
 * credential-related configuration semantics:
 * </p>
 * <ul>
 *   <li>{@link #WEB_SUB_PUBLISH_URL}, {@link #WEB_SUB_HUB_URL} — hub connectivity</li>
 *   <li>{@link #IDA_CREDENTIAL_TYPE}, {@link #IDA_CREDENTIAL_RECIPIENT},
 *       {@link #IDA_EVENT_TYPE_NAMESPACE}, {@link #IDA_EVENT_TYPE_NAME} (+ {@code *_DEFAULT}) —
 *       credential event typing for IDA</li>
 *   <li>{@link #IDA_NOTIFY_REQ_ID}, {@link #IDA_NOTIFY_REQ_VER} — IDA notify API metadata</li>
 *   <li>{@link #ID_HASH}, {@link #TOKEN}, {@link #EXPIRY_TIMESTAMP}, {@link #TRANSACTION_LIMIT} —
 *       fields appearing in credential / WebSub additional data</li>
 *   <li>{@link #CREDENTIAL_STATUS_UPDATE_TOPIC}, {@link #VID_EVENT_TOPIC} — topic config keys</li>
 * </ul>
 * <p>
 * Do not rename public static field names or change property-key <em>string values</em>
 * without coordinating IDA and Spring Cloud Config overlays. Literal defaults that appear
 * in payloads (e.g. {@link #IDA_CREDENTIAL_TYPE_DEFAULT} {@code "auth"}) are especially
 * sensitive.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Config key resolved by EnvUtil / @Value
 * {@literal @}Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
 * private String publisherUrl;
 *
 * // Literal in credential additional data
 * additionalData.put(IdRepoConstants.ID_HASH, idHash);
 *
 * // Pool default when property absent
 * int max = env.getProperty(IdRepoConstants.IDENTITY_POOL_MAX, Integer.class,
 *     IdRepoConstants.IDENTITY_POOL_MAX_DEFAULT);
 * </pre>
 *
 * @author Manoj SP
 * @see RestServicesConstants
 * @see IDAEventType
 * @see io.mosip.idrepository.core.util.EnvUtil
 * @see io.mosip.idrepository.core.helper.IdRepoWebSubHelper
 */
public class IdRepoConstants {

	// ---- Identity JSON / CBEFF ----

	/** CBEFF format label in identity JSON file attributes. */
	public static final String CBEFF_FORMAT = "cbeff";

	/** JSON attribute name for file format (e.g. {@code cbeff}). */
	public static final String FILE_FORMAT_ATTRIBUTE = "format";

	/** JSON attribute name for file name / reference value. */
	public static final String FILE_NAME_ATTRIBUTE = "value";

	/** Config path pattern for VID type entries in VID policy JSON. */
	public static final String VID_TYPE_PATH = "vidPolicies.*.vidType";

	/** Config path pattern for VID policy entries in VID policy JSON. */
	public static final String VID_POLICY_PATH = "vidPolicies.*.vidPolicy";

	/** Root JSON path for the identity object. */
	public static final String ROOT_PATH = "identity";

	/** Config key: application version regex pattern. */
	public static final String VERSION_PATTERN = "mosip.idrepo.application.version.pattern";

	/** Config key: datetime timezone for identity timestamps. */
	public static final String DATETIME_TIMEZONE = "mosip.idrepo.datetime.timezone";

	/** Config key: minutes to adjust future-dated timestamps during validation. */
	public static final String DATETIME_ADJUSTMENT = "mosip.idrepo.datetime.future-time-adjustment";

	/** Config key: registered/active UIN status value. */
	public static final String ACTIVE_STATUS = "mosip.idrepo.identity.uin-status.registered";

	/** Config key: UTC datetime format pattern. */
	public static final String DATETIME_PATTERN = "mosip.utc-datetime-pattern";

	/** Config key: identity module application version. */
	public static final String APPLICATION_VERSION = "mosip.idrepo.identity.application.version";

	/** Config key: VID module application version. */
	public static final String APPLICATION_VERSION_VID = "mosip.idrepo.vid.application.version";

	/** Config key: MOSIP application ID for cryptomanager calls. */
	public static final String APPLICATION_ID = "mosip.idrepo.app-id";

	/** Config key: application display name. */
	public static final String APPLICATION_NAME = "mosip.idrepo.application.name";

	/** Config key: identity JSON schema file name on config server. */
	public static final String JSON_SCHEMA_FILE_NAME = "mosip.idrepo.json-schema-fileName";

	/** Config key: JSONPath expressions for identity field mapping. */
	public static final String MOSIP_KERNEL_IDREPO_JSON_PATH = "mosip.idrepo.identity.json.path";

	// ---- VID configuration keys ----

	/** Config key: VID status considered active. */
	public static final String VID_ACTIVE_STATUS = "mosip.idrepo.vid.active-status";

	/** Config key: comma-separated allowed VID statuses ({@code mosip.idrepo.vid.allowedstatus}). */
	public static final String VID_ALLOWED_STATUS = "mosip.idrepo.vid.allowedstatus";

	/** Config key: VID database JDBC URL ({@code mosip_idmap}). */
	public static final String VID_DB_URL = "mosip.idrepo.vid.db.url";

	/** Config key: VID database username. */
	public static final String VID_DB_USERNAME = "mosip.idrepo.vid.db.username";

	/** Config key: VID database password. */
	public static final String VID_DB_PASSWORD = "mosip.idrepo.vid.db.password";

	/** Config key: VID database JDBC driver class. */
	public static final String VID_DB_DRIVER_CLASS_NAME = "mosip.idrepo.vid.db.driverClassName";

	/** Config key: VID policy file URL on config server. */
	public static final String VID_POLICY_FILE_URL = "mosip.idrepo.vid.policy-file-url";

	/** Config key: VID policy JSON schema URL. */
	public static final String VID_POLICY_SCHEMA_URL = "mosip.idrepo.vid.policy-schema-url";

	/** Config key: VID status allowing unlimited transactions. */
	public static final String VID_UNLIMITED_TRANSACTION_STATUS = "mosip.idrepo.vid.unlimited-txn-status";

	/** Config key: VID statuses eligible for regeneration. */
	public static final String VID_REGENERATE_ALLOWED_STATUS = "mosip.idrepo.vid.regenerate.allowed-status";

	/** Literal VID status set when a VID is regenerated (previous VID invalidated). */
	public static final String VID_REGENERATE_ACTIVE_STATUS = "INVALIDATED";

	/** Config key: number of digits in UIN hash salt key index. */
	public static final String SALT_KEY_LENGTH = "mosip.identity.salt.key.length";

	/** Delimiter used when composing salt key strings. */
	public static final String SPLITTER = "_";

	/** Config key: VID deactivated status value. */
	public static final String VID_DEACTIVATED = "mosip.idrepo.vid.deactive-status";

	/** Config key: VID reactivated status value. */
	public static final String VID_REACTIVATED = "mosip.idrepo.vid.reactive-status";

	// ---- IDA / WebSub ----

	/** Config key: IDA event notify API request ID. */
	public static final String IDA_NOTIFY_REQ_ID = "ida.api.id.event.notify";

	/** Config key: IDA event notify API version. */
	public static final String IDA_NOTIFY_REQ_VER = "ida.api.version.event.notify";

	/** Config key: IDA credential type for WebSub credential events. */
	public static final String IDA_CREDENTIAL_TYPE = "id-repo-ida-credential-type";

	/** Default IDA credential type when config is absent. */
	public static final String IDA_CREDENTIAL_TYPE_DEFAULT = "auth";

	/** Config key: IDA credential recipient for WebSub events (legacy config key spelling). */
	public static final String IDA_CREDENTIAL_RECIPIENT = "id-repo-ida-credential-recepiant";

	/** Default IDA credential recipient when config is absent. */
	public static final String IDA_CREDENTIAL_RECIPIENT_DEFAULT = "IDA";

	/** Config key: IDA WebSub event type namespace. */
	public static final String IDA_EVENT_TYPE_NAMESPACE = "id-repo-ida-event-type-namespace";

	/** Default IDA WebSub event type namespace when config is absent. */
	public static final String IDA_EVENT_TYPE_NAMESPACE_DEFAULT = "mosip";

	/** Config key: IDA WebSub event type name. */
	public static final String IDA_EVENT_TYPE_NAME = "id-repo-ida-event-type-name";

	/** Default IDA WebSub event type name when config is absent. */
	public static final String IDA_EVENT_TYPE_NAME_DEFAULT = "ida";

	/** Config key: skip credential requests for partners that already have credentials. */
	public static final String SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = "skip-requesting-existing-credentials-for-partners";

	/** Config key: WebSub hub publish URL. */
	public static final String WEB_SUB_PUBLISH_URL = "websub.publish.url";

	/** Config key: WebSub hub base URL. */
	public static final String WEB_SUB_HUB_URL = "websub.hub.url";

	// ---- Object store ----

	/** Config key: object store account name. */
	public static final String OBJECT_STORE_ACCOUNT_NAME = "mosip.idrepo.objectstore.account-name";

	/** Config key: object store bucket name. */
	public static final String OBJECT_STORE_BUCKET_NAME = "mosip.idrepo.objectstore.bucket-name";

	/** Config key: object store adapter implementation name. */
	public static final String OBJECT_STORE_ADAPTER_NAME = "mosip.idrepo.objectstore.adapter-name";

	/** Config key: prepend partner cert thumbprint to shared credential data. */
	public static final String PREPEND_THUMPRINT_STATUS = "mosip.credential.service.share.prependThumbprint";

	/** Config key: dummy OLV partner ID for non-production testing. */
	public static final String IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID = "idrepo-dummy-online-verification-partner-id";

	/** Partner type constant for online verification partners. */
	public static final String MOSIP_OLV_PARTNER = "MOSIP_OLV_PARTNER";

	// ---- HikariCP pool sizing (per pod / per JVM) ----

	/** Config key: identity DB max pool size. */
	public static final String IDENTITY_POOL_MAX = "mosip.idrepo.identity.pool.max";

	/** Default identity max pool. */
	public static final int IDENTITY_POOL_MAX_DEFAULT = 12;

	/** Config key: identity DB minimum idle connections. */
	public static final String IDENTITY_POOL_MIN = "mosip.idrepo.identity.pool.min";

	/** Default identity min idle. */
	public static final int IDENTITY_POOL_MIN_DEFAULT = 2;

	/** Config key: identity pool connection acquire timeout (ms). */
	public static final String IDENTITY_POOL_TIMEOUT_MS = "mosip.idrepo.identity.pool.timeout-ms";

	/** Config key: VID DB max pool size. */
	public static final String VID_POOL_MAX = "mosip.idrepo.vid.pool.max";

	/** Default VID max pool. */
	public static final int VID_POOL_MAX_DEFAULT = 6;

	/** Config key: VID DB minimum idle connections. */
	public static final String VID_POOL_MIN = "mosip.idrepo.vid.pool.min";

	/** Default VID min idle. */
	public static final int VID_POOL_MIN_DEFAULT = 1;

	/** Config key: VID pool connection acquire timeout (ms). */
	public static final String VID_POOL_TIMEOUT_MS = "mosip.idrepo.vid.pool.timeout-ms";

	/** Config key: credential DB max pool size. */
	public static final String CREDENTIAL_POOL_MAX = "mosip.idrepo.credential.pool.max";

	/** Default credential max pool. */
	public static final int CREDENTIAL_POOL_MAX_DEFAULT = 10;

	/** Config key: credential DB minimum idle connections. */
	public static final String CREDENTIAL_POOL_MIN = "mosip.idrepo.credential.pool.min";

	/** Default credential min idle. */
	public static final int CREDENTIAL_POOL_MIN_DEFAULT = 2;

	/** Config key: credential pool connection acquire timeout (ms). */
	public static final String CREDENTIAL_POOL_TIMEOUT_MS = "mosip.idrepo.credential.pool.timeout-ms";

	/** Config key: idle connection eviction for all Hikari pools (ms). */
	public static final String POOL_IDLE_TIMEOUT_MS = "mosip.idrepo.datasource.pool.idle-timeout-ms";

	/** Default idle timeout (10 minutes). */
	public static final long POOL_IDLE_TIMEOUT_DEFAULT_MS = 600_000L;

	/** Config key: max connection lifetime for all Hikari pools (ms). */
	public static final String POOL_MAX_LIFETIME_MS = "mosip.idrepo.datasource.pool.max-lifetime-ms";

	/** Default max lifetime (30 minutes; keep below PostgreSQL server timeout). */
	public static final long POOL_MAX_LIFETIME_DEFAULT_MS = 1_800_000L;

	/** Default connection acquire timeout (ms). */
	public static final long POOL_CONNECTION_TIMEOUT_DEFAULT_MS = 30_000L;

	/** Config key: cryptomanager reference ID for UIN encryption. */
	public static final String UIN_REFID = "mosip.idrepo.crypto.refId.uin";

	/** Config key: cryptomanager reference ID for UIN demographic data. */
	public static final String UIN_DATA_REFID = "mosip.idrepo.crypto.refId.uin-data";

	/** Config key: cryptomanager reference ID for biometric documents. */
	public static final String BIO_DATA_REFID = "mosip.idrepo.crypto.refId.bio-doc-data";

	/** Config key: cryptomanager reference ID for demographic documents. */
	public static final String DEMO_DATA_REFID = "mosip.idrepo.crypto.refId.demo-doc-data";

	/** Config key: WebSub topic for VID credential update events. */
	public static final String VID_EVENT_TOPIC = "mosip.idrepo.websub.vid-credential-update.topic";

	/** Config key: WebSub secret for VID credential update subscription. */
	public static final String VID_EVENT_SECRET = "mosip.idrepo.websub.vid-credential-update.secret";

	/** Config key: callback URL for VID credential update WebSub events. */
	public static final String VID_EVENT_CALLBACK_URL = "mosip.idrepo.websub.vid-credential-update.callback-url";

	/** Config key: WebSub topic for credential status update events. */
	public static final String CREDENTIAL_STATUS_UPDATE_TOPIC = "mosip.idrepo.websub.credential-status-update.topic";

	/**
	 * Config key: when {@code true}, audit-manager REST posts run off the caller thread.
	 * Default {@code true} for lower identity/credential API latency; set {@code false} for sync audits.
	 */
	public static final String AUDIT_ASYNC_ENABLED = "mosip.idrepo.audit.async-enabled";

	/**
	 * Config key: when {@code true}, non-critical WebSub publishes (credential issued, identity events)
	 * run off the caller thread. Set {@code false} to publish synchronously (failures propagate).
	 */
	public static final String WEBSUB_PUBLISH_ASYNC_ENABLED = "mosip.idrepo.websub.publish.async-enabled";

	// ---- Pipeline / identity runtime config keys ----

	/** Config key: disable VID support in credential flows. */
	public static final String VID_DISABLE_SUPPORT = "mosip.idrepo.vid.disable-support";

	/** Config key: disable UIN-based credential requests. */
	public static final String DISABLE_UIN_BASED_CREDENTIAL_REQUEST = "mosip.idrepo.identity.disable-uin-based-credential-request";

	/** Config key: credential status manager batch page size. */
	public static final String CREDENTIAL_REQUEST_BATCH_PAGE_SIZE = "mosip.idrepo.credential.request.batch.page.size";

	/** Config key: identity request timestamp max deviation in seconds. */
	public static final String MAX_REQUEST_TIME_DEVIATION_SECONDS = "mosip.idrepo.identity.max-request-time-deviation-seconds";

	/** Config key: enable convention-based ID for credential requests. */
	public static final String ENABLE_CONVENTION_BASED_ID = "mosip.idrepo.credential.request.enable-convention-based-id";

	/** Config key: trim whitespace on identity update. */
	public static final String UPDATE_IDENTITY_TRIM_WHITESPACES = "mosip.idrepo.update-identity.trim-whitespaces";

	/** Config key: identity fields replaced wholesale on update (SpEL map). */
	public static final String UPDATE_IDENTITY_FIELDS_TO_REPLACE = "mosip.idrepo.update-identity.fields-to-replace";

	/** Config key: handle field postfix mapping (SpEL map). */
	public static final String IDENTITY_FIELDID_HANDLE_POSTFIX_MAPPING = "mosip.identity.fieldid.handle-postfix.mapping";

	/** Config key: mandatory attributes for new registration validation. */
	public static final String KERNEL_MANDATORY_ATTRIBUTES_NEW_REGISTRATION = "mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.new-registration";

	/** Config key: mandatory attributes for UIN update validation. */
	public static final String KERNEL_MANDATORY_ATTRIBUTES_UPDATE_UIN = "mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.update-uin";

	/** Config key: allow force-merge on draft identity create. */
	public static final String CREATE_IDENTITY_ENABLE_FORCE_MERGE = "mosip.idrepo.create-identity.enable-force-merge";

	/** Config key: template extraction executor core pool size. */
	public static final String EXTRACT_TEMPLATE_CORE_POOL_SIZE = "mosip.idrepo.extract.template.core-pool-size";

	/** Config key: template extraction executor max pool size. */
	public static final String EXTRACT_TEMPLATE_MAX_POOL_SIZE = "mosip.idrepo.extract.template.max-pool-size";

	/** Config key: template extraction executor queue capacity. */
	public static final String EXTRACT_TEMPLATE_QUEUE_CAPACITY = "mosip.idrepo.extract.template.queue-capacity";

	/** Config key: async thread-pool monitor interval in milliseconds. */
	public static final String MONITOR_THREAD_QUEUE_IN_MS = "mosip.idrepo.monitor-thread-queue-in-ms";

	/** Config key: allowed auth types for identity requests. */
	public static final String AUTH_TYPES_ALLOWED = "auth.types.allowed";

	/** Config key: identity DB JDBC URL (salt-generator job). */
	public static final String IDENTITY_DB_URL = "mosip.idrepo.identity.db.url";

	/** Config key: identity DB username (salt-generator job). */
	public static final String IDENTITY_DB_USERNAME = "mosip.idrepo.identity.db.username";

	/** Config key: identity DB password (salt-generator job). */
	public static final String IDENTITY_DB_PASSWORD = "mosip.idrepo.identity.db.password";

	/** Config key: identity DB driver class name (salt-generator job). */
	public static final String IDENTITY_DB_DRIVER_CLASS_NAME = "mosip.idrepo.identity.db.driverClassName";

	/** Config key: fetch-identity type for credential issuance. */
	public static final String FETCH_IDENTITY_TYPE = "mosip.credential.service.fetch-identity.type";

	/** Default fetch-identity type when config is absent. */
	public static final String FETCH_IDENTITY_TYPE_DEFAULT = "all";

	/** Config key: mask function identity attributes (VID). */
	public static final String MASK_FUNCTION_IDENTITY_ATTRIBUTES = "mosip.mask.function.identityAttributes";

	/** Default VID create API id when config is absent. */
	public static final String VID_CREATE_ID_DEFAULT = "mosip.vid.create";

	/** Default VID application version when config is absent. */
	public static final String APPLICATION_VERSION_VID_DEFAULT = "v1";

	// ---- Credential store / credreq config keys ----

	/** Config key: credreq WebSub subscription retry count. */
	public static final String CREDENTIAL_SUBSCRIPTION_RETRY_COUNT = "retry-count";

	/** Config key: credreq WebSub resubscription delay in seconds. */
	public static final String CREDENTIAL_RESUBSCRIPTION_DELAY_SECS = "resubscription-delay-secs";

	/** Config key: credreq WebSub initial subscription delay in seconds. */
	public static final String CREDENTIAL_SUBSCRIPTION_DELAY_SECS = "subscription-delay-secs";

	/** Config key: credreq HTTP client max connections per host. */
	public static final String CREDREQ_HTTPCLIENT_MAX_PER_HOST = "idrepo.default.processor.httpclient.connections.max.per.host";

	/** Config key: credreq HTTP client max total connections. */
	public static final String CREDREQ_HTTPCLIENT_MAX_TOTAL = "idrepo.default.processor.httpclient.connections.max";

	/** Config key: credreq WebSub secret placeholder. */
	public static final String CREDREQ_WEBSUB_SECRET = "WEBSUBSECRET";

	/** Config key: credreq WebSub callback URL placeholder. */
	public static final String CREDREQ_CALLBACK_URL = "CALLBACKURL";

	// ---- Credential store config keys ----

	/** Config key: Spring Cloud Config server file storage URI. */
	public static final String CONFIG_SERVER_FILE_STORAGE_URI = "config.server.file.storage.uri";

	/** Config key: MVEL policy file name for credential issuance. */
	public static final String CREDENTIAL_SERVICE_MVEL_FILE = "credential.service.mvel.file";

	/** Config key: credential type mapping file name. */
	public static final String CREDENTIAL_SERVICE_CREDENTIALTYPE_FILE = "credential.service.credentialtype.file";

	/** Config key: credential service cryptomanager application id. */
	public static final String CREDENTIAL_SERVICE_APPLICATION_ID = "credential.service.application.id";

	/** Default credential service application id when config is absent. */
	public static final String CREDENTIAL_SERVICE_APPLICATION_ID_DEFAULT = "PARTNER";

	/** Config key: date-of-birth format for credential payloads. */
	public static final String CREDENTIAL_SERVICE_DOB_FORMAT = "credential.service.dob.format";

	/** Config key: default VID type in credential payloads. */
	public static final String CREDENTIAL_SERVICE_DEFAULT_VID_TYPE = "credential.service.default.vid.type";

	/** Default VID type for credentials when config is absent. */
	public static final String CREDENTIAL_SERVICE_DEFAULT_VID_TYPE_DEFAULT = "PERPETUAL";

	/** Config key: biometric conversion request version. */
	public static final String CREDENTIAL_SERVICE_CONVERT_REQUEST_VERSION = "credential.service.convert.request.version";

	/** Default biometric conversion version when config is absent. */
	public static final String CREDENTIAL_SERVICE_CONVERT_REQUEST_VERSION_DEFAULT = "ISO19794_5_2011";

	/** Config key: verifiable credential JSON-LD context URI. */
	public static final String CREDENTIAL_SERVICE_VERCRED_CONTEXT_URI = "mosip.credential.service.vercred.context.uri";

	/** Config key: verifiable credential subject id URL. */
	public static final String CREDENTIAL_SERVICE_VERCRED_ID_URL = "mosip.credential.service.vercred.id.url";

	/** Config key: verifiable credential issuer URL. */
	public static final String CREDENTIAL_SERVICE_VERCRED_ISSUER_URL = "mosip.credential.service.vercred.issuer.url";

	/** Config key: verifiable credential proof purpose. */
	public static final String CREDENTIAL_SERVICE_VERCRED_PROOF_PURPOSE = "mosip.credential.service.vercred.proof.purpose";

	/** Config key: verifiable credential proof type. */
	public static final String CREDENTIAL_SERVICE_VERCRED_PROOF_TYPE = "mosip.credential.service.vercred.proof.type";

	/** Config key: verifiable credential proof verification method. */
	public static final String CREDENTIAL_SERVICE_VERCRED_PROOF_VERIFICATION_METHOD = "mosip.credential.service.vercred.proof.verificationmethod";

	/** Config key: verifiable credential JSON-LD context URL map (SpEL). */
	public static final String CREDENTIAL_SERVICE_VERCRED_CONTEXT_URL_MAP = "mosip.credential.service.vercred.context.url.map";

	/** Config key: comma-separated verifiable credential types. */
	public static final String CREDENTIAL_SERVICE_VERCRED_TYPES = "mosip.credential.service.vercred.types";

	/** Config key: datashare protocol. */
	public static final String DATA_SHARE_PROTOCOL = "mosip.data.share.protocol";

	/** Config key: datashare internal domain name. */
	public static final String DATA_SHARE_INTERNAL_DOMAIN_NAME = "mosip.data.share.internal.domain.name";

	// ---- Kernel token ID config keys ----

	/** Config key: kernel token ID UIN salt. */
	public static final String KERNEL_TOKENID_UIN_SALT = "mosip.kernel.tokenid.uin.salt";

	/** Config key: kernel token ID length. */
	public static final String KERNEL_TOKENID_LENGTH = "mosip.kernel.tokenid.length";

	/** Config key: kernel token ID partner-code salt. */
	public static final String KERNEL_TOKENID_PARTNERCODE_SALT = "mosip.kernel.tokenid.partnercode.salt";

	// ---- Service REST controller config keys ----

	/** Config key: RID validation regex pattern. */
	public static final String RID_VALIDATION_PATTERN = "mosip.idrepo.rid.validation.pattern";

	/** Default RID validation pattern when config is absent. */
	public static final String RID_VALIDATION_PATTERN_DEFAULT = "\\d*";

	/** Config key: get-by-RID API request id. */
	public static final String RID_GET_ID = "mosip.idrepo.rid.get.id";

	/** Config key: get-by-RID API version. */
	public static final String RID_GET_VERSION = "mosip.idrepo.rid.get.version";

	/** Config key: ID/VID metadata API request id. */
	public static final String IDVID_METADATA_ID = "mosip.idrepo.idvid.metadata.id";

	/** Config key: ID/VID metadata API version. */
	public static final String IDVID_METADATA_VERSION = "mosip.idrepo.idvid.metadata.version";

	// ---- EnvUtil bootstrap config keys ----

	/** Config key: identity object validator date format. */
	public static final String IOV_DATE_FORMAT = "mosip.kernel.idobjectvalidator.date-format";

	/** Config key: comma-separated mandatory languages (first entry used for anonymous profile filter). */
	public static final String MANDATORY_LANGUAGES = "mosip.mandatory-languages";

	/** Config key: credential JSON schema URI. */
	public static final String CREDENTIAL_SERVICE_SCHEMA = "mosip.credential.service.credential.schema";

	/** Config key: credential-request service API request id. */
	public static final String CREDENTIAL_REQUEST_SERVICE_ID = "mosip.credential.request.service.id";

	/** Config key: credential-service API request id. */
	public static final String CREDENTIAL_SERVICE_SERVICE_ID = "mosip.credential.service.service.id";

	/** Config key: credential format identifier. */
	public static final String CREDENTIAL_SERVICE_FORMAT_ID = "mosip.credential.service.format.id";

	/** Config key: credential format issuer. */
	public static final String CREDENTIAL_SERVICE_FORMAT_ISSUER = "mosip.credential.service.format.issuer";

	/** Config key: credential-request service API version. */
	public static final String CREDENTIAL_REQUEST_SERVICE_VERSION = "mosip.credential.request.service.version";

	/** Config key: credential-service API version. */
	public static final String CREDENTIAL_SERVICE_SERVICE_VERSION = "mosip.credential.service.service.version";

	/** Config key: credential type name in issued credential JSON. */
	public static final String CREDENTIAL_SERVICE_TYPE_NAME = "mosip.credential.service.type.name";

	/** Config key: credential type XML namespace. */
	public static final String CREDENTIAL_SERVICE_TYPE_NAMESPACE = "mosip.credential.service.type.namespace";

	/** Config key: include certificate hash in issued credentials. */
	public static final String CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE_HASH = "mosip.credential.service.includeCertificateHash";

	/** Config key: include X.509 certificate in issued credentials. */
	public static final String CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE = "mosip.credential.service.includeCertificate";

	/** Config key: embed signed payload in issued credentials. */
	public static final String CREDENTIAL_SERVICE_INCLUDE_PAYLOAD = "mosip.credential.service.includePayload";

	/** Config key: credential-request Keycloak token issuer URL. */
	public static final String CREDENTIAL_REQUEST_TOKEN_ISSUER_URL = "credential.request.token.request.issuerUrl";

	/** Config key: credential-request Keycloak token API version. */
	public static final String CREDENTIAL_REQUEST_TOKEN_VERSION = "credential.request.token.request.version";

	/** Config key: credential-request Keycloak token request id. */
	public static final String CREDENTIAL_REQUEST_TOKEN_REQUEST_ID = "credential.request.token.request.id";

	/** Config key: credential-request Keycloak token client id. */
	public static final String CREDENTIAL_REQUEST_TOKEN_CLIENT_ID = "credential.request.token.request.clientId";

	/** Config key: credential-request Keycloak token client secret. */
	public static final String CREDENTIAL_REQUEST_TOKEN_SECRET_KEY = "credential.request.token.request.secretKey";

	/** Config key: credential-request Keycloak token application id. */
	public static final String CREDENTIAL_REQUEST_TOKEN_APP_ID = "credential.request.token.request.appid";

	/** Config key: credential-service Keycloak token request id. */
	public static final String CREDENTIAL_SERVICE_TOKEN_REQUEST_ID = "credential.service.token.request.id";

	/** Config key: credential-service Keycloak token issuer URL. */
	public static final String CREDENTIAL_SERVICE_TOKEN_ISSUER_URL = "credential.service.token.request.issuerUrl";

	/** Config key: credential-service Keycloak token API version. */
	public static final String CREDENTIAL_SERVICE_TOKEN_VERSION = "credential.service.token.request.version";

	/** Config key: credential-service Keycloak token client id. */
	public static final String CREDENTIAL_SERVICE_TOKEN_CLIENT_ID = "credential.service.token.request.clientId";

	/** Config key: credential-service Keycloak token client secret. */
	public static final String CREDENTIAL_SERVICE_TOKEN_SECRET_KEY = "credential.service.token.request.secretKey";

	/** Config key: credential-service Keycloak token application id. */
	public static final String CREDENTIAL_SERVICE_TOKEN_APP_ID = "credential.service.token.request.appid";

	/** Config key: active async thread pool size for identity executors. */
	public static final String ACTIVE_ASYNC_THREAD_COUNT = "mosip.idrepo.active-async-thread-count";

	/** Config key: async thread-pool queue depth threshold for monitoring. */
	public static final String MAX_THREAD_QUEUE_THRESHOLD = "mosip.idrepo.max-thread-queue-threshold";

	/** Spring property: application name (used to load service-specific token config). */
	public static final String SPRING_APPLICATION_NAME = "spring.application.name";

	/** Application name prefix for credential-request-generator standalone config. */
	public static final String CREDENTIAL_REQUEST_APP_NAME_PREFIX = "credential-request";

	/** Application name prefix for credential-service standalone config. */
	public static final String CREDENTIAL_SERVICE_APP_NAME_PREFIX = "credential-service";

	/** Config key: identity field mapping file on config server. */
	public static final String IDENTITY_MAPPING_JSON = "mosip.identity.mapping-file";

	// ---- Biometric extraction ----

	/** Identity JSON key for face extraction format. */
	public static final String FACE_EXTRACTION_FORMAT = "faceExtractionFormat";

	/** Identity JSON key for iris extraction format. */
	public static final String IRIS_EXTRACTION_FORMAT = "irisExtractionFormat";

	/** Identity JSON key for finger extraction format. */
	public static final String FINGER_EXTRACTION_FORMAT = "fingerExtractionFormat";

	/** Config key: cryptomanager reference ID for credential encryption. */
	public static final String CREDENTIAL_CRYPTO_REF_ID = "mosip.credential.request.crypto-ref-id";

	/** Unused instance value field (legacy; class uses static constants only). */
	private final String value;

	/** JSON field name for VID token in credential payloads. */
	public static final String TOKEN = "TOKEN";

	/** JSON field name for hashed identifier. */
	public static final String ID_HASH = "id_hash";

	/** Additional-data key: pre-encrypted individual id (handles) to avoid re-encryption in pipeline. */
	public static final String ENCRYPTED_ID = "encrypted_id";

	/** JSON field name for VID/token expiry timestamp. */
	public static final String EXPIRY_TIMESTAMP = "expiry_timestamp";

	/** JSON field name for VID transaction limit. */
	public static final String TRANSACTION_LIMIT = "transaction_limit";

	/** Application module label used in audit and logging. */
	public static final String ID_REPO = "ID_REPO";

	/** Dot separator for JSON path composition. */
	public static final String DOT = ".";

	/** Suffix appended to modality names for extraction-format query params. */
	public static final String EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX = "ExtractionFormat";

	/** UIN draft status literal. */
	public static final String DRAFT_STATUS = "DRAFT";

	/** Config key: default VID type created for draft identities. */
	public static final String DEFAULT_VID_TYPE = "mosip.idrepo.draft-vid.default-type-to-create";

	/** Config key: ID-schema field path for VID on create. */
	public static final String VID_CREATE_ID = "mosip.idrepo.vid.id.create";

	/** Config key: ID-schema field path for VID on update. */
	public static final String VID_UPDATE_ID = "mosip.idrepo.vid.id.update";

	/** Default salt key length when config is absent. */
	public static final int DEFAULT_SALT_KEY_LENGTH = 3;

	/** Config key: partner OLV cache refresh interval in milliseconds. */
	public static final String IDREPO_CACHE_UPDATE_INTERVAL = "mosip.idrepo.cache.update.interval.milli.seconds";

	/** Default partner cache refresh interval (2 hours). */
	public static final int CACHE_UPDATE_DEFAULT_INTERVAL = 7200000;

	/** Config key: TTL in minutes for PMS / WebSub / partner REST response caches. */
	public static final String PARTNER_CACHE_TTL_MINUTES = "mosip.idrepo.partner.cache.ttl.minutes";

	/** Default TTL for external-service caches (5 minutes). */
	public static final int PARTNER_CACHE_TTL_DEFAULT_MINUTES = 5;

	/** Config key: TTL in minutes for internal caches (DB salts, identity attribute hashing). */
	public static final String INTERNAL_CACHE_TTL_MINUTES = "mosip.idrepo.internal.cache.ttl.minutes";

	/** Default TTL for internal caches (30 minutes). */
	public static final int INTERNAL_CACHE_TTL_DEFAULT_MINUTES = 30;

	/** Spring cache region: PMS datashare policies per credential type and partner. */
	public static final String CACHE_DATASHARE_POLICIES = "DATASHARE_POLICIES";

	/** Spring cache region: PMS partner biometric extractor formats. */
	public static final String CACHE_PARTNER_EXTRACTOR_FORMATS = "PARTNER_EXTRACTOR_FORMATS";

	/** Spring cache region: active OLV partner IDs from PMS. */
	public static final String CACHE_ONLINE_VERIFICATION_PARTNERS = "Online_Verification_Partners";

	/** Spring cache region: registered WebSub topics per partner. */
	public static final String CACHE_WEBSUB_TOPICS = "topics";

	/** Cache regions backed by outbound REST / WebSub calls (short TTL). */
	public static final List<String> EXTERNAL_SERVICE_CACHE_NAMES = List.of(CACHE_DATASHARE_POLICIES,
			CACHE_PARTNER_EXTRACTOR_FORMATS, CACHE_ONLINE_VERIFICATION_PARTNERS, CACHE_WEBSUB_TOPICS);

	/** JSON response key for MOSIP error list. */
	public static final String ERRORS = "errors";

	/** Biometric modalities supported for template extraction. */
	public static final List<BiometricType> SUPPORTED_MODALITIES = List.of(FINGER, IRIS, FACE);

	// ---- Identity service method / JSON literals ----

	/** Service method name: retrieve biometric files. */
	public static final String GET_FILES = "getFiles";

	/** Service method name: update identity. */
	public static final String UPDATE_IDENTITY = "updateIdentity";

	/** Config/event key for MOSIP ID update notifications. */
	public static final String MOSIP_ID_UPDATE = "mosip.id.update";

	/** Service method name: add identity. */
	public static final String ADD_IDENTITY = "addIdentity";

	/** Service method name: retrieve identity. */
	public static final String RETRIEVE_IDENTITY = "retrieveIdentity";

	/** Identity JSON section name for biometrics. */
	public static final String BIOMETRICS = "Biometrics";

	/** Short label for biometric data category. */
	public static final String BIO = "bio";

	/** Short label for demographic data category. */
	public static final String DEMO = "demo";

	/** Spring bean / class label for identity service implementation. */
	public static final String ID_REPO_SERVICE_IMPL = "IdRepoServiceImpl";

	/** Access type: create-only. */
	public static final String CREATE = "create";

	/** Access type: read-only. */
	public static final String READ = "read";

	/** Access type: all operations. */
	public static final String ALL = "all";

	/** Identity JSON section name for demographics. */
	public static final String DEMOGRAPHICS = "Demographics";

	/** Error/log message when no draft record exists. */
	public static final String DRAFT_RECORD_NOT_FOUND = "DRAFT RECORD NOT FOUND";

	/** Identity JSON field for eKYC-verified attributes. */
	public static final String VERIFIED_ATTRIBUTES = "verifiedAttributes";

	/** Service method name: get draft. */
	public static final String GET_DRAFT = "getDraft";

	/** Service method name: discard draft. */
	public static final String DISCARD_DRAFT = "discardDraft";

	/** Service method name: publish draft. */
	public static final String PUBLISH_DRAFT = "publishDraft";

	/** UIN status after draft publish. */
	public static final String DRAFTED = "DRAFTED";

	/** Service method name: update draft. */
	public static final String UPDATE_DRAFT = "UpdateDraft";

	/** Service method name: generate UIN for draft. */
	public static final String GENERATE_UIN = "generateUin";

	/** Service method name: create draft. */
	public static final String CREATE_DRAFT = "createDraft";

	/** Spring bean / class label for draft service implementation. */
	public static final String ID_REPO_DRAFT_SERVICE_IMPL = "IdRepoDraftServiceImpl";

	/** Separator between auth-type and status in composite keys. */
	public static final String AUTH_TYPE_SEPERATOR = "-";

	/** Config key: get-drafts API request ID. */
	public static final String GET_DRAFT_UIN_ID = "mosip.identity.get.drafts.id";

	/** Config key: get-drafts API version. */
	public static final String GET_DRAFT_UIN_VERSION = "mosip.identity.get.drafts.version";

	/** Config key: attributes excluded from identity retrieve responses. */
	public static final String EXCLUDED_ATTRIBUTE_LIST = "mosip.identity.get.excluded.attribute.list";

	/**
	 * Legacy private constructor; not used by static constants.
	 *
	 * @param value unused instance value
	 */
	private IdRepoConstants(String value) {
		this.value = value;
	}

	/**
	 * Returns the legacy instance value.
	 * <p>
	 * Prefer the public static constants on this class; instance usage is historical.
	 * </p>
	 *
	 * @return the instance value (legacy; prefer static constants)
	 */
	public String getValue() {
		return value;
	}
}
