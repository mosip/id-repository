package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION_VID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_ADJUSTMENT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_PATTERN;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEFAULT_SALT_KEY_LENGTH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEFAULT_VID_TYPE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDENTITY_MAPPING_JSON;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_KERNEL_IDREPO_JSON_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.PREPEND_THUMPRINT_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SALT_KEY_LENGTH;
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Manoj SP
 *
 */
@Component
public class EnvUtil {

	@Autowired
	private Environment env;

	@Getter @Setter private static String iovDateFormat;
	@Getter @Setter private static String anonymousProfileFilterLanguage;
	@Getter @Setter private static String appId;
	@Getter @Setter private static String appName;
	@Getter @Setter private static Long dateTimeAdjustment;
	@Getter @Setter private static String versionPattern;
	@Getter @Setter private static String dateTimePattern;
	@Getter @Setter private static String credServiceSchema;
	@Getter @Setter private static String credServiceFormatId;
	@Getter @Setter private static String credReqServiceId;
	@Getter @Setter private static String credServiceId;
	@Getter @Setter private static String credServiceFormatIssuer;
	@Getter @Setter private static String credReqServiceVersion;
	@Getter @Setter private static String credServiceVersion;
	@Getter @Setter private static String vidActiveStatus;
	@Getter @Setter private static String uinActiveStatus;
	@Getter @Setter private static String credServiceTypeName;
	@Getter @Setter private static String credServiceTypeNamespace;
	@Getter @Setter private static String credCryptoRefId;
	@Getter @Setter private static Boolean credServiceIncludeCertificateHash;
	@Getter @Setter private static Boolean credServiceIncludeCertificate;
	@Getter @Setter private static Boolean credServiceIncludePayload;
	@Getter @Setter private static String uinJsonPath;
	@Getter @Setter private static String idrepoDBUrl;
	@Getter @Setter private static String idrepoDBUsername;
	@Getter @Setter private static String idrepoDBPassword;
	@Getter @Setter private static String idrepoDBDriverClassName;
	@Getter @Setter private static String appVersion;
	@Getter @Setter private static Boolean prependThumbprintStatus;
	@Getter @Setter private static Integer idrepoSaltKeyLength;
	@Getter @Setter private static String credReqTokenIssuerUrl;
	@Getter @Setter private static String credReqTokenClientId;
	@Getter @Setter private static String credReqTokenVersion;
	@Getter @Setter private static String credReqTokenAppId;
	@Getter @Setter private static String credReqTokenSecretKey;
	@Getter @Setter private static Boolean isDraftVidTypePresent;
	@Getter @Setter private static String draftVidType;
	@Getter @Setter private static String createVidId;
	@Getter @Setter private static String vidAppVersion;
	@Getter @Setter private static String updatedVidId;
	@Getter @Setter private static String vidPolicyFileUrl;
	@Getter @Setter private static String vidPolicySchemaUrl;
	@Getter @Setter private static String vidDBUrl;
	@Getter @Setter private static String vidDBUsername;
	@Getter @Setter private static String vidDBPassword;
	@Getter @Setter private static String vidDBDriverClassName;
	@Getter @Setter private static String datetimeTimezone;
	@Getter @Setter private static String vidDeactivatedStatus;
	@Getter @Setter private static String vidUnlimitedTxnStatus;
	@Getter @Setter private static String credReqTokenRequestId;
	@Getter @Setter private static String credServiceTokenRequestId;
	@Getter @Setter private static String credServiceTokenRequestIssuerUrl;
	@Getter @Setter private static String credServiceTokenRequestClientId;
	@Getter @Setter private static String credServiceTokenRequestVersion;
	@Getter @Setter private static String credServiceTokenRequestAppId;
	@Getter @Setter private static String credServiceTokenRequestSecretKey;
	@Getter @Setter private static Integer activeAsyncThreadCount;
	@Getter @Setter private static String monitorAsyncThreadQueue;
	@Getter @Setter private static Integer asyncThreadQueueThreshold;
	@Getter @Setter private static String identityMappingJsonUrl;
	@Getter @Setter private static String identityUpdateCountPolicyFileUrl;

