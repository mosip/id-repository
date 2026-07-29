package io.mosip.idrepository.credential.store.provider.impl;

import io.mosip.kernel.core.util.DateUtils2;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.credential.store.constant.CredentialConstants;
import io.mosip.idrepository.credential.store.constant.JsonConstants;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.AllowedKycDto;
import io.mosip.idrepository.credential.store.dto.DataProviderResponse;
import io.mosip.idrepository.credential.store.dto.EncryptZkResponseDto;
import io.mosip.idrepository.credential.store.dto.ZkDataAttribute;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.CredentialFormatterException;
import io.mosip.idrepository.credential.store.exception.DataEncryptionFailureException;
import io.mosip.idrepository.credential.store.provider.CredentialProvider;
import io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl;
import io.mosip.idrepository.credential.store.util.EncryptionUtil;
import io.mosip.idrepository.credential.store.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * Credential formatter for ID Authentication (IDA) partner credentials.
 * <p>
 * Role in the MOSIP credential pipeline: selected by {@link CredentialStoreServiceImpl} when
 * partner policy maps the credential type to {@code IdAuthProvider}. Builds a W3C-style JSON-LD
 * credential envelope with zero-knowledge (ZK) encryption for policy-marked attributes; biometric
 * CBEFF payloads are split per modality before ZK encryption.
 * </p>
 *
 * @see CredentialStoreServiceImpl#getProvider(String)
 * @see EncryptionUtil#encryptDataWithZK(String, List, String)
 * @see CredentialProvider
 */
@Component
public class IdAuthProvider extends CredentialProvider {

	/** Shared helpers for ID generation and config-backed property access. */
	@Autowired
	Utilities utilities;

	/**
	 * Config key for the modulo value used in credential formatting.
	 * Property: {@value}.
	 */
	public static final String MODULO_VALUE = "mosip.credential.service.modulo-value";

	/**
	 * Additional-data key holding the ZK-encrypted random key for demographic attributes.
	 */
	public static final String DEMO_ENCRYPTED_RANDOM_KEY = "demoEncryptedRandomKey";

	/**
	 * Additional-data key holding the random-key index for demographic ZK encryption.
	 */
	public static final String DEMO_ENCRYPTED_RANDOM_INDEX = "demoRankomKeyIndex";

	/**
	 * Additional-data key holding the ZK-encrypted random key for biometric attributes.
	 */
	public static final String BIO_ENCRYPTED_RANDOM_KEY = "bioEncryptedRandomKey";

	/**
	 * Additional-data key holding the random-key index for biometric ZK encryption.
	 */
	public static final String BIO_ENCRYPTED_RANDOM_INDEX = "bioRankomKeyIndex";

	/**
	 * Config key for the datetime pattern used in credential issuance timestamps.
	 * Property: {@value}.
	 */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** Delegates ZK and PIN-based encryption to cryptomanager. */
	@Autowired
	EncryptionUtil encryptionUtil;

	/** Class logger. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(IdAuthProvider.class);

	/** Serializes non-string sharable attribute values to JSON strings. */
	@Lazy
	@Autowired
	private ObjectMapper mapper;

	/** Parses and re-packages CBEFF biometric XML for per-modality ZK encryption. */
	@Autowired
	private CbeffUtil cbeffutil;

