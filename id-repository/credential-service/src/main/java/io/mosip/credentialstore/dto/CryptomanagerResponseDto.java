package io.mosip.credentialstore.dto;



import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author Sowmya
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CryptomanagerResponseDto extends ResponseWrapper<EncryptResponseDto> {

}
