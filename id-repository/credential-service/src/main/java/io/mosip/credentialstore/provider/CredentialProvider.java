package io.mosip.credentialstore.provider;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import io.mosip.credentialstore.util.VIDUtil;
import io.mosip.idrepository.core.dto.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.BestFingerDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.Filter;
import io.mosip.credentialstore.dto.JsonValue;
import io.mosip.credentialstore.dto.PartnerCredentialTypePolicyDto;
import io.mosip.credentialstore.dto.Source;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;


/**
 * The Interface CredentialProvider.
 * 
 * @author Sowmya
 */
@Component
public class CredentialProvider {

	@Autowired
	EncryptionUtil encryptionUtil;

	/** The utilities. */
	@Autowired
	Utilities utilities;

	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private CbeffUtil cbeffutil;

	@Autowired
	private VIDUtil vidUtil;

	@Value("${credential.service.default.vid.type:PERPETUAL}")
	private String defaultVidType;

	@Autowired(required = true)
	@Qualifier("varres")
	VariableResolverFactory functionFactory;

	@Value("${credential.service.dob.format}")
	private String dobFormat;

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialProvider.class);
	/**
	 * Gets the formatted credential data.
	 *
	 * @param encryptMap the encrypt map
	 * @param credentialServiceRequestDto the credential service request dto
	 * @param sharableAttributeMap the sharable attribute map
	 * @return the formatted credential data
	 * @throws CredentialFormatterException the credential formatter exception
	 */
	@SuppressWarnings("unchecked")
	public DataProviderResponse getFormattedCredentialData(
			CredentialServiceRequestDto credentialServiceRequestDto, Map<AllowedKycDto, Object> sharableAttributeMap)
			throws CredentialFormatterException{
		String requestId = credentialServiceRequestDto.getRequestId();
		DataProviderResponse dataProviderResponse=null;
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Formatting credential data");
			String pin = credentialServiceRequestDto.getEncryptionKey();
			List<String> protectedAttributes = new ArrayList<>();
			 Map<String, Object> formattedMap=new HashMap<>();
			formattedMap.put(JsonConstants.ID, credentialServiceRequestDto.getId());

			 for (Map.Entry<AllowedKycDto,Object> entry : sharableAttributeMap.entrySet()) {
				 AllowedKycDto allowedKycDto = entry.getKey();
				 String attributeName = allowedKycDto.getAttributeName();
				 Object value = entry.getValue();
				String valueStr = null;
				if (value instanceof String) {
					valueStr = value.toString();
				} else {
					valueStr = mapper.writeValueAsString(value);
				}
				formattedMap.put(attributeName, valueStr);
				if (allowedKycDto.isEncrypted() || credentialServiceRequestDto.isEncrypt()) {
					if (!valueStr.isEmpty()) {
					String encryptedValue = encryptionUtil.encryptDataWithPin(attributeName, valueStr, pin, requestId);
					formattedMap.put(attributeName, encryptedValue);
					protectedAttributes.add(attributeName);
					}
				} else {
					formattedMap.put(attributeName, valueStr);
				}

			}
			String credentialId = utilities.generateId();


			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			JSONObject json = new JSONObject();
			List<String> typeList = new ArrayList<>();
			typeList.add(EnvUtil.getCredServiceSchema());
			json.put(JsonConstants.ID, EnvUtil.getCredServiceFormatId() + credentialId);
			json.put(JsonConstants.TYPE, typeList);
			json.put(JsonConstants.ISSUER, EnvUtil.getCredServiceFormatIssuer());
			json.put(JsonConstants.ISSUANCEDATE, DateUtils.formatToISOString(localdatetime));
			json.put(JsonConstants.ISSUEDTO, credentialServiceRequestDto.getIssuer());
			json.put(JsonConstants.CONSENT, "");
			json.put(JsonConstants.CREDENTIALSUBJECT, formattedMap);
			json.put(JsonConstants.PROTECTEDATTRIBUTES, protectedAttributes);
			dataProviderResponse = new DataProviderResponse();
			dataProviderResponse.setCredentialId(credentialId);
			dataProviderResponse.setIssuanceDate(localdatetime);
			dataProviderResponse.setJSON(json);
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"end formatting credential data");
			return dataProviderResponse;
		} catch (JsonProcessingException | ApiNotAccessibleException | DataEncryptionFailureException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		}

	}

	@SuppressWarnings("rawtypes")
	public Map<AllowedKycDto, Object> prepareSharableAttributes(IdResponseDTO idResponseDto,
			PartnerCredentialTypePolicyDto policyResponseDto, CredentialServiceRequestDto credentialServiceRequestDto)
			throws CredentialFormatterException {
		String requestId = credentialServiceRequestDto.getRequestId();
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Preparing demo and bio sharable attributes");
		Map<AllowedKycDto, Object> attributesMap = new HashMap<>();
		JSONObject identity = new JSONObject((Map) idResponseDto.getResponse().getIdentity());

		List<AllowedKycDto> sharableAttributeList = policyResponseDto.getPolicies().getShareableAttributes();
		Set<AllowedKycDto> sharableAttributeDemographicKeySet = new HashSet<>();
		Set<AllowedKycDto> sharableAttributeBiometricKeySet = new HashSet<>();
			List<String> userRequestedAttributes = credentialServiceRequestDto.getSharableAttributes();
			Map<String, Object> additionalData = credentialServiceRequestDto.getAdditionalData();

			if (userRequestedAttributes != null && !userRequestedAttributes.isEmpty()) {
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"preparing sharable attributes from user requested if attributes available in policy");
				sharableAttributeList.forEach(dto -> {

					if (userRequestedAttributes.contains(dto.getAttributeName())) {
						if (dto.getGroup() == null) {

							sharableAttributeDemographicKeySet.add(dto);

						} else if (dto.getGroup().equalsIgnoreCase(CredentialConstants.CBEFF)) {
							sharableAttributeBiometricKeySet.add(dto);

						}
					}


				});

			} else {
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"preparing  sharable attributes from policy");
				sharableAttributeList.forEach(dto -> {
					if (dto.getGroup() == null) {

						sharableAttributeDemographicKeySet.add(dto);

					} else if (dto.getGroup().equalsIgnoreCase(CredentialConstants.CBEFF)) {
						sharableAttributeBiometricKeySet.add(dto);

					}

				});
			}

		for (AllowedKycDto key : sharableAttributeDemographicKeySet) {
			String attribute = key.getSource().get(0).getAttribute();

			Object object = identity.get(attribute);
				if (object != null) {
					Object formattedObject = filterAndFormat(key, object, identity);
					attributesMap.put(key, formattedObject);
				} else {
					if (attribute.equalsIgnoreCase(CredentialConstants.FULLNAME)) {
						Object formattedObject = getFullname(identity);
						attributesMap.put(key, formattedObject);
					} else if (attribute.equalsIgnoreCase(CredentialConstants.ENCRYPTIONKEY)) {
						additionalData.put(key.getAttributeName(), credentialServiceRequestDto.getEncryptionKey());
					}else if(attribute.equalsIgnoreCase(CredentialConstants.VID)){
						VidInfoDTO vidInfoDTO;
						VidResponseDTO vidResponseDTO;
						String vidType= key.getSource().get(0).getFilter().get(0).getType()==null?defaultVidType:key.getSource().get(0).getFilter().get(0).getType();
						if(key.getFormat().equalsIgnoreCase(CredentialConstants.RETRIEVE)) {
							vidInfoDTO = vidUtil.getVIDData(identity.get("UIN").toString(), vidType,null);
							if (vidInfoDTO == null) {
								vidResponseDTO=vidUtil.generateVID(identity.get("UIN").toString(), vidType);
								vidInfoDTO = vidUtil.getVIDData(identity.get("UIN").toString(), vidType,vidResponseDTO.getVid());
							}
						}else {
							vidResponseDTO=vidUtil.generateVID(identity.get("UIN").toString(), vidType);
							vidInfoDTO = vidUtil.getVIDData(identity.get("UIN").toString(),vidType,vidResponseDTO.getVid());
						}
						attributesMap.put(key, vidInfoDTO.getVid());
						additionalData.put("ExpiryTimestamp", vidInfoDTO.getExpiryTimestamp().toString());
						additionalData.put("TransactionLimit", vidInfoDTO.getTransactionLimit());
					}
				}
		}
			credentialServiceRequestDto.setAdditionalData(additionalData);
		String individualBiometricsValue = null;
		List<DocumentsDTO> documents = idResponseDto.getResponse().getDocuments();

		for (AllowedKycDto key : sharableAttributeBiometricKeySet) {
			String attribute = key.getSource().get(0).getAttribute();
			for (DocumentsDTO doc : documents) {
				if (doc.getCategory().equals(attribute)) {
					individualBiometricsValue = doc.getValue();
					break;
				}
			}
			if (individualBiometricsValue != null) {
					if ((key.getFormat() != null)
							&& CredentialConstants.BESTTWOFINGERS.equalsIgnoreCase(key.getFormat())) {
						List<BestFingerDto> bestFingerList = getBestTwoFingers(individualBiometricsValue, key);
						attributesMap.put(key, bestFingerList);
					} else {
					String cbeff = filterBiometric(individualBiometricsValue, key);
					attributesMap.put(key, cbeff);
					}
			}

		}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"end preparing demo and bio sharable attributes");
		return attributesMap;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		}
	}

	private List<BestFingerDto> getBestTwoFingers(String individualBiometricsValue, AllowedKycDto key)
			throws Exception {
		List<BestFingerDto> bestFingerList = new ArrayList<>();
		Map<String, Long> subTypeScoreMap = new HashMap<>();
		Source source = key.getSource().get(0);

		List<Filter> filterList = source.getFilter();

		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		filterList.forEach(filter -> {
			if (filter.getSubType() != null && !filter.getSubType().isEmpty()) {
				typeAndSubTypeMap.put(filter.getType(), filter.getSubType());
			} else {
				typeAndSubTypeMap.put(filter.getType(), null);
			}
		});

		List<BIR> birList = cbeffutil.getBIRDataFromXML(CryptoUtil.decodeURLSafeBase64(individualBiometricsValue));

		for (BIR bir : birList) {
			BDBInfo bdbInfo = bir.getBdbInfo();
			String type = bdbInfo.getType().get(0).value();
			if (typeAndSubTypeMap.containsKey(type) && typeAndSubTypeMap.get(type) == null) {

				List<String> bdbSubTypeList = bdbInfo.getSubtype();
				String subType;
				if (bdbSubTypeList != null) {
					subType = getSubType(bdbSubTypeList);
					subTypeScoreMap.put(subType, bdbInfo.getQuality().getScore());
				}

			} else if (typeAndSubTypeMap.containsKey(type) && typeAndSubTypeMap.get(type) != null) {
				List<String> subTypeList = typeAndSubTypeMap.get(type);
				List<String> bdbSubTypeList = bdbInfo.getSubtype();
				String subType;
				if (bdbSubTypeList != null) {
					subType = getSubType(bdbSubTypeList);
					if (subTypeList.contains(subType)) {
						subTypeScoreMap.put(subType, bdbInfo.getQuality().getScore());

					}
				}
			}
		}
		if (!subTypeScoreMap.isEmpty()) {
			if (subTypeScoreMap.size() == 1) {
				String firstBestFinger = Collections.max(subTypeScoreMap.entrySet(), Map.Entry.comparingByValue())
						.getKey();
				bestFingerList.add(new BestFingerDto(firstBestFinger, 1));
			} else {
				String firstBestFinger = Collections.max(subTypeScoreMap.entrySet(), Map.Entry.comparingByValue())
						.getKey();
				subTypeScoreMap.remove(firstBestFinger);
				String secondBestFinger = Collections.max(subTypeScoreMap.entrySet(), Map.Entry.comparingByValue())
						.getKey();
				bestFingerList.add(new BestFingerDto(firstBestFinger, 1));
				bestFingerList.add(new BestFingerDto(secondBestFinger, 2));
			}

		}

		return bestFingerList;
	}

	protected String getSubType(List<String> bdbSubTypeList) {
		String subType = null;
		if (Objects.nonNull(bdbSubTypeList) && !bdbSubTypeList.isEmpty()) {
			if (bdbSubTypeList.size() == 1) {
				subType = bdbSubTypeList.get(0);
			} else {
				subType = bdbSubTypeList.get(0) + " " + bdbSubTypeList.get(1);
			}
		}
		return subType;
	}
	
	@SuppressWarnings("unchecked")
	private JSONArray getFullname(JSONObject identity) {
		Map<String, Map<String, String>> languageMap = new HashMap<>();
		//TODO remove harding of below attributes
		getName(identity, "firstName", languageMap);
		getName(identity, "lastName", languageMap);
		getName(identity, "middleName", languageMap);
		JSONArray array = new JSONArray();
		for (Entry<String, Map<String, String>> languageSpecificEntry : languageMap.entrySet()) {
			Map<String, String> langSpecificfullName = new HashMap<>();
			String lang = languageSpecificEntry.getKey();
			Map<String, String> languageSpecificValues = languageSpecificEntry.getValue();
			String formattedName = formatName(languageSpecificValues.get("firstName"),
					languageSpecificValues.get("lastName"),
					languageSpecificValues.get("middleName"));
			langSpecificfullName.put("language", lang);
			langSpecificfullName.put("value", formattedName);
			JSONObject jsonObject = new JSONObject(langSpecificfullName);
			array.add(jsonObject);
		}
		return array;
	}

	private void getName(JSONObject identity, String attribute, Map<String, Map<String, String>> languageMap) {

		JSONArray node = JsonUtil.getJSONArray(identity, attribute);
		if (node != null) {
		JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
		for (JsonValue jsonValue : jsonValues) {
				Map<String, String> nameMap;
				String lang=jsonValue.getLanguage();
				if (languageMap.containsKey(lang)) {
					nameMap = languageMap.get(lang);
				} else {
					nameMap=new HashMap<>();
				}
				nameMap.put(attribute, jsonValue.getValue());
				languageMap.put(lang, nameMap);
			}
		}
	}

	private String filterBiometric(String individualBiometricsValue, AllowedKycDto key) throws Exception {

		Source source = key.getSource().get(0);

		List<Filter> filterList = source.getFilter();
		if (filterList != null && !filterList.isEmpty()) {
			Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
			filterList.forEach(filter -> {
				if (filter.getSubType() != null && !filter.getSubType().isEmpty()) {
					typeAndSubTypeMap.put(filter.getType(), filter.getSubType());
				} else {
					typeAndSubTypeMap.put(filter.getType(), null);
				}
			});

			List<BIR> birList = cbeffutil.getBIRDataFromXML(CryptoUtil.decodeURLSafeBase64(individualBiometricsValue));

			List<BIR> filteredBIRList = new ArrayList<>();
			for (BIR bir : birList) {
				BDBInfo bdbInfo = bir.getBdbInfo();
				String type = bdbInfo.getType().get(0).value();
				if (typeAndSubTypeMap.containsKey(type) && typeAndSubTypeMap.get(type) == null) {
					filteredBIRList.add(bir);
				} else if (typeAndSubTypeMap.containsKey(type) && typeAndSubTypeMap.get(type) != null) {
					List<String> subTypeList = typeAndSubTypeMap.get(type);
					List<String> bdbSubTypeList = bdbInfo.getSubtype();
					String subType;
					subType = getSubType(bdbSubTypeList);
					if (subTypeList.contains(subType)) {
						filteredBIRList.add(bir);
					}
				}
			}
			if (!filteredBIRList.isEmpty()) {
				byte[] cBEFFByte = cbeffutil.createXML(filteredBIRList);
				return CryptoUtil.encodeToURLSafeBase64(cBEFFByte);

			} else {
				return individualBiometricsValue;
			}
		} else {
			return individualBiometricsValue;
		}

	}

	private Object filterAndFormat(AllowedKycDto key, Object object, JSONObject identity) {
		Object formattedObject = object;
		Source source = key.getSource().get(0);
		String attribute = source.getAttribute();
		List<Filter> filterList = source.getFilter();
		if (filterList != null && !filterList.isEmpty()) {
			Filter filter = filterList.get(0);
			String lang = filter.getLanguage();
			JSONArray node = JsonUtil.getJSONArray(identity, attribute);
			JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
			for (JsonValue jsonValue : jsonValues) {
				if (jsonValue.getLanguage().equals(lang))
					formattedObject = jsonValue.getValue();
			}
		}
		if (key.getFormat() != null) {
			if (attribute.equalsIgnoreCase(CredentialConstants.DATEOFBIRTH)
					&& !key.getFormat().equalsIgnoreCase(CredentialConstants.MASK)) {
				formattedObject = formatDate(formattedObject, key.getFormat());
			} else if (key.getFormat().equalsIgnoreCase(CredentialConstants.MASK)) {
				formattedObject = maskData(formattedObject);
			}
		}

		return formattedObject;
	}

	private Object maskData(Object object) {
		Map<String, String> context = new HashMap<>();
		context.put("value", String.valueOf(object));
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression("convertToMaskData(value);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

	private Object formatDate(Object object, String format) {
		Map<String, String> context = new HashMap<>();
		context.put("value", String.valueOf(object));
		context.put("inputformat", dobFormat);
		context.put("outputformat", format);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression("convertDateFormat(value, inputformat, outputformat);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

	private String formatName(String firstName, String lastName, String middleName) {
		Map<String, String> context = new HashMap<>();
		context.put("firstName", firstName);
		context.put("lastName", lastName);
		context.put("middleName", middleName);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression("formatName(firstName,middleName,lastName);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}
}