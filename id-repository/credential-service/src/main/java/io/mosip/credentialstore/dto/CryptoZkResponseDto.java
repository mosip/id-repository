package io.mosip.credentialstore.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
public class CryptoZkResponseDto extends ResponseWrapper<EncryptZkResponseDto>{

}
