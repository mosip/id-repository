package io.mosip.credentialstore.provider.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.entity.UinHashSalt;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.repositary.UinHashSaltRepo;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.util.HMACUtils;



/**
 * The Class IdAuthProvider.
 * 
 * @author Sowmya
 */
@Component
public class IdAuthProvider implements CredentialProvider {
	
	
	@Autowired
    Utilities utilities;
	
	
	@Autowired
	private UinHashSaltRepo<UinHashSalt, Integer> uinHashSaltRepo;
	
	
	@Autowired
	Environment env;

	
	public static final String MODULO_VALUE = "mosip.credential.service.modulo-value";
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.provider.CredentialProvider#
	 * getFormattedCredentialData(io.mosip.credentialstore.dto.
	 * PolicyDetailResponseDto,
	 * io.mosip.credentialstore.dto.CredentialServiceRequestDto, java.util.Map)
	 */
	@Override
	public DataProviderResponse getFormattedCredentialData(PolicyDetailResponseDto policyDetailResponseDto,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException {

		try {
			List<ShareableAttribute> shareableAttributes = policyDetailResponseDto.getPolicies()
					.getShareableAttributes();
		Map<String, Object> formattedMap=new HashMap<>();
		for (ShareableAttribute attribute : shareableAttributes) {
				Object value = sharableAttributeMap.get(attribute.getAttributeName());
				if (attribute.isEncrypted()) {
					// TODO use zero knowledge encryption to encrypt the value then put in map
					if(attribute.getAttributeName().equalsIgnoreCase(CredentialConstants.FACE) ||attribute.getAttributeName().equalsIgnoreCase(CredentialConstants.IRIS) ||attribute.getAttributeName().equalsIgnoreCase(CredentialConstants.FINGER)) {
					     //Call bio zero knowledge encryption
					}else {
						//Call demo zero knowledge encryption
					}
						formattedMap.put(attribute.getAttributeName(), value);
				} else {
					formattedMap.put(attribute.getAttributeName(), value);
				}

		}
			String data = JsonUtil.objectMapperObjectToJson(formattedMap);
			DataProviderResponse dataProviderResponse=new DataProviderResponse();
			dataProviderResponse.setFormattedData(data.getBytes());
			String credentialId = utilities.generateId();
			Map<String,Object> map=retrieveUinHash(credentialServiceRequestDto);
			credentialServiceRequestDto.getAdditionalData().putAll(map);
			dataProviderResponse.setCredentialId(credentialId);
			return dataProviderResponse;
		} catch (IOException e) {
			throw new CredentialFormatterException(e);
		}


	}
	public  Map<String,Object>  retrieveUinHash(CredentialServiceRequestDto credentialServiceRequestDto) {
		Map<String,Object> additionalMap=credentialServiceRequestDto.getAdditionalData();
		Integer moduloValue = env.getProperty(MODULO_VALUE, Integer.class);
		int modResult = (int) (Long.parseLong(credentialServiceRequestDto.getId()) % moduloValue);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		String saltedIdHash= modResult + SPLITTER + HMACUtils.digestAsPlainTextWithSalt(credentialServiceRequestDto.getId().getBytes(), hashSalt.getBytes());
		additionalMap.put("SaltedIdHash", saltedIdHash);
		additionalMap.put("module", moduloValue);
		additionalMap.put("moduleSalt", hashSalt);
		return additionalMap;
	}
}
