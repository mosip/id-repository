package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_ASYNC_THREAD_COUNT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION_VID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_CRYPTO_REF_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_APP_NAME_PREFIX;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_SERVICE_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_SERVICE_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_TOKEN_APP_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_TOKEN_CLIENT_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_TOKEN_ISSUER_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_TOKEN_REQUEST_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_TOKEN_SECRET_KEY;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_REQUEST_TOKEN_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_APP_NAME_PREFIX;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_FORMAT_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_FORMAT_ISSUER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE_HASH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_INCLUDE_PAYLOAD;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_SCHEMA;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_SERVICE_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_SERVICE_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TOKEN_APP_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TOKEN_CLIENT_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TOKEN_ISSUER_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TOKEN_REQUEST_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TOKEN_SECRET_KEY;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TOKEN_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TYPE_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_SERVICE_TYPE_NAMESPACE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_ADJUSTMENT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_PATTERN;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEFAULT_SALT_KEY_LENGTH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEFAULT_VID_TYPE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDENTITY_DB_DRIVER_CLASS_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDENTITY_DB_PASSWORD;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDENTITY_DB_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDENTITY_DB_USERNAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDENTITY_MAPPING_JSON;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IOV_DATE_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MANDATORY_LANGUAGES;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MAX_THREAD_QUEUE_THRESHOLD;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MONITOR_THREAD_QUEUE_IN_MS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_KERNEL_IDREPO_JSON_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.PREPEND_THUMPRINT_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SALT_KEY_LENGTH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPRING_APPLICATION_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VERSION_PATTERN;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_CREATE_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_DRIVER_CLASS_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_PASSWORD;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DB_USERNAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DEACTIVATED;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_POLICY_FILE_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_POLICY_SCHEMA_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_UNLIMITED_TRANSACTION_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_UPDATE_ID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import lombok.Getter;
import lombok.Setter;

/**
 * Central accessor for ID Repository configuration loaded from the Spring {@link Environment}.
 *
 * <p>
 * At startup, {@link #init()} reads identity, credential, VID, and database properties from
 * Spring Cloud Config (and local overrides) and caches them in static fields so domain code can
 * call {@code EnvUtil.getXxx()} without injecting {@link Environment} everywhere. Property key
 * names are defined in {@link IdRepoConstants}; this class is the primary resolution point for
 * those keys at runtime.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Spring creates the {@code @Component} and injects {@link Environment}</li>
 *   <li>{@link #init()} runs via {@link PostConstruct}</li>
 *   <li>Credential-request / credential-service token secrets load only when
 *       {@link IdRepoConstants#SPRING_APPLICATION_NAME} matches the corresponding app-name
 *       prefix</li>
 *   <li>Callers use static getters (Lombok {@code @Getter} on static fields)</li>
 * </ol>
 *
 * <h2>Property groups cached</h2>
 * <ul>
 *   <li>Application identity — app id/name/version, datetime pattern, version regex</li>
 *   <li>Identity / VID DB — JDBC URL, user, password, driver (multi-PU wiring)</li>
 *   <li>Salt — {@link #getIdrepoSaltKeyLength()} for {@link SaltUtil} / {@link IdRepoSecurityManager}</li>
 *   <li>Credential issue format — schema, type namespace/name, include-certificate flags</li>
 *   <li>VID policy URLs, active/deactivated/unlimited statuses, draft VID type</li>
 *   <li>Async thread pool sizing / queue monitoring</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * String pattern = EnvUtil.getVersionPattern();
 * long skewAdj = EnvUtil.getDateTimeAdjustment();
 * Integer saltLen = EnvUtil.getIdrepoSaltKeyLength();
 *
 * // ad-hoc keys not cached as fields
 * String value = envUtil.getProperty(IdRepoConstants.SOME_KEY, "default");
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link BaseIdRepoValidator} — version pattern, datetime adjustment</li>
 *   <li>{@link IdRepoSecurityManager} — salt length, crypto app id, thumbprint flag</li>
 *   <li>Identity / VID / credential services — status literals, DB accessors, policy URLs</li>
 * </ul>
 *
 * <h2>Tests</h2>
 * <p>
 * {@link #merge(ConfigurableEnvironment)} overlays a parent environment. Static setters
 * (Lombok {@code @Setter}) allow unit tests to override individual cached values without a
 * full Spring context.
 * </p>
 *
 * @author Manoj SP
 * @see IdRepoConstants
 * @see BaseIdRepoValidator
 * @see IdRepoSecurityManager
 * @see SaltUtil
 */
