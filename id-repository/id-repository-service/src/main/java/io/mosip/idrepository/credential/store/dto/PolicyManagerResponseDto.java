package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PMS policy manager API response wrapper for partner credential-type policy.
 * <p>
 * {@code response} contains {@link PartnerCredentialTypePolicyDto} fetched during issuance.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.util.PolicyUtil
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyManagerResponseDto extends ResponseWrapper<PartnerCredentialTypePolicyDto> {

}
