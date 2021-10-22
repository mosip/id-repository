package io.mosip.credentialstore.provider.impl;

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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;


/**
 * The Class IdAuthProvider.
 * 
 * @author Sowmya
 */
@Component
public class IdAuthProvider extends CredentialProvider {
	
	
	/** The utilities. */
	@Autowired
    Utilities utilities;	
	
	/** The env. */
	@Autowired
	Environment env;

	
	/** The Constant MODULO_VALUE. */
	public static final String MODULO_VALUE = "mosip.credential.service.modulo-value";
	
	/** The Constant DEMO_ENCRYPTED_RANDOM_KEY. */
	public static final String DEMO_ENCRYPTED_RANDOM_KEY = "demoEncryptedRandomKey";
	
	/** The Constant DEMO_ENCRYPTED_RANDOM_INDEX. */
	public static final String DEMO_ENCRYPTED_RANDOM_INDEX = "demoRankomKeyIndex";
	
	/** The Constant BIO_ENCRYPTED_RANDOM_KEY. */
	public static final String BIO_ENCRYPTED_RANDOM_KEY = "bioEncryptedRandomKey";
	
	/** The Constant BIO_ENCRYPTED_RANDOM_INDEX. */
	public static final String BIO_ENCRYPTED_RANDOM_INDEX = "bioRankomKeyIndex";
	
	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The encryption util. */
	@Autowired
	EncryptionUtil encryptionUtil;

	private static final Logger LOGGER = IdRepoLogger.getLogger(IdAuthProvider.class);

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private CbeffUtil cbeffutil;


	/* (non-Javadoc)
	 * @see io.mosip.credentialstore.provider.CredentialProvider#getFormattedCredentialData(java.util.Map, io.mosip.idrepository.core.dto.CredentialServiceRequestDto, java.util.Map)
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
			formattedMap.put(JsonConstants.ID, credentialServiceRequestDto.getId());
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
		 if(!demoZkDataAttributes.isEmpty()) {
				EncryptZkResponseDto demoEncryptZkResponseDto = encryptionUtil
						.encryptDataWithZK(credentialServiceRequestDto.getId(), demoZkDataAttributes, requestId);
			 addToFormatter(demoEncryptZkResponseDto,formattedMap);
			 additionalData.put(DEMO_ENCRYPTED_RANDOM_KEY, demoEncryptZkResponseDto.getEncryptedRandomKey());
			 additionalData.put(DEMO_ENCRYPTED_RANDOM_INDEX, demoEncryptZkResponseDto.getRankomKeyIndex());
		 }
			if (!bioZkDataAttributes.isEmpty()) {
				EncryptZkResponseDto bioEncryptZkResponseDto = encryptionUtil
						.encryptDataWithZK(credentialServiceRequestDto.getId(), bioZkDataAttributes, requestId);
			 addToFormatter(bioEncryptZkResponseDto,formattedMap);
			 additionalData.put(BIO_ENCRYPTED_RANDOM_KEY, bioEncryptZkResponseDto.getEncryptedRandomKey());
			 additionalData.put(BIO_ENCRYPTED_RANDOM_INDEX, bioEncryptZkResponseDto.getRankomKeyIndex());
		 }  

			String credentialId = utilities.generateId();

		    credentialServiceRequestDto.setAdditionalData(additionalData);


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
	


	/**
	 * Adds the to formatter.
	 *
	 * @param demoEncryptZkResponseDto the demo encrypt zk response dto
	 * @param formattedMap the formatted map
	 */
	private void addToFormatter(EncryptZkResponseDto demoEncryptZkResponseDto, Map<String, Object> formattedMap) {
		List<ZkDataAttribute> zkDataAttributes= demoEncryptZkResponseDto.getZkDataAttributes();
		for(ZkDataAttribute attribute:zkDataAttributes) {
			formattedMap.put(attribute.getIdentifier(), attribute.getValue());
		}		
	}
	
	/**
	 * Split the cbeff file based on type and subType
	 * 
	 * @param individualBiometricsValue
	 * @return
	 * @throws Exception
	 */
	private List<ZkDataAttribute> splitCbeff(String individualBiometricsValue) throws Exception {
		List<ZkDataAttribute> zkDataAttributes = new ArrayList<>();
		List<BIRType> typeList = cbeffutil.getBIRDataFromXML(CryptoUtil.decodeBase64(individualBiometricsValue));
		List<BIR> birList = cbeffutil.convertBIRTypeToBIR(typeList);
		for (BIR bir : birList) {
			List<BIR> birs = new ArrayList<>();
			birs.add(bir);
			BDBInfo bdbInfo = bir.getBdbInfo();
			String type = bdbInfo.getType().get(0).value();
			String subType = getSubType(bdbInfo.getSubtype());
			if (subType != null) {
				ZkDataAttribute zkDataAttribute = new ZkDataAttribute();
				zkDataAttribute.setIdentifier(type + "_" + subType);
				zkDataAttribute.setValue(new String(cbeffutil.createXML(birs)));
				zkDataAttributes.add(zkDataAttribute);
			}
		}
		
		List<BIR> faceBirList = birList.stream()
				.filter(bir -> bir.getBdbInfo().getType().get(0).value().toLowerCase().startsWith(SingleType.FACE.value().toLowerCase()))
				.collect(Collectors.toList());
		if (!faceBirList.isEmpty()) {
			ZkDataAttribute zkDataAttribute = new ZkDataAttribute();
			zkDataAttribute.setIdentifier(SingleType.FACE.value());
			zkDataAttribute.setValue(new String(cbeffutil.createXML(faceBirList)));
			zkDataAttributes.add(zkDataAttribute);
		}
		return zkDataAttributes;
	}
	
	/**
	 * 
	 * @param bdbSubTypeList
	 * @return
	 */
	private String getSubType(List<String> bdbSubTypeList) {
		String subType;
		try {
			if (bdbSubTypeList.size() == 1) {
				subType = bdbSubTypeList.get(0);
			} else {
				subType = bdbSubTypeList.get(0) + " " + bdbSubTypeList.get(1);
			}
			return subType;
		} catch (Exception e) {
			return null;
		}
	}

}
