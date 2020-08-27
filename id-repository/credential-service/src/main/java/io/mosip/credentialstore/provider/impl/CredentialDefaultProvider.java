package io.mosip.credentialstore.provider.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.Proof;
import io.mosip.credentialstore.dto.VerifiableCredential;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.kernel.core.util.DateUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class CredentialDefaultProvider.
 * 
 * @author Sowmya
 */
@Component
public class CredentialDefaultProvider implements CredentialProvider {

	/** The environment. */
	@Autowired
	private Environment environment;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.vc.datetime.pattern";
	
	
	/** The digital signature util. */
	@Autowired
	DigitalSignatureUtil digitalSignatureUtil;
	
	/** The utilities. */
	@Autowired
    Utilities utilities;
	
	/** The encryption util. */
	@Autowired
	EncryptionUtil encryptionUtil;


	/* (non-Javadoc)
	 * @see io.mosip.credentialstore.provider.CredentialProvider#getFormattedCredentialData(java.util.Map, io.mosip.idrepository.core.dto.CredentialServiceRequestDto, java.util.Map)
	 */
	@Override
	public DataProviderResponse getFormattedCredentialData(	Map<String,Boolean> encryptMap,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException {
      // TODO pin based encryption added for this  need to check for removal 
		DataProviderResponse dataProviderResponse=null;
		try {
			
			String pin = "";
			if (credentialServiceRequestDto.isEncrypt()) {
				pin = credentialServiceRequestDto.getEncryptionKey();
			}
			if (pin.isEmpty())
				pin = utilities.generatePin();
			 Map<String, Object> formattedMap=new HashMap<>();
             
			 for (Map.Entry<String,Object> entry : sharableAttributeMap.entrySet()) {
				    String key=entry.getKey();
					Object value = entry.getValue();
					if (encryptMap.get(key)) {
						String encryptedValue=encryptionUtil.encryptDataWithPin(value.toString(), pin);
						formattedMap.put(key, encryptedValue);
					} else {
						formattedMap.put(key, value);
					}
					

			}
			VerifiableCredential vc = new VerifiableCredential();

			String credentialId = utilities.generateId();
			vc.setId(credentialId);
			vc.setCredentialSubject(formattedMap);

			String date = DateUtils.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN));
			vc.setIssuanceDate(date);
			Proof proof=new Proof();
			String data = JsonUtil.objectMapperObjectToJson(sharableAttributeMap);
			proof.setJws(digitalSignatureUtil.sign(data.getBytes()));
			proof.setCreated(date);
			proof.setType("DigitalSignature");
			vc.setProof(proof);
			String VcData = JsonUtil.objectMapperObjectToJson(vc);
			
			
			dataProviderResponse=new DataProviderResponse();
			dataProviderResponse.setFormattedData(VcData.getBytes());
			dataProviderResponse.setCredentialId(credentialId);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(environment.getProperty(DATETIME_PATTERN));
			LocalDateTime issuanceDate = LocalDateTime
					.parse(date,format);
			dataProviderResponse.setIssuanceDate(issuanceDate);
			Map<String,Object> additionalData=credentialServiceRequestDto.getAdditionalData();
			additionalData.put("encryptionPin", pin);
			credentialServiceRequestDto.setAdditionalData(additionalData);
			return dataProviderResponse;
		} catch (IOException e) {
			throw new CredentialFormatterException(e);
		} catch (DataEncryptionFailureException e) {
			throw new CredentialFormatterException(e);
		} catch (ApiNotAccessibleException e) {
			throw new CredentialFormatterException(e);
		} catch (SignatureException e) {
			throw new CredentialFormatterException(e);
		}


	}

	
}