	public String getProperty(String key) {
		return env.getProperty(key);
	}

	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return env.getProperty(key, targetType, defaultValue);
	}

	public String getProperty(String key, String defaultValue) {
		return env.getProperty(key, defaultValue);
	}

	public <T> T getProperty(String key, Class<T> targetType) {
		return env.getProperty(key, targetType);
	}

	public void merge(ConfigurableEnvironment parent) {
		((ConfigurableEnvironment) env).merge(parent);
	}

	public boolean containsProperty(String key) {
		return env.containsProperty(key);
	}

	@PostConstruct
	public void init() {
		this.initCredentialRequestGeneratorServiceProperties();
		this.initCredentialServiceProperties();
		setIovDateFormat(this.getProperty("mosip.kernel.idobjectvalidator.date-format"));
		setAnonymousProfileFilterLanguage(this.getProperty("mosip.mandatory-languages", "").split(",")[0]);
		setAppId(this.getProperty(IdRepoConstants.APPLICATION_ID));
		setAppName(this.getProperty(IdRepoConstants.APPLICATION_NAME));
		setAppVersion(this.getProperty(APPLICATION_VERSION));
		setDateTimeAdjustment(this.getProperty(DATETIME_ADJUSTMENT, Long.class, 0l));
		setVersionPattern(this.getProperty(VERSION_PATTERN));
		setDateTimePattern(this.getProperty(DATETIME_PATTERN));
		setCredServiceSchema(this.getProperty("mosip.credential.service.credential.schema"));
		setCredReqServiceId(this.getProperty("mosip.credential.request.service.id"));
		setCredServiceId(this.getProperty("mosip.credential.service.service.id"));
		setCredServiceFormatId(this.getProperty("mosip.credential.service.format.id"));
		setCredServiceFormatIssuer(this.getProperty("mosip.credential.service.format.issuer"));
		setCredReqServiceVersion(this.getProperty("mosip.credential.request.service.version"));
		setCredServiceVersion(this.getProperty("mosip.credential.service.service.version"));
		setVidActiveStatus(this.getProperty(VID_ACTIVE_STATUS));
		setUinActiveStatus(this.getProperty(ACTIVE_STATUS));
		setCredServiceTypeName(this.getProperty("mosip.credential.service.type.name"));
		setCredServiceTypeNamespace(this.getProperty("mosip.credential.service.type.namespace"));
		setCredCryptoRefId(this.getProperty(IdRepoConstants.CREDENTIAL_CRYPTO_REF_ID));
		setCredServiceIncludeCertificateHash(
				this.getProperty("mosip.credential.service.includeCertificateHash", Boolean.class));
		setCredServiceIncludeCertificate(
				this.getProperty("mosip.credential.service.includeCertificate", Boolean.class));
		setCredServiceIncludePayload(this.getProperty("mosip.credential.service.includePayload", Boolean.class));
		setUinJsonPath(this.getProperty(MOSIP_KERNEL_IDREPO_JSON_PATH, ""));
		setIdrepoDBUrl(this.getProperty("mosip.idrepo.identity.db.url"));
		setIdrepoDBUsername(this.getProperty("mosip.idrepo.identity.db.username"));
		setIdrepoDBPassword(this.getProperty("mosip.idrepo.identity.db.password"));
		setIdrepoDBDriverClassName(this.getProperty("mosip.idrepo.identity.db.driverClassName"));
		setPrependThumbprintStatus(this.getProperty(PREPEND_THUMPRINT_STATUS, Boolean.class));
		setIdrepoSaltKeyLength(this.getProperty(SALT_KEY_LENGTH, Integer.class, DEFAULT_SALT_KEY_LENGTH));
		setCredReqTokenIssuerUrl(this.getProperty("credential.request.token.request.issuerUrl"));
		setCredReqTokenVersion(this.getProperty("credential.request.token.request.version"));
		setIsDraftVidTypePresent(this.containsProperty(DEFAULT_VID_TYPE));
		setDraftVidType(this.getProperty(DEFAULT_VID_TYPE));
		setCreateVidId(this.getProperty(VID_CREATE_ID));
		setVidAppVersion(this.getProperty(APPLICATION_VERSION_VID));
		setUpdatedVidId(this.getProperty(VID_UPDATE_ID));
		setVidPolicyFileUrl(this.getProperty(VID_POLICY_FILE_URL));
		setVidPolicySchemaUrl(this.getProperty(VID_POLICY_SCHEMA_URL));
		setVidDBUrl(this.getProperty(VID_DB_URL));
		setVidDBUsername(this.getProperty(VID_DB_USERNAME));
		setVidDBPassword(this.getProperty(VID_DB_PASSWORD));
		setVidDBDriverClassName(this.getProperty(VID_DB_DRIVER_CLASS_NAME));
		setDatetimeTimezone(this.getProperty(IdRepoConstants.DATETIME_TIMEZONE));
		setVidDeactivatedStatus(this.getProperty(VID_DEACTIVATED));
		setVidUnlimitedTxnStatus(this.getProperty(VID_UNLIMITED_TRANSACTION_STATUS));
		setCredReqTokenRequestId(this.getProperty("credential.request.token.request.id"));
		setCredServiceTokenRequestId(this.getProperty("credential.service.token.request.id"));
		setCredServiceTokenRequestIssuerUrl(this.getProperty("credential.service.token.request.issuerUrl"));
		setCredServiceTokenRequestVersion(this.getProperty("credential.service.token.request.version"));
		setActiveAsyncThreadCount(this.getProperty("mosip.idrepo.active-async-thread-count", Integer.class));
		setMonitorAsyncThreadQueue(this.getProperty("mosip.idrepo.monitor-thread-queue-in-ms"));
		setAsyncThreadQueueThreshold(this.getProperty("mosip.idrepo.max-thread-queue-threshold", Integer.class, 0));
		setIdentityMappingJsonUrl(this.getProperty(IDENTITY_MAPPING_JSON));
	}

	private void initCredentialRequestGeneratorServiceProperties() {
		if (env.getProperty("spring.application.name", "").startsWith("credential-request")) {
			setCredReqTokenClientId(this.getProperty("credential.request.token.request.clientId"));
			setCredReqTokenSecretKey(this.getProperty("credential.request.token.request.secretKey"));
			setCredReqTokenAppId(this.getProperty("credential.request.token.request.appid"));
		}
	}

	private void initCredentialServiceProperties() {
		if (env.getProperty("spring.application.name", "").startsWith("credential-service")) {
			setCredServiceTokenRequestClientId(this.getProperty("credential.service.token.request.clientId"));
			setCredServiceTokenRequestSecretKey(this.getProperty("credential.service.token.request.secretKey"));
			setCredServiceTokenRequestAppId(this.getProperty("credential.service.token.request.appid"));
		}
	}
}
