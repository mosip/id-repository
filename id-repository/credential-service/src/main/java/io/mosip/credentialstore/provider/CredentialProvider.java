package io.mosip.credentialstore.provider;

import java.util.Map;


import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;

/**
 * The Interface CredentialProvider.
 * 
 * @author Sowmya
 */
public interface CredentialProvider {

	/**
	 * Gets the formatted credential data.
	 *
	 * @param policyDetailResponseDto     the policy detail response dto
	 * @param credentialServiceRequestDto the credential service request dto
	 * @param sharableAttributeMap        the sharable attribute map
	 * @return the formatted credential data
	 * @throws CredentialFormatterException the credential formatter exception
	 */
	public DataProviderResponse getFormattedCredentialData(PolicyDetailResponseDto policyDetailResponseDto,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException;

}
