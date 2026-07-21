package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Keymanager JWT/VC sign API response wrapper.
 *
 * @see JWTSignatureResponseDto
 * @see io.mosip.idrepository.credential.store.util.DigitalSignatureUtil
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SignResponseDto extends ResponseWrapper<JWTSignatureResponseDto> {

}
