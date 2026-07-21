package io.mosip.idrepository.credential.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cryptomanager PIN-based encrypt/decrypt request for partner credential attributes.
 * <p>
 * Used when policy marks attributes as encrypted with the partner-supplied encryption key (PIN).
 * </p>
 *
 * @author Mahammed Taheer
 * @since 1.2.0
 * @see io.mosip.idrepository.credential.store.util.EncryptionUtil#encryptDataWithPin
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CryptoWithPinRequestDto {

	/** Plaintext attribute value or JSON fragment to encrypt. */
	private String data;

	/** Partner encryption key (PIN) from the credential issuance request. */
	private String userPin;
}
