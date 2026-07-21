package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.entities.BIR;
import lombok.Data;

/**
 * Request to extract normalized biometric records (BIR) using configured formats.
 *
 * <p>
 * Carries a map of modality-to-format extraction preferences and the source
 * {@link BIR} list. Used during identity retrieve/update when callers request
 * biometric template conversion before storage or credential issuance.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Driven by query parameters on identity retrieve-by-id (see
 * {@link IdRequestByIdDTO} finger/iris/face extraction formats) and internal
 * biometric SDK integration.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity retrieve/update paths that request template extraction</li>
 *   <li>Biometric SDK adapters producing {@link BioExtractResponseDTO}</li>
 * </ul>
 *
 * @see BioExtractResponseDTO
 * @see IdRequestByIdDTO
 */
@Data
public class BioExtractRequestDTO {

	/** Map of biometric modality to extraction format (for example, finger to ISO). */
	private Map<String, String> extractionFormats;

	/** Raw or stored biometric records to extract templates from. */
	private List<BIR> biometrics;
}
