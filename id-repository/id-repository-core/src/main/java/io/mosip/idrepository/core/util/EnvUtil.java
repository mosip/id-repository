package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION_VID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_ADJUSTMENT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_PATTERN;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEFAULT_SALT_KEY_LENGTH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEFAULT_VID_TYPE;
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
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.AbstractEnvironment;
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
@Primary
public class EnvUtil extends AbstractEnvironment {
	
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

	@PostConstruct
	public void init() {
		this.merge((ConfigurableEnvironment) env);
		this.initCredentialRequestGeneratorServiceProperties();
		this.initCredentialServiceProperties();
		setIovDateFormat(super.getProperty("mosip.kernel.idobjectvalidator.date-format"));
		setAnonymousProfileFilterLanguage(super.getProperty("mosip.mandatory-languages", "").split(",")[0]);
		setAppId(super.getProperty(IdRepoConstants.APPLICATION_ID));
		setAppName(super.getProperty(IdRepoConstants.APPLICATION_NAME));
		setAppVersion(super.getProperty(APPLICATION_VERSION));
		setDateTimeAdjustment(super.getProperty(DATETIME_ADJUSTMENT, Long.class, 0l));
		setVersionPattern(super.getProperty(VERSION_PATTERN));
		setDateTimePattern(super.getProperty(DATETIME_PATTERN));
		setCredServiceSchema(super.getProperty("mosip.credential.service.credential.schema"));
		setCredReqServiceId(super.getProperty("mosip.credential.request.service.id"));
		setCredServiceId(super.getProperty("mosip.credential.service.service.id"));
		setCredServiceFormatId(super.getProperty("mosip.credential.service.format.id"));
		setCredServiceFormatIssuer(super.getProperty("mosip.credential.service.format.issuer"));
		setCredReqServiceVersion(super.getProperty("mosip.credential.request.service.version"));
		setCredServiceVersion(super.getProperty("mosip.credential.service.service.version"));
		setVidActiveStatus(super.getProperty(VID_ACTIVE_STATUS));
		setUinActiveStatus(super.getProperty(ACTIVE_STATUS));
		setCredServiceTypeName(super.getProperty("mosip.credential.service.type.name"));
		setCredServiceTypeNamespace(super.getProperty("mosip.credential.service.type.namespace"));
		setCredCryptoRefId(super.getProperty(IdRepoConstants.CREDENTIAL_CRYPTO_REF_ID));
		setCredServiceIncludeCertificateHash(super.getProperty("mosip.credential.service.includeCertificateHash", Boolean.class));
		setCredServiceIncludeCertificate(super.getProperty("mosip.credential.service.includeCertificate", Boolean.class));
		setCredServiceIncludePayload(super.getProperty("mosip.credential.service.includePayload", Boolean.class));
		setUinJsonPath(super.getProperty(MOSIP_KERNEL_IDREPO_JSON_PATH, ""));
		setIdrepoDBUrl(super.getProperty("mosip.idrepo.identity.db.url"));
		setIdrepoDBUsername(super.getProperty("mosip.idrepo.identity.db.username"));
		setIdrepoDBPassword(super.getProperty("mosip.idrepo.identity.db.password"));
		setIdrepoDBDriverClassName(super.getProperty("mosip.idrepo.identity.db.driverClassName"));
		setPrependThumbprintStatus(super.getProperty(PREPEND_THUMPRINT_STATUS, Boolean.class));
		setIdrepoSaltKeyLength(super.getProperty(SALT_KEY_LENGTH, Integer.class, DEFAULT_SALT_KEY_LENGTH));
		setCredReqTokenIssuerUrl(super.getProperty("credential.request.token.request.issuerUrl"));
		setCredReqTokenVersion(super.getProperty("credential.request.token.request.version"));
		setIsDraftVidTypePresent(super.containsProperty(DEFAULT_VID_TYPE));
		setDraftVidType(super.getProperty(DEFAULT_VID_TYPE));
		setCreateVidId(super.getProperty(VID_CREATE_ID));
		setVidAppVersion(super.getProperty(APPLICATION_VERSION_VID));
		setUpdatedVidId(super.getProperty(VID_UPDATE_ID));
		setVidPolicyFileUrl(super.getProperty(VID_POLICY_FILE_URL));
		setVidPolicySchemaUrl(super.getProperty(VID_POLICY_SCHEMA_URL));
		setVidDBUrl(super.getProperty(VID_DB_URL));
		setVidDBUsername(super.getProperty(VID_DB_USERNAME));
		setVidDBPassword(super.getProperty(VID_DB_PASSWORD));
		setVidDBDriverClassName(super.getProperty(VID_DB_DRIVER_CLASS_NAME));
		setDatetimeTimezone(super.getProperty(IdRepoConstants.DATETIME_TIMEZONE));
		setVidDeactivatedStatus(super.getProperty(VID_DEACTIVATED));
		setVidUnlimitedTxnStatus(super.getProperty(VID_UNLIMITED_TRANSACTION_STATUS));
		setCredReqTokenRequestId(super.getProperty("credential.request.token.request.id"));
		setCredServiceTokenRequestId(super.getProperty("credential.service.token.request.id"));
		setCredServiceTokenRequestIssuerUrl(super.getProperty("credential.service.token.request.issuerUrl"));
		setCredServiceTokenRequestVersion(super.getProperty("credential.service.token.request.version"));
	}
	
	private void initCredentialRequestGeneratorServiceProperties() {
		if (env.getProperty("spring.application.name", "").startsWith("credential-request")) {
			setCredReqTokenClientId(super.getProperty("credential.request.token.request.clientId"));
			setCredReqTokenSecretKey(super.getProperty("credential.request.token.request.secretKey"));
			setCredReqTokenAppId(super.getProperty("credential.request.token.request.appid"));
		}
	}
	
	private void initCredentialServiceProperties() {
		if (env.getProperty("spring.application.name", "").startsWith("credential-service")) {
			setCredServiceTokenRequestClientId(super.getProperty("credential.service.token.request.clientId"));
			setCredServiceTokenRequestSecretKey(super.getProperty("credential.service.token.request.secretKey"));
			setCredServiceTokenRequestAppId(super.getProperty("credential.service.token.request.appid"));
		}
	}
}
