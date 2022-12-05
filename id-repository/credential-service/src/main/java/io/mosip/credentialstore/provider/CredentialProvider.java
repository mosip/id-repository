package io.mosip.credentialstore.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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
import io.mosip.credentialstore.util.VIDUtil;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.dto.VidResponseDTO;
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
	
	public static final String IDENTITY_ATTRIBUTES = "mosip.mask.function.identityAttributes";
	
	public static final String DATE = "mosip.mask.function.date";
	
	public static final String ADDRESS = "mosip.format.function.address";
	
	public static final String NAME = "mosip.format.function.name";
	
	public static final String DATE_TIME_FORMAT = "mosip.format.function.dateTimeFormat";

	@Autowired
	private Environment env;



	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private CbeffUtil cbeffutil;

	@Autowired
	VIDUtil vidUtil;

	@Autowired(required = true)
	@Qualifier("varres")
	VariableResolverFactory functionFactory;

	@Value("${credential.service.dob.format}")
	private String dobFormat;

	@Value("${credential.service.default.vid.type:PERPETUAL}")
	private String defaultVidType;

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialProvider.class);
	
	private IdentityMapping identityMap;
	
	@Value("${mosip.identity.mapping-file}")
	private String identityMappingJson;

	@PostConstruct
	private void getMapping() throws IOException {
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
					IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder.setDateFormat(EnvUtil.getIovDateFormat());
	}
	
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
		List<AllowedKycDto> userRequestedSharableAttributesList=Collections.EMPTY_LIST;
		
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
			if (sharableAttributeList != null && userRequestedAttributes != null && !userRequestedAttributes.isEmpty()) {
				userRequestedSharableAttributesList = sharableAttributeList.stream()
						.filter(allowedKycDto -> userRequestedAttributes.contains(allowedKycDto.getAttributeName()))
						.collect(Collectors.toList());
				sharableAttributeList = userRequestedSharableAttributesList;
				
			}
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
			
			List<String> userReqMaskingAttributes = (List) additionalData.get(CredentialConstants.MASKING_ATTRIBUTES);
			Map<String, Object> userReqFormatingAttributes = (Map) additionalData.get(CredentialConstants.FORMATTING_ATTRIBUTES);			
			identityMap = IdentityIssuanceProfileBuilder.getIdentityMapping();

		//formatting and masking the data based on request	
		for (AllowedKycDto key : sharableAttributeDemographicKeySet) {
			String attribute = key.getSource().get(0).getAttribute();
			Object object = identity.get(attribute);
			Object formattedObject=object;
			
				if (object != null) {
					if (userReqMaskingAttributes != null && !userReqMaskingAttributes.isEmpty()
							&& userReqMaskingAttributes.contains(attribute)) {
						formattedObject = maskData(object.toString());
					} 
					if (userReqFormatingAttributes != null && !userReqFormatingAttributes.isEmpty()
							&& userReqFormatingAttributes.containsKey(attribute)) {
						formattedObject = filterAndFormat(key, identity, userReqFormatingAttributes);
					}
					attributesMap.put(key, formattedObject);
				} else if (attribute.equalsIgnoreCase(CredentialConstants.FULLNAME)) {
					 formattedObject = filterAndFormat(key, identity, userReqFormatingAttributes);
					attributesMap.put(key, formattedObject);
				}else if (attribute.equalsIgnoreCase(CredentialConstants.ENCRYPTIONKEY)) {
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

	/**
	 * format the data based on user request
	 * @param key
	 * @param identity
	 * @param userReqFormatingAttributes
	 * @return
	 * @throws Exception
	 */
	private Object filterAndFormat(AllowedKycDto key, JSONObject identity,
			Map<String, Object> userReqFormatingAttributes) throws Exception {
		Object formattedObject = null;
		Source source = key.getSource().get(0);
		String attribute = source.getAttribute();
		if (attribute.equals(CredentialConstants.DATEOFBIRTH)) {
			String dateOfBirth = (String) userReqFormatingAttributes.get(CredentialConstants.DATEOFBIRTH);
			formattedObject = formatDate(identity.get(CredentialConstants.DATEOFBIRTH), dateOfBirth);
		} else if (attribute.equals(CredentialConstants.FULLNAME) || attribute.equals(CredentialConstants.NAME) || attribute.equals(CredentialConstants.FULLADDRESS))
			formattedObject = formatData(identity, attribute);
		return formattedObject;
	}

	/**
	 * 
	 * @param identity
	 * @param format the name and address attributes
	 * @return
	 * @throws Exception
	 */
	private JSONArray formatData(JSONObject identity, String formatAttrName) throws Exception {
		List<String> identityAttributesList = Collections.EMPTY_LIST;
		String formattedData = "";
		if (formatAttrName.equals(CredentialConstants.NAME) || formatAttrName.equals(CredentialConstants.FULLNAME)) {
			identityAttributesList = getNameAttributes();
		} else if (formatAttrName.equals(CredentialConstants.FULLADDRESS)) {
			identityAttributesList = getAddressAttributes();
		}
		Map<String, Map<String, String>> languageMap = new HashMap<>();
		JSONArray array = new JSONArray();
		LOGGER.debug("name attributes:  ");
		for (String identityAttr : identityAttributesList) {
			Object identityObj = identity.get(identityAttr);
			if (identityObj != null && identityObj instanceof List) {
				getIdentityAttributeForList(identity, identityAttr, languageMap);
			} else if (identityObj != null && identityObj instanceof String) {
				getIdentityAttribute(identity, identityAttr, languageMap);
			}
		}
		for (Entry<String, Map<String, String>> languageSpecificEntry : languageMap.entrySet()) {
			Map<String, String> langSpecificIdentity = new HashMap<>();
			String lang = languageSpecificEntry.getKey();
			List<String> formatDataList = new ArrayList<>();
			Map<String, String> languageSpecificValues = languageSpecificEntry.getValue();
			for (String identityAttr : identityAttributesList) {
				if (identityAttr != null && languageSpecificValues.get(identityAttr) != null) {
					formatDataList.add(languageSpecificValues.get(identityAttr));
				}
			}
			if (formatAttrName.equals(CredentialConstants.NAME))
				formattedData = formatName(formatDataList);
			else if (formatAttrName.equals(CredentialConstants.FULLADDRESS))
				formattedData = formatAddress(formatDataList);
			langSpecificIdentity.put("language", lang);
			langSpecificIdentity.put("value", formattedData);
			JSONObject jsonObject = new JSONObject(langSpecificIdentity);
			array.add(jsonObject);
		}
		return array;
	}
	
	/**
	 * get the address data from identity json file
	 * @return
	 * @throws Exception
	 */
	private List<String> getAddressAttributes() throws Exception {
		List<String> addressAttributes = identityMap.getIdentity().getFullAddress().getValueList();
		return addressAttributes;
	}

	/**
	 * get the name data from identity json file
	 * @return
	 * @throws Exception
	 */
	private List<String> getNameAttributes() throws Exception {
		List<String> nameAttributes = identityMap.getIdentity().getName().getValueList();
		return nameAttributes;
	}

	/**
	 * get the identity attribute based on requested data
	 * 
	 * @param identity
	 * @param attribute
	 * @param languageMap
	 */
	private void getIdentityAttribute(JSONObject identity, String attribute,
			Map<String, Map<String, String>> languageMap) {
		if (identity.get(attribute) == null) {
			return;
		}
		String jsonValue = (String) identity.get(attribute);
		Map<String, String> nameMap;
		String lang = CredentialConstants.LANGUAGE;
		if (languageMap.containsKey(lang)) {
			nameMap = languageMap.get(lang);
		} else {
			nameMap = new HashMap<>();
		}
		nameMap.put(attribute, jsonValue);
		languageMap.put(lang, nameMap);
	}

	/**
	 * get the identity attributes list based on requested data
	 * @param identity
	 * @param attribute
	 * @param languageMap
	 */
	private void getIdentityAttributeForList(JSONObject identity, String attribute,
			Map<String, Map<String, String>> languageMap) {
		if (identity.get(attribute) == null) {
			return;
		}
		JSONArray node = JsonUtil.getJSONArray(identity, attribute);
		if (node != null) {
			JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
			for (JsonValue jsonValue : jsonValues) {
				Map<String, String> nameMap;
				String lang = jsonValue.getLanguage();
				if (languageMap.containsKey(lang)) {
					nameMap = languageMap.get(lang);
				} else {
					nameMap = new HashMap<>();
				}
				nameMap.put(attribute, jsonValue.getValue());
				languageMap.put(lang, nameMap);
			}
		}
	}	

	/**
	 * format the date based on user requested data format
	 * @param object
	 * @param format
	 * @return
	 */
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

	/**
	 * format the name attribute
	 * @param name
	 * @return
	 */
	private String formatName(List<String> name) {
		Map<String, List<String>> context = new HashMap<>();
		context.put("name", name);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(env.getProperty(NAME) + "(name);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

	/**
	 * format the address attribute
	 * @param address
	 * @return
	 */
	private String formatAddress(List<String> address) {
		Map<String, List<String>> context = new HashMap<>();
		context.put("address", address);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(env.getProperty(ADDRESS) + "(address);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

	/**
	 * masking the UIN,VID,Phone and email attribtute's 
	 * @param maskData
	 * @return
	 */
	private String maskData(String maskData) {
		LOGGER.debug("Enter into maskData format");
		Map<String, String> context = new HashMap<>();
		context.put("maskData", maskData);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(env.getProperty(IDENTITY_ATTRIBUTES) + "(maskData);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}	

}