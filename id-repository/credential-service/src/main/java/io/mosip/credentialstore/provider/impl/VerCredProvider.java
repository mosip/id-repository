package io.mosip.credentialstore.provider.impl;

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

import javax.annotation.PostConstruct;

import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.dto.BestFingerDto;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import foundation.identity.jsonld.ConfigurableDocumentLoader;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.canonicalizer.URDNA2015Canonicalizer;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.exception.VerCredException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;

@Component
public class VerCredProvider extends CredentialProvider {

	private static final Logger LOGGER = IdRepoLogger.getLogger(VerCredProvider.class);

	@Autowired
	EncryptionUtil encryptionUtil;

	@Autowired
	private DigitalSignatureUtil digitalSignatureUtil;
	
	/** The utilities. */
	@Autowired
	Utilities utilities;

	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	@Autowired
	private ObjectMapper mapper;

	@Value("${config.server.file.storage.uri:}")
	private String configServerFileStorageURL;
	
	@Value("#{${mosip.credential.service.vercred.context.url.map}}")
	private Map<String, String> vcContextUrlMap;

	@Value("${mosip.credential.service.vercred.context.uri:}")
	private String vcContextUri;

	@Value("${mosip.credential.service.vercred.id.url:}")
	private String verCredIdUrl;

	@Value("${mosip.credential.service.vercred.issuer.url:}")
	private String verCredIssuer;

	@Value("#{'${mosip.credential.service.vercred.types:}'.split(',')}")
	private List<String> verCredTypes;

	@Value("${mosip.credential.service.vercred.proof.purpose:}")
	private String proofPurpose;

	@Value("${mosip.credential.service.vercred.proof.type:}")
	private String proofType;

	@Value("${mosip.credential.service.vercred.proof.verificationmethod:}")
	private String verificationMethod;

	private ConfigurableDocumentLoader confDocumentLoader = null;

	private JSONObject vcContextJsonld = null;

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
			LocalDateTime localdatetime = LocalDateTime.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

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
			verCredJsonObject.put(JsonConstants.VC_ISSUANCE_DATE, DateUtils.formatToISOString(localdatetime));

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
