package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keymanager JWT signature response payload.
 *
 * @author Mahammed Taheer
 * @since 1.2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JWTSignatureResponseDto {

	/** Compact JWS over the signed credential or VC document. */
	private String jwtSignedData;

	/** Server timestamp when the signature was produced. */
	private LocalDateTime timestamp;
}
