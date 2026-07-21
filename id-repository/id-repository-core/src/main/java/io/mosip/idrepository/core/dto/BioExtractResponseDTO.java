package io.mosip.idrepository.core.dto;

import java.util.List;

import io.mosip.kernel.biometrics.entities.BIR;
import lombok.Data;

/**
 * Response containing biometric records after format extraction.
 *
 * <p>
 * Holds the {@link BIR} list produced by the biometric SDK when converting
 * stored or inbound biometrics to the formats requested in
 * {@link BioExtractRequestDTO}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity processing that returns extracted templates to callers</li>
 *   <li>Credential issuance paths that need normalized biometric payloads</li>
 * </ul>
 *
 * @see BioExtractRequestDTO
 */
@Data
public class BioExtractResponseDTO {

	/** Biometric records converted to the requested extraction formats. */
	private List<BIR> extractedBiometrics;
}
