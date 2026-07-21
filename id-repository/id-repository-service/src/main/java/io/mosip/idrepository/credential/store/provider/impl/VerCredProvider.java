package io.mosip.idrepository.credential.store.provider.impl;

import io.mosip.kernel.core.util.DateUtils2;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;

import foundation.identity.jsonld.ConfigurableDocumentLoader;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.canonicalizer.URDNA2015Canonicalizer;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.credential.store.constant.CredentialConstants;
import io.mosip.idrepository.credential.store.constant.JsonConstants;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.AllowedKycDto;
import io.mosip.idrepository.credential.store.dto.BestFingerDto;
import io.mosip.idrepository.credential.store.dto.DataProviderResponse;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.CredentialFormatterException;
import io.mosip.idrepository.credential.store.provider.CredentialProvider;
import io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl;
import io.mosip.idrepository.credential.store.util.DigitalSignatureUtil;
import io.mosip.idrepository.credential.store.util.EncryptionUtil;
import io.mosip.idrepository.credential.store.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import jakarta.annotation.PostConstruct;

/**
 * Credential formatter for W3C Verifiable Credential (VC) partner credentials.
 * <p>
 * Role in the MOSIP credential pipeline: selected by {@link CredentialStoreServiceImpl} when
 * partner policy maps the credential type to {@code VerCredProvider}. Builds a JSON-LD VC with
 * cached context documents, canonicalizes the proof, and signs via {@link DigitalSignatureUtil}.
 * Attribute-level PIN encryption is intentionally disabled for VC compatibility.
 * </p>
 *
 * @see CredentialStoreServiceImpl#getProvider(String)
 * @see DigitalSignatureUtil#signVerCred(String, String)
 * @see CredentialProvider
 */
@Component
public class VerCredProvider extends CredentialProvider {

	/** Class logger. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(VerCredProvider.class);

	/** Delegates encryption (available but attribute-level VC encryption is disabled). */
	@Autowired
	EncryptionUtil encryptionUtil;

	/** Signs the canonicalized VC proof with the credential-service key. */
	@Autowired
	private DigitalSignatureUtil digitalSignatureUtil;

	/** Shared helpers for ID generation, VC context loading, and config access. */
	@Autowired
	Utilities utilities;

	/**
	 * Config key for the datetime pattern used in credential issuance timestamps.
	 * Property: {@value}.
	 */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/**
	 * Base URI of the Spring Cloud Config file storage used to load VC context JSON.
	 * Config key: {@link IdRepoConstants#CONFIG_SERVER_FILE_STORAGE_URI}.
	 */
	@Value("${" + IdRepoConstants.CONFIG_SERVER_FILE_STORAGE_URI + ":}")
	private String configServerFileStorageURL;