	/**
	 * Formats sharable identity attributes into an IDA credential JSON document.
	 * <p>
	 * Encrypted attributes are grouped into demographic and biometric ZK batches; cleartext
	 * attributes are placed directly in {@code credentialSubject}. ZK metadata is stored in
	 * {@link CredentialServiceRequestDto#getAdditionalData()} for the WebSub event payload.
	 * </p>
	 *
	 * @param credentialServiceRequestDto issuance request (UIN/VID, issuer, requestId, additionalData)
	 * @param sharableAttributeMap       policy-filtered attributes keyed by {@link AllowedKycDto}
	 * @return formatted credential JSON, generated credentialId, and issuance timestamp
	 * @throws CredentialFormatterException if serialization, ZK encryption, or downstream API calls fail
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DataProviderResponse getFormattedCredentialData(
			CredentialServiceRequestDto credentialServiceRequestDto, Map<AllowedKycDto, Object> sharableAttributeMap)
			throws CredentialFormatterException {
		String requestId = credentialServiceRequestDto.getRequestId();
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"formatting the data start");
		DataProviderResponse dataProviderResponse=new DataProviderResponse();
		try {
			List<String> protectedAttributes = new ArrayList<>();
			List<ZkDataAttribute> bioZkDataAttributes=new ArrayList<>();
			
			List<ZkDataAttribute> demoZkDataAttributes=new ArrayList<>();
            Map<String, Object> formattedMap=new HashMap<>();
			for (Map.Entry<AllowedKycDto, Object> entry : sharableAttributeMap.entrySet()) {
				AllowedKycDto allowedKycDto = entry.getKey();
				String attributeName = allowedKycDto.getAttributeName();
				Object value = entry.getValue();
				String valueStr=null;
				if (value instanceof String) {
					valueStr=value.toString();
				}else {
					valueStr=mapper.writeValueAsString(value);
				}
				if (allowedKycDto.isEncrypted()) {
					ZkDataAttribute zkDataAttribute=new ZkDataAttribute();
					zkDataAttribute.setIdentifier(attributeName);
					zkDataAttribute.setValue(valueStr);
					if (allowedKycDto.getGroup() != null
							&& allowedKycDto.getGroup().equalsIgnoreCase(CredentialConstants.CBEFF)) {
						bioZkDataAttributes.addAll(splitCbeff(zkDataAttribute.getValue()));
					} else {
						demoZkDataAttributes.add(zkDataAttribute);
					}
					protectedAttributes.add(attributeName);
				} else {
					formattedMap.put(attributeName, valueStr);
				}
				

		}

		 Map<String,Object> additionalData=credentialServiceRequestDto.getAdditionalData();
		 // Demo and bio ZK use independent keymanager calls — run in parallel when both present.
		 EncryptZkResponseDto demoEncryptZkResponseDto = null;
		 EncryptZkResponseDto bioEncryptZkResponseDto = null;
		 if (!demoZkDataAttributes.isEmpty() && !bioZkDataAttributes.isEmpty()) {
			 java.util.concurrent.CompletableFuture<EncryptZkResponseDto> demoFuture =
					 java.util.concurrent.CompletableFuture.supplyAsync(() -> {
						 try {
							 return encryptionUtil.encryptDataWithZK(credentialServiceRequestDto.getId(),
									 demoZkDataAttributes, requestId);
						 } catch (DataEncryptionFailureException | ApiNotAccessibleException e) {
							 throw new java.util.concurrent.CompletionException(e);
						 }
					 });
			 java.util.concurrent.CompletableFuture<EncryptZkResponseDto> bioFuture =
					 java.util.concurrent.CompletableFuture.supplyAsync(() -> {
						 try {
							 return encryptionUtil.encryptDataWithZK(credentialServiceRequestDto.getId(),
									 bioZkDataAttributes, requestId);
						 } catch (DataEncryptionFailureException | ApiNotAccessibleException e) {
							 throw new java.util.concurrent.CompletionException(e);
						 }
					 });
			 try {
				 demoEncryptZkResponseDto = demoFuture.join();
				 bioEncryptZkResponseDto = bioFuture.join();
			 } catch (java.util.concurrent.CompletionException e) {
				 Throwable cause = e.getCause() != null ? e.getCause() : e;
				 if (cause instanceof DataEncryptionFailureException de) {
					 throw de;
				 }
				 if (cause instanceof ApiNotAccessibleException api) {
					 throw api;
				 }
				 throw new CredentialFormatterException(cause);
			 }
		 } else if (!demoZkDataAttributes.isEmpty()) {
				demoEncryptZkResponseDto = encryptionUtil
						.encryptDataWithZK(credentialServiceRequestDto.getId(), demoZkDataAttributes, requestId);
		 } else if (!bioZkDataAttributes.isEmpty()) {
				bioEncryptZkResponseDto = encryptionUtil
						.encryptDataWithZK(credentialServiceRequestDto.getId(), bioZkDataAttributes, requestId);
		 }
		 if (demoEncryptZkResponseDto != null) {
			 addToFormatter(demoEncryptZkResponseDto,formattedMap);
			 additionalData.put(DEMO_ENCRYPTED_RANDOM_KEY, demoEncryptZkResponseDto.getEncryptedRandomKey());
			 additionalData.put(DEMO_ENCRYPTED_RANDOM_INDEX, demoEncryptZkResponseDto.getRankomKeyIndex());
		 }
		 if (bioEncryptZkResponseDto != null) {
			 addToFormatter(bioEncryptZkResponseDto,formattedMap);
			 additionalData.put(BIO_ENCRYPTED_RANDOM_KEY, bioEncryptZkResponseDto.getEncryptedRandomKey());
			 additionalData.put(BIO_ENCRYPTED_RANDOM_INDEX, bioEncryptZkResponseDto.getRankomKeyIndex());
		 }  

			String credentialId = utilities.generateId();

		    credentialServiceRequestDto.setAdditionalData(additionalData);


			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			JSONObject json = new JSONObject();
			List<String> typeList = new ArrayList<>();
			typeList.add(EnvUtil.getCredServiceSchema());

			json.put(JsonConstants.ID, EnvUtil.getCredServiceFormatId() + credentialId);
			json.put(JsonConstants.TYPE, typeList);
			json.put(JsonConstants.ISSUER, EnvUtil.getCredServiceFormatIssuer());
			json.put(JsonConstants.ISSUANCEDATE, DateUtils2.formatToISOString(localdatetime));
			json.put(JsonConstants.ISSUEDTO, credentialServiceRequestDto.getIssuer());
			json.put(JsonConstants.CONSENT, "");
			json.put(JsonConstants.CREDENTIALSUBJECT, formattedMap);
			json.put(JsonConstants.PROTECTEDATTRIBUTES, protectedAttributes);
			dataProviderResponse.setIssuanceDate(localdatetime);
			dataProviderResponse.setJSON(json);
			dataProviderResponse.setCredentialId(credentialId);
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"end formatting credential data");
			return dataProviderResponse;
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (DataEncryptionFailureException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (ApiNotAccessibleException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		}

	}
	

	private void addToFormatter(EncryptZkResponseDto demoEncryptZkResponseDto, Map<String, Object> formattedMap) {
		List<ZkDataAttribute> zkDataAttributes= demoEncryptZkResponseDto.getZkDataAttributes();
		for(ZkDataAttribute attribute:zkDataAttributes) {
			formattedMap.put(attribute.getIdentifier(), attribute.getValue());
		}		
	}
	
	private List<ZkDataAttribute> splitCbeff(String individualBiometricsValue) throws Exception {
		List<ZkDataAttribute> zkDataAttributes = new ArrayList<>();
		List<BIR> birList = cbeffutil.getBIRDataFromXML(CryptoUtil.decodeURLSafeBase64(individualBiometricsValue));
		for (BIR bir : birList) {
			List<BIR> birs = new ArrayList<>();
			birs.add(bir);
			BDBInfo bdbInfo = bir.getBdbInfo();
			String type = bdbInfo.getType().get(0).value();
			String subType = super.getSubType(bdbInfo.getSubtype());
			if (subType != null) {
				ZkDataAttribute zkDataAttribute = new ZkDataAttribute();
				zkDataAttribute.setIdentifier(type + "_" + subType);
				zkDataAttribute.setValue(new String(cbeffutil.createXML(birs)));
				zkDataAttributes.add(zkDataAttribute);
			}
		}
		
		List<BIR> faceBirList = birList.stream()
				.filter(bir -> bir.getBdbInfo().getType().get(0).value().toLowerCase().startsWith(BiometricType.FACE.value().toLowerCase()))
				.collect(Collectors.toList());
		if (!faceBirList.isEmpty()) {
			ZkDataAttribute zkDataAttribute = new ZkDataAttribute();
			zkDataAttribute.setIdentifier(BiometricType.FACE.value());
			zkDataAttribute.setValue(new String(cbeffutil.createXML(faceBirList)));
			zkDataAttributes.add(zkDataAttribute);
		}
		return zkDataAttributes;
	}
	
}
