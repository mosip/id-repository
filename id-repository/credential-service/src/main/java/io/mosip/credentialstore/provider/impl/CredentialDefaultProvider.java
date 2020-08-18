package io.mosip.credentialstore.provider.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.JsonUtil;
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
	private Environment environment;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.vc.datetime.pattern";
	
	
	@Autowired
	DigitalSignatureUtil digitalSignatureUtil;
	
	@Autowired
    Utilities utilities;

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
		// encryption is not needed for default provider
		// TODO PIN based encryption if it needed MOSIP-8595
		DataProviderResponse dataProviderResponse;
		try {

			VerifiableCredential vc = new VerifiableCredential();
			// TODO where we need to store this credential id so that document comes back or
			// need to share request id only?
			String credentialId = utilities.generateId();
			vc.setId(credentialId);
			vc.setCredentialSubject(sharableAttributeMap);

			String date = DateUtils.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN));
			vc.setIssuanceDate(date);

			String VcData = JsonUtil.objectMapperObjectToJson(vc);
			dataProviderResponse=new DataProviderResponse();
			dataProviderResponse.setFormattedData(VcData.getBytes());
			dataProviderResponse.setCredentialId(credentialId);
			dataProviderResponse.setIssuanceDate(date);
			String data = JsonUtil.objectMapperObjectToJson(sharableAttributeMap);
			dataProviderResponse.setSignature(digitalSignatureUtil.sign(data.getBytes()));
			return dataProviderResponse;
		} catch (IOException e) {
			throw new CredentialFormatterException(e);
		}catch(SignatureException e) {
			throw new CredentialFormatterException(e);
		} catch (ApiNotAccessibleException e) {
			throw new CredentialFormatterException(e);
		}

	}

	/**
	 * Generate id.
	 *
	 * @return the string
	 */
	private String generateId() {
		return UUID.randomUUID().toString();
	}
}
