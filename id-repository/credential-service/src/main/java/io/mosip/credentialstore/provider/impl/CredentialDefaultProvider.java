package io.mosip.credentialstore.provider.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialDefaultProvider.
 * 
 * @author Sowmya
 */
@Component
public class CredentialDefaultProvider implements CredentialProvider {

	/** The environment. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The utilities. */
	@Autowired
    Utilities utilities;
	
	/** The encryption util. */
	@Autowired
	EncryptionUtil encryptionUtil;

	@Autowired
	private ObjectMapper mapper;


	/* (non-Javadoc)
	 * @see io.mosip.credentialstore.provider.CredentialProvider#getFormattedCredentialData(java.util.Map, io.mosip.idrepository.core.dto.CredentialServiceRequestDto, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DataProviderResponse getFormattedCredentialData(	Map<String,Boolean> encryptMap,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException {

		DataProviderResponse dataProviderResponse=null;
		try {
			
			String pin = credentialServiceRequestDto.getEncryptionKey();

			 Map<String, Object> formattedMap=new HashMap<>();


			 for (Map.Entry<String,Object> entry : sharableAttributeMap.entrySet()) {
				    String key=entry.getKey();
					Object value = entry.getValue();
				String valueStr = null;
				if (value instanceof String) {
					valueStr = value.toString();
				} else {
					valueStr = mapper.writeValueAsString(value);
				}
					if (encryptMap.get(key)) {
					String encryptedValue = encryptionUtil.encryptDataWithPin(valueStr, pin);
						formattedMap.put(key, encryptedValue);
					} else {
					formattedMap.put(key, valueStr);
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
			json.put(JsonConstants.ID, env.getProperty("mosip.credential.service.format.id"));
			json.put(JsonConstants.TYPE, typeList);
			json.put(JsonConstants.ISSUER, env.getProperty("mosip.credential.service.format.issuer"));
			json.put(JsonConstants.ISSUANCEDATE, DateUtils.formatToISOString(localdatetime));
			json.put(JsonConstants.ISSUEDTO, credentialServiceRequestDto.getIssuer());
			json.put(JsonConstants.CONSENT, "");
			json.put(JsonConstants.CREDENTIALSUBJECT, formattedMap);

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

	
}
