package io.mosip.idrepository.credential.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested cryptomanager encrypt payload inside {@link CryptomanagerResponseDto}.
 * <p>
 * Holds the partner-certificate-encrypted ciphertext produced when credential-service
 * encrypts sharable identity attributes via the MOSIP cryptomanager service.
 * Aligns with upstream {@code io.mosip.credentialstore.dto.EncryptResponseDto}.
 * </p>
 *
 * @see CryptomanagerResponseDto
 * @see io.mosip.idrepository.credential.store.util.EncryptionUtil
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncryptResponseDto {

	/** Base64-encoded ciphertext encrypted with the partner's X.509 certificate. */
	private String data;

}
