package io.mosip.credentialstore.provider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyResponseDto;
import io.mosip.credentialstore.dto.Source;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
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
	private Environment env;

	@Autowired
	private ObjectMapper mapper;

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

		DataProviderResponse dataProviderResponse=null;
		try {
			
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
					String encryptedValue = encryptionUtil.encryptDataWithPin(valueStr, pin);
					formattedMap.put(attributeName, encryptedValue);
					protectedAttributes.add(attributeName);
				} else {
					formattedMap.put(attributeName, valueStr);
				}

			}
			String credentialId = utilities.generateId();
			formattedMap.put(JsonConstants.ID,
					env.getProperty("mosip.credential.service.format.credentialsubject.id") + credentialId);

			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

			JSONObject json = new JSONObject();
			List<String> typeList = new ArrayList<>();
			typeList.add(JsonConstants.VERFIABLECREDENTIAL);
			typeList.add(JsonConstants.MOSIPVERFIABLECREDENTIAL);
			typeList.add(JsonConstants.PHILSYSVERFIABLECREDENTIAL);
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

			return dataProviderResponse;
		} catch (DataEncryptionFailureException e) {
			throw new CredentialFormatterException(e);
		} catch (ApiNotAccessibleException e) {
			throw new CredentialFormatterException(e);
		} catch (JsonProcessingException e) {
			throw new CredentialFormatterException(e);
		}

	}

	public Map<AllowedKycDto, Object> prepareSharableAttributes(IdResponseDTO idResponseDto,
			PolicyResponseDto policyResponseDto, CredentialServiceRequestDto credentialServiceRequestDto)
			throws Exception {
		Map<AllowedKycDto, Object> attributesMap = new HashMap<>();
		JSONObject identity = new JSONObject((Map) idResponseDto.getResponse().getIdentity());

		List<AllowedKycDto> sharableAttributeList = policyResponseDto.getPolicies().getShareableAttributes();
		Set<AllowedKycDto> sharableAttributeDemographicKeySet = new HashSet<>();
		Set<AllowedKycDto> sharableAttributeBiometricKeySet = new HashSet<>();
		// TO DO how to handle if this face and finger from user
		if (credentialServiceRequestDto.getSharableAttributes() != null
				&& !credentialServiceRequestDto.getSharableAttributes().isEmpty()) {
			credentialServiceRequestDto.getSharableAttributes().forEach(attribute -> {
				AllowedKycDto allowedKycDto = new AllowedKycDto();
				List<Source> sourceList = new ArrayList<>();
				Source source = new Source();
				source.setAttribute(attribute);
				sourceList.add(source);
				allowedKycDto.setSource(sourceList);
				allowedKycDto.setAttributeName(attribute);
				sharableAttributeDemographicKeySet.add(allowedKycDto);


			});
		}
		sharableAttributeList.forEach(dto -> {
			if (dto.getGroup() == null) {

				sharableAttributeDemographicKeySet.add(dto);

			} else if (dto.getGroup().equalsIgnoreCase(CredentialConstants.CBEFF)) {
				sharableAttributeBiometricKeySet.add(dto);

			}

		});

		for (AllowedKycDto key : sharableAttributeDemographicKeySet) {
			String attribute = key.getSource().get(0).getAttribute();
			Object object = identity.get(attribute);
			if (object != null) {
				attributesMap.put(key, object);
			}
		}
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
			attributesMap.put(key,
					CryptoUtil.encodeBase64(individualBiometricsValue.getBytes()));
			}

		}
		return attributesMap;
	}
}
