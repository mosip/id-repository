package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document attachment entry in identity request or response payloads.
 *
 * <p>
 * Represents one proof document (category + value) in the
 * {@link BaseRequestResponseDTO#getDocuments()} list for identity add, update,
 * and retrieve.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Part of {@code /idrepository/v1/identity} request/response JSON. Category
 * codes typically align with ID schema document types (POA, POI, etc.).
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity validators and {@code IdRepoService}</li>
 *   <li>{@link IdentityMapping.Documents} path resolution for profiling</li>
 * </ul>
 *
 * @author Manoj SP
 * @see BaseRequestResponseDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentsDTO {

	/** Document category or type (for example, POA, POI). */
	private String category;

	/** Base64-encoded document content or reference value. */
	private String value;
}
