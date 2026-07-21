package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * Shared payload fragment for identity add, update, and retrieve operations.
 *
 * <p>
 * Holds lifecycle {@link #status}, the schema-shaped {@link #identity} object,
 * optional {@link #documents}, and {@link #verifiedAttributes}. Extended by
 * {@link RequestDTO} (inbound) and {@link ResponseDTO} (outbound).
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Nested inside {@link IdRequestDTO} / {@link IdResponseDTO} for
 * {@code /idrepository/v1/identity} create, update, and retrieve. The
 * {@link #identity} object shape follows the active ID schema version.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link RequestDTO}, {@link ResponseDTO}</li>
 *   <li>Identity validators and {@code IdRepoService} implementations</li>
 *   <li>Credential pipeline when retrieving identity for issuance</li>
 * </ul>
 *
 * @author Manoj SP
 * @see RequestDTO
 * @see ResponseDTO
 * @see DocumentsDTO
 */
@Data
public class BaseRequestResponseDTO {

	/** Lifecycle status of the identity record (for example, ACTIVATED). */
	private String status;

	/** Identity JSON object keyed by schema field names. */
	private Object identity;

	/** Supporting documents (proof of address, identity, etc.) attached to the record. */
	private List<DocumentsDTO> documents;

	/** Attribute names that have been verified for the individual. */
	private List<String> verifiedAttributes;
}
