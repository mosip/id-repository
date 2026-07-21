package io.mosip.idrepository.credential.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cryptomanager PIN-encryption response for a single credential attribute.
 *
 * @author Mahammed Taheer
 * @since 1.2.0
 * @see CryptoWithPinRequestDto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncryptWithPinResponseDto {

	/** Base64-encoded ciphertext produced by cryptomanager PIN encrypt API. */
	private String data;
}