@Component
public class EnvUtil {

	/** Spring {@link Environment} backing all property lookups. */
	@Autowired
	private Environment env;

	/** Date format pattern for identity-object validation ({@link IdRepoConstants#IOV_DATE_FORMAT}). */
	@Getter @Setter private static String iovDateFormat;
	/** First mandatory language used when filtering anonymous profile attributes. */
	@Getter @Setter private static String anonymousProfileFilterLanguage;
	/** MOSIP application ID for cryptomanager ({@link IdRepoConstants#APPLICATION_ID}). */
	@Getter @Setter private static String appId;
	/** Application display name ({@link IdRepoConstants#APPLICATION_NAME}). */
	@Getter @Setter private static String appName;
	/**
	 * Seconds added to UTC “now” before request-time skew checks
	 * ({@link IdRepoConstants#DATETIME_ADJUSTMENT}).
	 */
	@Getter @Setter private static Long dateTimeAdjustment = 0L;
	/** Regex pattern for API version validation ({@link IdRepoConstants#VERSION_PATTERN}). */
	@Getter @Setter private static String versionPattern;
	/** UTC datetime format pattern ({@link IdRepoConstants#DATETIME_PATTERN}). */
	@Getter @Setter private static String dateTimePattern;
	/** Credential JSON schema URI ({@link IdRepoConstants#CREDENTIAL_SERVICE_SCHEMA}). */
	@Getter @Setter private static String credServiceSchema;
	/** Credential format identifier ({@link IdRepoConstants#CREDENTIAL_SERVICE_FORMAT_ID}). */
	@Getter @Setter private static String credServiceFormatId;
	/** Credential-request service API ID ({@link IdRepoConstants#CREDENTIAL_REQUEST_SERVICE_ID}). */
	@Getter @Setter private static String credReqServiceId;
	/** Credential-service API ID ({@link IdRepoConstants#CREDENTIAL_SERVICE_SERVICE_ID}). */
	@Getter @Setter private static String credServiceId;
	/** Credential format issuer ({@link IdRepoConstants#CREDENTIAL_SERVICE_FORMAT_ISSUER}). */
	@Getter @Setter private static String credServiceFormatIssuer;
	/** Credential-request service API version ({@link IdRepoConstants#CREDENTIAL_REQUEST_SERVICE_VERSION}). */
	@Getter @Setter private static String credReqServiceVersion;
	/** Credential-service API version ({@link IdRepoConstants#CREDENTIAL_SERVICE_SERVICE_VERSION}). */
	@Getter @Setter private static String credServiceVersion;
	/** VID status value considered active ({@link IdRepoConstants#VID_ACTIVE_STATUS}). */
	@Getter @Setter private static String vidActiveStatus;
	/** UIN status value considered registered/active ({@link IdRepoConstants#ACTIVE_STATUS}). */
	@Getter @Setter private static String uinActiveStatus;
	/** Credential type name in issued credential JSON ({@link IdRepoConstants#CREDENTIAL_SERVICE_TYPE_NAME}). */
	@Getter @Setter private static String credServiceTypeName;
	/** Credential type XML namespace ({@link IdRepoConstants#CREDENTIAL_SERVICE_TYPE_NAMESPACE}). */
	@Getter @Setter private static String credServiceTypeNamespace;
	/** Cryptomanager reference ID for credential encryption ({@link IdRepoConstants#CREDENTIAL_CRYPTO_REF_ID}). */
	@Getter @Setter private static String credCryptoRefId;
	/** Whether issued credentials include certificate hash ({@link IdRepoConstants#CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE_HASH}). */
	@Getter @Setter private static Boolean credServiceIncludeCertificateHash;
	/** Whether issued credentials include the X.509 certificate ({@link IdRepoConstants#CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE}). */
	@Getter @Setter private static Boolean credServiceIncludeCertificate;
	/** Whether issued credentials embed the signed payload ({@link IdRepoConstants#CREDENTIAL_SERVICE_INCLUDE_PAYLOAD}). */
	@Getter @Setter private static Boolean credServiceIncludePayload;
	/** JSONPath expressions for identity field mapping ({@link IdRepoConstants#MOSIP_KERNEL_IDREPO_JSON_PATH}). */
	@Getter @Setter private static String uinJsonPath;
	/** Identity database JDBC URL ({@link IdRepoConstants#IDENTITY_DB_URL}). */
	@Getter @Setter private static String idrepoDBUrl;
	/** Identity database username ({@link IdRepoConstants#IDENTITY_DB_USERNAME}). */
	@Getter @Setter private static String idrepoDBUsername;
	/** Identity database password ({@link IdRepoConstants#IDENTITY_DB_PASSWORD}). */
	@Getter @Setter private static String idrepoDBPassword;
	/** Identity database JDBC driver class ({@link IdRepoConstants#IDENTITY_DB_DRIVER_CLASS_NAME}). */
	@Getter @Setter private static String idrepoDBDriverClassName;
	/** Identity module application version ({@link IdRepoConstants#APPLICATION_VERSION}). */
	@Getter @Setter private static String appVersion;
	/** Whether to prepend certificate thumbprint before encryption ({@link IdRepoConstants#PREPEND_THUMPRINT_STATUS}). */
	@Getter @Setter private static Boolean prependThumbprintStatus;
	/** Number of trailing ID digits used for salt-bucket routing ({@link IdRepoConstants#SALT_KEY_LENGTH}). */
	@Getter @Setter private static Integer idrepoSaltKeyLength;
	/** Keycloak issuer URL for credential-request outbound token ({@link IdRepoConstants#CREDENTIAL_REQUEST_TOKEN_ISSUER_URL}). */
	@Getter @Setter private static String credReqTokenIssuerUrl;
	/** Keycloak client ID for credential-request token ({@link IdRepoConstants#CREDENTIAL_REQUEST_TOKEN_CLIENT_ID}). */
	@Getter @Setter private static String credReqTokenClientId;
	/** Keycloak token version for credential-request ({@link IdRepoConstants#CREDENTIAL_REQUEST_TOKEN_VERSION}). */
	@Getter @Setter private static String credReqTokenVersion;
	/** Keycloak app ID for credential-request token ({@link IdRepoConstants#CREDENTIAL_REQUEST_TOKEN_APP_ID}). */
	@Getter @Setter private static String credReqTokenAppId;
	/** Keycloak client secret for credential-request token ({@link IdRepoConstants#CREDENTIAL_REQUEST_TOKEN_SECRET_KEY}). */
	@Getter @Setter private static String credReqTokenSecretKey;
	/** {@code true} when {@link IdRepoConstants#DEFAULT_VID_TYPE} is defined in config. */
	@Getter @Setter private static Boolean isDraftVidTypePresent;
	/** Draft VID type value ({@link IdRepoConstants#DEFAULT_VID_TYPE}). */
	@Getter @Setter private static String draftVidType;
	/** API ID for VID create requests ({@link IdRepoConstants#VID_CREATE_ID}). */
	@Getter @Setter private static String createVidId;
	/** VID module application version ({@link IdRepoConstants#APPLICATION_VERSION_VID}). */
	@Getter @Setter private static String vidAppVersion;
	/** API ID for VID update requests ({@link IdRepoConstants#VID_UPDATE_ID}). */
	@Getter @Setter private static String updatedVidId;
	/** Config-server URL for VID policy JSON ({@link IdRepoConstants#VID_POLICY_FILE_URL}). */
	@Getter @Setter private static String vidPolicyFileUrl;
	/** Config-server URL for VID policy JSON schema ({@link IdRepoConstants#VID_POLICY_SCHEMA_URL}). */
	@Getter @Setter private static String vidPolicySchemaUrl;
	/** VID (idmap) database JDBC URL ({@link IdRepoConstants#VID_DB_URL}). */
	@Getter @Setter private static String vidDBUrl;
	/** VID database username ({@link IdRepoConstants#VID_DB_USERNAME}). */
	@Getter @Setter private static String vidDBUsername;
	/** VID database password ({@link IdRepoConstants#VID_DB_PASSWORD}). */
	@Getter @Setter private static String vidDBPassword;
	/** VID database JDBC driver class ({@link IdRepoConstants#VID_DB_DRIVER_CLASS_NAME}). */
	@Getter @Setter private static String vidDBDriverClassName;
	/** Timezone for identity datetime fields ({@link IdRepoConstants#DATETIME_TIMEZONE}). */
	@Getter @Setter private static String datetimeTimezone;
	/** VID status value for deactivated VIDs ({@link IdRepoConstants#VID_DEACTIVATED}). */
	@Getter @Setter private static String vidDeactivatedStatus;
	/** VID status allowing unlimited authentication transactions ({@link IdRepoConstants#VID_UNLIMITED_TRANSACTION_STATUS}). */
	@Getter @Setter private static String vidUnlimitedTxnStatus;
	/** Token request ID for credential-request service ({@link IdRepoConstants#CREDENTIAL_REQUEST_TOKEN_REQUEST_ID}). */
	@Getter @Setter private static String credReqTokenRequestId;
	/** Token request ID for credential-service ({@link IdRepoConstants#CREDENTIAL_SERVICE_TOKEN_REQUEST_ID}). */
	@Getter @Setter private static String credServiceTokenRequestId;
	/** Keycloak issuer URL for credential-service outbound token ({@link IdRepoConstants#CREDENTIAL_SERVICE_TOKEN_ISSUER_URL}). */
	@Getter @Setter private static String credServiceTokenRequestIssuerUrl;
	/** Keycloak client ID for credential-service token ({@link IdRepoConstants#CREDENTIAL_SERVICE_TOKEN_CLIENT_ID}). */
	@Getter @Setter private static String credServiceTokenRequestClientId;
	/** Keycloak token version for credential-service ({@link IdRepoConstants#CREDENTIAL_SERVICE_TOKEN_VERSION}). */
	@Getter @Setter private static String credServiceTokenRequestVersion;
	/** Keycloak app ID for credential-service token ({@link IdRepoConstants#CREDENTIAL_SERVICE_TOKEN_APP_ID}). */
	@Getter @Setter private static String credServiceTokenRequestAppId;
	/** Keycloak client secret for credential-service token ({@link IdRepoConstants#CREDENTIAL_SERVICE_TOKEN_SECRET_KEY}). */
	@Getter @Setter private static String credServiceTokenRequestSecretKey;
	/** Active thread count for async credential processing ({@link IdRepoConstants#ACTIVE_ASYNC_THREAD_COUNT}). */
	@Getter @Setter private static Integer activeAsyncThreadCount;
	/** Interval in ms for monitoring async thread queue depth ({@link IdRepoConstants#MONITOR_THREAD_QUEUE_IN_MS}). */
	@Getter @Setter private static String monitorAsyncThreadQueue;
	/** Maximum async thread queue depth before back-pressure ({@link IdRepoConstants#MAX_THREAD_QUEUE_THRESHOLD}). */
	@Getter @Setter private static Integer asyncThreadQueueThreshold;
	/** Config-server URL for identity mapping JSON ({@link IdRepoConstants#IDENTITY_MAPPING_JSON}). */
	@Getter @Setter private static String identityMappingJsonUrl;

