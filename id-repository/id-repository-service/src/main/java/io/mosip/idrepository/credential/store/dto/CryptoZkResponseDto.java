package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Keymanager ZK-encrypt API response envelope for IdAuth credential attributes.
 * <p>
 * Kernel {@link ResponseWrapper} deserialized by
 * {@link io.mosip.idrepository.credential.store.util.EncryptionUtil} when applying
 * zero-knowledge encryption to sharable demographic/biometric fields. {@link #getResponse()}
 * yields {@link EncryptZkResponseDto} with protected attribute values and session key material.
 * </p>
 *
 * @see EncryptZkResponseDto
 * @see io.mosip.idrepository.credential.store.util.EncryptionUtil
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CryptoZkResponseDto extends ResponseWrapper<EncryptZkResponseDto> {

}
