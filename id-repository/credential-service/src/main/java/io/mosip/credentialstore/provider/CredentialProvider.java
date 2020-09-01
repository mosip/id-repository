package io.mosip.credentialstore.provider;

import java.util.Map;

import org.springframework.stereotype.Service;

import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;

// TODO: Auto-generated Javadoc
/**
 * The Interface CredentialProvider.
 * 
 * @author Sowmya
 */
@Service
public interface CredentialProvider {


	/**
	 * Gets the formatted credential data.
	 *
	 * @param encryptMap the encrypt map
	 * @param credentialServiceRequestDto the credential service request dto
	 * @param sharableAttributeMap the sharable attribute map
	 * @return the formatted credential data
	 * @throws CredentialFormatterException the credential formatter exception
	 */
	public DataProviderResponse getFormattedCredentialData(	Map<String,Boolean> encryptMap,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException;

}