	/**
	 * Returns the raw property value for {@code key}, or {@code null} if undefined.
	 *
	 * @param key Spring property key (typically a constant from {@link IdRepoConstants})
	 * @return property value, or {@code null} when not set
	 */
	public String getProperty(String key) {
		return env.getProperty(key);
	}

	/**
	 * Returns the property value coerced to {@code targetType}, using {@code defaultValue}
	 * when the key is absent.
	 *
	 * @param <T>          desired property type
	 * @param key          Spring property key
	 * @param targetType   class to convert the value into
	 * @param defaultValue value returned when the property is not defined
	 * @return resolved property value or {@code defaultValue}
	 */
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return env.getProperty(key, targetType, defaultValue);
	}

	/**
	 * Returns the property value as a string, using {@code defaultValue} when absent.
	 *
	 * @param key          Spring property key
	 * @param defaultValue value returned when the property is not defined
	 * @return resolved property value or {@code defaultValue}
	 */
	public String getProperty(String key, String defaultValue) {
		return env.getProperty(key, defaultValue);
	}

	/**
	 * Returns the property value coerced to {@code targetType}, or {@code null} when absent.
	 *
	 * @param <T>        desired property type
	 * @param key        Spring property key
	 * @param targetType class to convert the value into
	 * @return resolved property value, or {@code null} when not set
	 */
	public <T> T getProperty(String key, Class<T> targetType) {
		return env.getProperty(key, targetType);
	}

	/**
	 * Merges property sources from {@code parent} into this bean's {@link Environment}.
	 * <p>
	 * Used in tests and bootstrap scenarios to overlay a parent
	 * {@link ConfigurableEnvironment} without replacing the entire Spring context. The
	 * underlying {@code env} must be a {@link ConfigurableEnvironment}.
	 * </p>
	 *
	 * @param parent environment whose property sources are merged into the current environment
	 */
	public void merge(ConfigurableEnvironment parent) {
		((ConfigurableEnvironment) env).merge(parent);
	}

	/**
	 * Indicates whether a property is defined in the current {@link Environment}.
	 * <p>
	 * Returns {@code true} even when the value is an empty string, as long as the key exists.
	 * </p>
	 *
	 * @param key Spring property key to check
	 * @return {@code true} if the property exists; {@code false} otherwise
	 */
	public boolean containsProperty(String key) {
		return env.containsProperty(key);
	}

	/**
	 * Loads all ID Repository configuration into static fields after dependency injection.
	 * <p>
	 * Invoked automatically by Spring via {@link PostConstruct}. Order:
	 * </p>
	 * <ol>
	 *   <li>{@link #initCredentialRequestGeneratorServiceProperties()}</li>
	 *   <li>{@link #initCredentialServiceProperties()}</li>
	 *   <li>Common identity / VID / credential / DB / async fields</li>
	 * </ol>
	 * <p>
	 * Salt length defaults to {@link IdRepoConstants#DEFAULT_SALT_KEY_LENGTH} when
	 * {@link IdRepoConstants#SALT_KEY_LENGTH} is unset. Mandatory languages take the first
	 * comma-separated entry for anonymous-profile filtering.
	 * </p>
	 *
	 * @see IdRepoConstants
	 */
	@PostConstruct
	public void init() {
		this.initCredentialRequestGeneratorServiceProperties();
		this.initCredentialServiceProperties();
		iovDateFormat = this.getProperty(IOV_DATE_FORMAT);
		anonymousProfileFilterLanguage = this.getProperty(MANDATORY_LANGUAGES, "").split(",")[0];
		appId = this.getProperty(IdRepoConstants.APPLICATION_ID);
		appName = this.getProperty(IdRepoConstants.APPLICATION_NAME);
		appVersion = this.getProperty(APPLICATION_VERSION);
		dateTimeAdjustment = this.getProperty(DATETIME_ADJUSTMENT, Long.class, 0l);
		versionPattern = this.getProperty(VERSION_PATTERN);
		dateTimePattern = this.getProperty(DATETIME_PATTERN);
		credServiceSchema = this.getProperty(CREDENTIAL_SERVICE_SCHEMA);
		credReqServiceId = this.getProperty(CREDENTIAL_REQUEST_SERVICE_ID);
		credServiceId = this.getProperty(CREDENTIAL_SERVICE_SERVICE_ID);
		credServiceFormatId = this.getProperty(CREDENTIAL_SERVICE_FORMAT_ID);
		credServiceFormatIssuer = this.getProperty(CREDENTIAL_SERVICE_FORMAT_ISSUER);
		credReqServiceVersion = this.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION);
		credServiceVersion = this.getProperty(CREDENTIAL_SERVICE_SERVICE_VERSION);
		vidActiveStatus = this.getProperty(VID_ACTIVE_STATUS);
		uinActiveStatus = this.getProperty(ACTIVE_STATUS);
		credServiceTypeName = this.getProperty(CREDENTIAL_SERVICE_TYPE_NAME);
		credServiceTypeNamespace = this.getProperty(CREDENTIAL_SERVICE_TYPE_NAMESPACE);
		credCryptoRefId = this.getProperty(CREDENTIAL_CRYPTO_REF_ID);
		credServiceIncludeCertificateHash = this.getProperty(CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE_HASH, Boolean.class);
		credServiceIncludeCertificate = this.getProperty(CREDENTIAL_SERVICE_INCLUDE_CERTIFICATE, Boolean.class);
		credServiceIncludePayload = this.getProperty(CREDENTIAL_SERVICE_INCLUDE_PAYLOAD, Boolean.class);
		uinJsonPath = this.getProperty(MOSIP_KERNEL_IDREPO_JSON_PATH, "");
		idrepoDBUrl = this.getProperty(IDENTITY_DB_URL);
		idrepoDBUsername = this.getProperty(IDENTITY_DB_USERNAME);
		idrepoDBPassword = this.getProperty(IDENTITY_DB_PASSWORD);
		idrepoDBDriverClassName = this.getProperty(IDENTITY_DB_DRIVER_CLASS_NAME);
		prependThumbprintStatus = this.getProperty(PREPEND_THUMPRINT_STATUS, Boolean.class);
		idrepoSaltKeyLength = this.getProperty(SALT_KEY_LENGTH, Integer.class, DEFAULT_SALT_KEY_LENGTH);
		credReqTokenIssuerUrl = this.getProperty(CREDENTIAL_REQUEST_TOKEN_ISSUER_URL);
		credReqTokenVersion = this.getProperty(CREDENTIAL_REQUEST_TOKEN_VERSION);
		isDraftVidTypePresent = this.containsProperty(DEFAULT_VID_TYPE);
		draftVidType = this.getProperty(DEFAULT_VID_TYPE);
		createVidId = this.getProperty(VID_CREATE_ID);
		vidAppVersion = this.getProperty(APPLICATION_VERSION_VID);
		updatedVidId = this.getProperty(VID_UPDATE_ID);
		vidPolicyFileUrl = this.getProperty(VID_POLICY_FILE_URL);
		vidPolicySchemaUrl = this.getProperty(VID_POLICY_SCHEMA_URL);
		vidDBUrl = this.getProperty(VID_DB_URL);
		vidDBUsername = this.getProperty(VID_DB_USERNAME);
		vidDBPassword = this.getProperty(VID_DB_PASSWORD);
		vidDBDriverClassName = this.getProperty(VID_DB_DRIVER_CLASS_NAME);
		datetimeTimezone = this.getProperty(IdRepoConstants.DATETIME_TIMEZONE);
		vidDeactivatedStatus = this.getProperty(VID_DEACTIVATED);
		vidUnlimitedTxnStatus = this.getProperty(VID_UNLIMITED_TRANSACTION_STATUS);
		credReqTokenRequestId = this.getProperty(CREDENTIAL_REQUEST_TOKEN_REQUEST_ID);
		credServiceTokenRequestId = this.getProperty(CREDENTIAL_SERVICE_TOKEN_REQUEST_ID);
		credServiceTokenRequestIssuerUrl = this.getProperty(CREDENTIAL_SERVICE_TOKEN_ISSUER_URL);
		credServiceTokenRequestVersion = this.getProperty(CREDENTIAL_SERVICE_TOKEN_VERSION);
		activeAsyncThreadCount = this.getProperty(ACTIVE_ASYNC_THREAD_COUNT, Integer.class);
		monitorAsyncThreadQueue = this.getProperty(MONITOR_THREAD_QUEUE_IN_MS);
		asyncThreadQueueThreshold = this.getProperty(MAX_THREAD_QUEUE_THRESHOLD, Integer.class, 0);
		identityMappingJsonUrl = this.getProperty(IDENTITY_MAPPING_JSON);
	}

	/**
	 * Loads credential-request-generator Keycloak token client id, secret, and app id.
	 * <p>
	 * Runs only when {@link IdRepoConstants#SPRING_APPLICATION_NAME} starts with
	 * {@link IdRepoConstants#CREDENTIAL_REQUEST_APP_NAME_PREFIX}, so secrets are not
	 * required in processes that never act as the credential-request client.
	 * </p>
	 */
	private void initCredentialRequestGeneratorServiceProperties() {
		if (env.getProperty(SPRING_APPLICATION_NAME, "").startsWith(CREDENTIAL_REQUEST_APP_NAME_PREFIX)) {
			credReqTokenClientId = this.getProperty(CREDENTIAL_REQUEST_TOKEN_CLIENT_ID);
			credReqTokenSecretKey = this.getProperty(CREDENTIAL_REQUEST_TOKEN_SECRET_KEY);
			credReqTokenAppId = this.getProperty(CREDENTIAL_REQUEST_TOKEN_APP_ID);
		}
	}

	/**
	 * Loads credential-service Keycloak token client id, secret, and app id.
	 * <p>
	 * Runs only when {@link IdRepoConstants#SPRING_APPLICATION_NAME} starts with
	 * {@link IdRepoConstants#CREDENTIAL_SERVICE_APP_NAME_PREFIX}.
	 * </p>
	 */
	private void initCredentialServiceProperties() {
		if (env.getProperty(SPRING_APPLICATION_NAME, "").startsWith(CREDENTIAL_SERVICE_APP_NAME_PREFIX)) {
			credServiceTokenRequestClientId = this.getProperty(CREDENTIAL_SERVICE_TOKEN_CLIENT_ID);
			credServiceTokenRequestSecretKey = this.getProperty(CREDENTIAL_SERVICE_TOKEN_SECRET_KEY);
			credServiceTokenRequestAppId = this.getProperty(CREDENTIAL_SERVICE_TOKEN_APP_ID);
		}
	}
}