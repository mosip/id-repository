package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query parameters for identity retrieve-by-id operations with optional biometric extraction.
 *
 * <p>
 * Binds individual id, id type, retrieve type, and per-modality extraction
 * formats used when retrieving identity with template conversion.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Maps to retrieve-by-id query parameters on {@code /idrepository/v1/identity}.
 * Extraction format fields feed {@link BioExtractRequestDTO} when biometric
 * templates must be converted before return.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code IdRepoServiceImpl} retrieve paths</li>
 *   <li>Credential pipeline identity retrieve (in-process or HTTP)</li>
 *   <li>IDA-related retrieve callers that need specific bio formats</li>
 * </ul>
 *
 * @see BioExtractRequestDTO
 * @see io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdRequestByIdDTO {

	/** Individual identifier (UIN, VID, or handle depending on {@link #idType}). */
	private String id;

	/** Retrieve operation type (for example, demo, bio, all). */
	private String type;

	/** Identifier type discriminator (UIN, VID, etc.). */
	private String idType;

	/** Fingerprint template extraction format requested in the response. */
	private String fingerExtractionFormat;

	/** Iris template extraction format requested in the response. */
	private String irisExtractionFormat;

	/** Face template extraction format requested in the response. */
	private String faceExtractionFormat;
}
