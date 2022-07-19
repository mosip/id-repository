package io.mosip.credentialstore.provider;

import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
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
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
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

	@Autowired(required = true)
	@Qualifier("varres")
	VariableResolverFactory functionFactory;

	@Value("${credential.service.dob.format}")
	private String dobFormat;
	
	@Value("${mosip.identity.mapping-file}")
	private String identityMappingJson;

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialProvider.class);
	
	private IdentityMapping identityMap; 
	
	public IdentityMapping getIdentityMapping() throws MalformedURLException,Exception{
		IdentityMapping identityMapping ;
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			 identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
					IdentityMapping.class);
		}
		return identityMapping;
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


			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

			JSONObject json = new JSONObject();
			List<String> typeList = new ArrayList<>();
			typeList.add(env.getProperty("mosip.credential.service.credential.schema"));
			json.put(JsonConstants.ID, env.getProperty("mosip.credential.service.format.id") + credentialId);
			json.put(JsonConstants.TYPE, typeList);
			json.put(JsonConstants.ISSUER, env.getProperty("mosip.credential.service.format.issuer"));
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
			if(sharableAttributeList != null && userRequestedAttributes!=null) {
				  userRequestedSharableAttributesList =  sharableAttributeList.stream()
					      .filter(allowedKycDto -> userRequestedAttributes.contains(allowedKycDto.getAttributeName()))
					      .collect(Collectors.toList());
				  sharableAttributeList=userRequestedSharableAttributesList;
				  
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
			
			Object formattedObject = null;
			List<String> userReqMaskingAttributes = (List) additionalData.get("maskingAttributes");
			Map<String, Object> userReqFormatingAttributes = (Map) additionalData.get("formatingAttributes");			
			identityMap = getIdentityMapping();
						
		for (AllowedKycDto key : sharableAttributeDemographicKeySet) {
			String attribute = key.getSource().get(0).getAttribute();
			if(!userReqMaskingAttributes.contains(attribute) && !userReqFormatingAttributes.containsKey(attribute))
				continue;
			Object object = identity.get(attribute);
				if (object != null) {
					 if(userReqMaskingAttributes.contains(attribute) && attribute.equals(CredentialConstants.EMAIL)) {
						 formattedObject = filterAndMask(key, object, identity);
					 }else
						 formattedObject =convertToMaskDataFormat(attribute);
					 	attributesMap.put(key, formattedObject);
					 
					 if(userReqFormatingAttributes.containsKey(attribute)) {
						 formattedObject = filterAndFormat(key, formattedObject, identity,userReqFormatingAttributes.get(attribute));
					 }
					 attributesMap.put(key, formattedObject);					 
					 if (attribute.equalsIgnoreCase(CredentialConstants.ENCRYPTIONKEY)) {
						additionalData.put(key.getAttributeName(), credentialServiceRequestDto.getEncryptionKey());
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

	private Object filterAndMask(AllowedKycDto key, Object object, JSONObject identity) {
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
		formattedObject = convertToMaskDataFormat(formattedObject.toString());
		return formattedObject;
	}
	
	private Object filterAndFormat(AllowedKycDto key, Object object, JSONObject identity,Object formattingObj) throws Exception {
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
		Map<String,String> formatObj = (Map) formattingObj;
		
		if(attribute.equals(CredentialConstants.FULLADDRESS))
			formattedObject = getFullAddress(identity);
		else if(attribute.equals(CredentialConstants.NAME))
			formattedObject = getFullName(identity);
		else if(attribute.equals(CredentialConstants.DATEOFBIRTH))
			formattedObject = formatDate(formattedObject, formatObj.get(CredentialConstants.FORMAT));
		return formattedObject;
	}
	
	private JSONArray getFullName(JSONObject identity) throws Exception {
		List<String> nameAttributes = getNameAttributes();
		Map<String, Map<String, String>> languageMap = new HashMap<>();
		JSONArray array = new JSONArray();
		System.out.println("name attributes:  ");
		for (String nameAttr : nameAttributes) {
			Object nameObj = identity.get(nameAttr);
			if(nameObj  != null && nameObj instanceof List) {
				getIdentityAttributeForList(identity, nameAttr, languageMap);
			}else if(nameObj != null && nameObj instanceof String) {
				getIdentityAttribute(identity, nameAttr, languageMap);
			}
		}
		for (Entry<String, Map<String, String>> languageSpecificEntry : languageMap.entrySet()) {
			Map<String, String> langSpecificfullName = new HashMap<>();
			String lang = languageSpecificEntry.getKey();
			List<String> name = new ArrayList<>();
			Map<String, String> languageSpecificValues = languageSpecificEntry.getValue();
			for (String nameAttr : nameAttributes) {
				if(nameAttr != null && languageSpecificValues.get(nameAttr) != null) {
					name.add(languageSpecificValues.get(nameAttr));
				}
			}
			String formattedName = formatName(name);
			langSpecificfullName.put("language", lang);
			langSpecificfullName.put("value", formattedName);
			JSONObject jsonObject = new JSONObject(langSpecificfullName);
			array.add(jsonObject);
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	private JSONArray getFullAddress(JSONObject identity) throws Exception {
		List<String> addressAttributes = getAddressAttributes();
		Map<String, Map<String, String>> languageMap = new HashMap<>();
		JSONArray array = new JSONArray();
		System.out.println("address attributes:  ");
		for (String addressAttr : addressAttributes) {
			System.out.println(addressAttr+"   "+identity.get(addressAttr));
			if(identity.get(addressAttr) != null && identity.get(addressAttr) instanceof List) {
				getIdentityAttributeForList(identity, addressAttr, languageMap);
			}
		}		
		for (Entry<String, Map<String, String>> languageSpecificEntry : languageMap.entrySet()) {
			Map<String, String> langSpecificfullName = new HashMap<>();
			List<String> address = new ArrayList<>();
			String lang = languageSpecificEntry.getKey();
			Map<String, String> languageSpecificValues = languageSpecificEntry.getValue();
			for ( String addressAttr : addressAttributes) {
				if (addressAttr != null && languageSpecificValues.get(addressAttr) != null) {
					address.add(languageSpecificValues.get(addressAttr));
				}
			}
			if(identity.get(CredentialConstants.POSTALCODE) != null)
				address.add((String)identity.get(CredentialConstants.POSTALCODE));
			String formattedAddress = formatAddress(address);
			langSpecificfullName.put("language", lang);
			langSpecificfullName.put("value", formattedAddress);
			JSONObject jsonObject = new JSONObject(langSpecificfullName);
			array.add(jsonObject);
		}
		for(Object address : array) {
			System.out.println(address.toString());
		}			
		return array;
	}

	private List<String> getAddressAttributes() throws Exception{
		List<String> addressAttributes=identityMap.getIdentity().getFullAddress().getValueList();
		return addressAttributes;
	}

	private List<String> getNameAttributes() throws Exception{
		List<String> nameAttributes= identityMap.getIdentity().getName().getValueList();
		return nameAttributes;
	}
	
	private void getIdentityAttribute(JSONObject identity, String attribute,Map<String, Map<String, String>> languageMap) {
		if (identity.get(attribute) == null) {
			return;
		}
        String jsonValue =(String) identity.get(attribute);
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

	private void getIdentityAttributeForList(JSONObject identity, String attribute, Map<String, Map<String, String>> languageMap) {
		if(identity.get(attribute)==null) {
			return;
		}
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
	
	private String formatName(List<String> name) {
		Map<String, List<String>> context = new HashMap<>();
		for (String nam : name) {
			System.out.println(nam);
		}
		context.put("name", name);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(env.getProperty(NAME) + "(name);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

	private String formatAddress(List<String> address) {
		Map<String, List<String>> context = new HashMap<>();
		for (String adr : address) {
			System.out.println(adr);
		}
		context.put("address", address);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(env.getProperty(ADDRESS) + "(address);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

	private String convertToMaskDataFormat(String maskData) {
		System.out.println("Enter into maskData format");
		Map<String, String> context = new HashMap<>();
		context.put("maskData", maskData);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(env.getProperty(IDENTITY_ATTRIBUTES) + "(maskData);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}	
}