	/**
	 * Map of external VC context URLs to local config-server paths for offline JSON-LD resolution.
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_CONTEXT_URL_MAP}.
	 */
	@Value("#{${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_CONTEXT_URL_MAP + "}}")
	private Map<String, String> vcContextUrlMap;

	/**
	 * Config-server path (relative to {@link #configServerFileStorageURL}) of the VC {@code @context} document.
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_CONTEXT_URI}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_CONTEXT_URI + ":}")
	private String vcContextUri;

	/**
	 * URL prefix prepended to the credential subject ID and generated credential ID.
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_ID_URL}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_ID_URL + ":}")
	private String verCredIdUrl;

	/**
	 * VC {@code issuer} field value (typically a DID or URI).
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_ISSUER_URL}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_ISSUER_URL + ":}")
	private String verCredIssuer;

	/**
	 * Comma-separated VC type strings (split at injection); e.g. {@code VerifiableCredential,MOSIPVerifiableCredential}.
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_TYPES}.
	 */
	@Value("#{'${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_TYPES + ":}'.split(',')}")
	private List<String> verCredTypes;

	/**
	 * Linked-data proof purpose (e.g. {@code assertionMethod}).
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_PROOF_PURPOSE}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_PROOF_PURPOSE + ":}")
	private String proofPurpose;

	/**
	 * Linked-data proof type (e.g. {@code RsaSignature2018}).
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_PROOF_TYPE}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_PROOF_TYPE + ":}")
	private String proofType;

	/**
	 * Verification method URI referenced in the VC proof.
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_VERCRED_PROOF_VERIFICATION_METHOD}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_VERCRED_PROOF_VERIFICATION_METHOD + ":}")
	private String verificationMethod;

	/** JSON-LD document loader with pre-cached context documents; initialized in {@link #init()}. */
	private ConfigurableDocumentLoader confDocumentLoader = null;

	/** Parsed VC {@code @context} JSON loaded from config server; initialized in {@link #init()}. */
	private JSONObject vcContextJsonld = null;

	/**
	 * Loads VC context documents from config server into the JSON-LD document loader cache.
	 * <p>
	 * When {@link #vcContextUrlMap} is unset, falls back to remote HTTP/HTTPS resolution with a warning.
	 * </p>
	 */
	@PostConstruct
	private void init() {
		if(Objects.isNull(vcContextUrlMap)){
			LOGGER.warn(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), "VerCred", 
				"Warning - Verifiable Credential Context URL Map not configured, VC generation may fail.");
			confDocumentLoader = new ConfigurableDocumentLoader();
			confDocumentLoader.setEnableHttps(true);
			confDocumentLoader.setEnableHttp(true);
			confDocumentLoader.setEnableFile(false);
		} else {
			Map<URI, JsonDocument> jsonDocumentCacheMap = new HashMap<URI, JsonDocument> ();
			vcContextUrlMap.keySet().stream().forEach(contextUrl -> {
				String localConfigUrl = vcContextUrlMap.get(contextUrl);
				JsonDocument jsonDocument = utilities.getVCContextJson(configServerFileStorageURL, localConfigUrl);
				try {
					jsonDocumentCacheMap.put(new URI(contextUrl), jsonDocument);
				} catch (URISyntaxException e) {
					LOGGER.warn(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), "VerCred", 
							"Warning - Verifiable Credential URI not able to add to cacheMap.");
				}
			});
			confDocumentLoader = new ConfigurableDocumentLoader(jsonDocumentCacheMap);
			confDocumentLoader.setEnableHttps(false);
			confDocumentLoader.setEnableHttp(false);
			confDocumentLoader.setEnableFile(false);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), "VerCred", 
					"Added cache for the list of configured URL Map: " + jsonDocumentCacheMap.keySet().toString());
		}
		vcContextJsonld = utilities.getVCContext(configServerFileStorageURL, vcContextUri);
	}

	/**
	 * Formats sharable identity attributes into a signed W3C Verifiable Credential.
	 * <p>
	 * Builds the VC JSON-LD object, canonicalizes the proof with URDNA2015, signs the digest,
	 * and attaches the JWS to the proof. Best-two-fingers attributes are expanded into structured maps.
	 * </p>
	 *
	 * @param credentialServiceRequestDto issuance request (UIN/VID, issuer, requestId)
	 * @param sharableAttributeMap       policy-filtered attributes keyed by {@link AllowedKycDto}
	 * @return signed VC JSON-LD, generated credentialId, and issuance timestamp
	 * @throws CredentialFormatterException if JSON-LD processing, signing, or downstream API calls fail
	 */
	@Override
	public DataProviderResponse getFormattedCredentialData(
			CredentialServiceRequestDto credentialServiceRequestDto, Map<AllowedKycDto, Object> sharableAttributeMap)
			throws CredentialFormatterException {
		String requestId = credentialServiceRequestDto.getRequestId();
		DataProviderResponse dataProviderResponse = null;
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Formatting credential data");
			String pin = credentialServiceRequestDto.getEncryptionKey();
			
			Map<String, Object> formattedMap = new HashMap<>();
			List<String> protectedAttributes = new ArrayList<>();
			formattedMap.put(JsonConstants.ID, verCredIdUrl + credentialServiceRequestDto.getId());
			formattedMap.put(JsonConstants.VC_VERSION_CONST, JsonConstants.VC_VERSION_1);

			for (Map.Entry<AllowedKycDto, Object> entry : sharableAttributeMap.entrySet()) {
				AllowedKycDto allowedKycDto = entry.getKey();
				String attributeName = allowedKycDto.getAttributeName();
				Object value = entry.getValue();
				String valueStr = null;
				if (value instanceof String) {
					valueStr = value.toString();
					formattedMap.put(attributeName, valueStr);
				}else if ((allowedKycDto.getFormat() != null)
						&& CredentialConstants.BESTTWOFINGERS.equalsIgnoreCase(allowedKycDto.getFormat())) {
					List<BestFingerDto> bestFingerList = (List<BestFingerDto>) value;
					List<Map<String, String>> bestFingerMapList = new ArrayList<>();
					for (BestFingerDto bestFinger : bestFingerList) {
						Map<String, String> bestFingerMap = new HashMap<>();
						bestFingerMap.put(CredentialConstants.BF_SUB_TYPE, bestFinger.getSubType());
						bestFingerMap.put(CredentialConstants.BF_RANK, Integer.toString(bestFinger.getRank()));
						bestFingerMapList.add(bestFingerMap);
					}
					formattedMap.put(attributeName, bestFingerMapList);
				}else {
					// Removed converting any object (like List) to string. Now adding it object directly to the map. 
					formattedMap.put(attributeName, value);
				}
				// Commented below code because VC does not support attribute level encryption.
				/* if (allowedKycDto.isEncrypted() || credentialServiceRequestDto.isEncrypt()) {
					if (Objects.isNull(pin)) {
						LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"pin is null, pin based encryption will not be able to perform.");
						throw new VerCredException(CredentialServiceErrorCodes.PIN_NOT_PROVIDER.getErrorCode(), 
								CredentialServiceErrorCodes.PIN_NOT_PROVIDER.getErrorMessage());
					}
					if (!valueStr.isEmpty()) {
						String encryptedValue = encryptionUtil.encryptDataWithPin(attributeName, valueStr, pin, requestId);
						formattedMap.put(attributeName, encryptedValue);
						protectedAttributes.add(attributeName);
					}
				} else {
					formattedMap.put(attributeName, valueStr);
				} */

			}

			String credentialId = utilities.generateId();

			dataProviderResponse = new DataProviderResponse();
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			Map<String, Object> verCredJsonObject = new HashMap<>();

			// @Context
			verCredJsonObject.put(JsonConstants.VC_AT_CONTEXT, vcContextJsonld.get("context"));

			// vc type
			verCredJsonObject.put(JsonConstants.VC_TYPE, verCredTypes);

			// vc id
			verCredJsonObject.put(JsonConstants.VC_ID, verCredIdUrl + credentialId);

			// vc issuer
			verCredJsonObject.put(JsonConstants.VC_ISSUER, verCredIssuer);

			// vc issuance date
			verCredJsonObject.put(JsonConstants.VC_ISSUANCE_DATE, DateUtils2.formatToISOString(localdatetime));

			// vc credentialSubject
			verCredJsonObject.put(JsonConstants.CREDENTIALSUBJECT, formattedMap);

			// Build the Json LD Object.
			JsonLDObject vcJsonLdObject = JsonLDObject.fromJsonObject(verCredJsonObject);
			vcJsonLdObject.setDocumentLoader(confDocumentLoader);

			// vc proof
			Date created = Date.from(localdatetime.atZone(ZoneId.systemDefault()).toInstant());
			LdProof vcLdProof = LdProof.builder()
										.defaultContexts(false)
										.defaultTypes(false)
										.type(proofType)
										.created(created)
										.proofPurpose(proofPurpose)
										.verificationMethod(new URI(verificationMethod))
										.build();
										
			URDNA2015Canonicalizer canonicalizer =	new URDNA2015Canonicalizer();
			byte[] vcSignBytes = canonicalizer.canonicalize(vcLdProof, vcJsonLdObject);			
			String vcEncodedData = CryptoUtil.encodeToURLSafeBase64(vcSignBytes);

			String jws = digitalSignatureUtil.signVerCred(vcEncodedData, credentialServiceRequestDto.getRequestId());

			LdProof ldProofWithJWS = LdProof.builder()
                .base(vcLdProof)
                .defaultContexts(false)
				.jws(jws)
				.build();
			
			ldProofWithJWS.addToJsonLDObject(vcJsonLdObject);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Verifiable Credential Generation completed for the provided data.");
			dataProviderResponse.setJSON(new JSONObject(vcJsonLdObject.toMap()));

			dataProviderResponse.setCredentialId(credentialId);

			dataProviderResponse.setIssuanceDate(localdatetime);
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"end formatting credential data");
			return dataProviderResponse;
		} /* catch (DataEncryptionFailureException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} */ catch (ApiNotAccessibleException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (JsonProcessingException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		}
	}
	

}
