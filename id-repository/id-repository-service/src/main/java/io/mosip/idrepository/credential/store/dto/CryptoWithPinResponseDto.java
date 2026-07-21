package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Keymanager PIN-encrypt API response wrapper.
 *
 * @see EncryptWithPinResponseDto
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CryptoWithPinResponseDto extends ResponseWrapper<EncryptWithPinResponseDto> {

}
