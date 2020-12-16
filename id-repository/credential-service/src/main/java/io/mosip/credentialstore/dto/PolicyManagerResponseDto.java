package io.mosip.credentialstore.dto;



import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyManagerResponseDto  extends ResponseWrapper<PartnerCredentialTypePolicyDto> {

}
