package io.mosip.idrepository.core.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard MOSIP error entry included in REST error responses.
 *
 * <p>
 * Pair of machine-readable {@link #errorCode} and human-readable
 * {@link #message}. Used in credential and other REST DTOs that do not use the
 * kernel {@code ResponseWrapper} error list exclusively. Implements
 * {@link Serializable} for propagation in response DTOs.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Appears in {@link CredentialIssueResponseDto} and
 * {@link CredentialServiceResponseDto} when validation or processing fails.
 * Some codes (for example, {@code IDR-CRG-009}) are matched by IDA — keep codes
 * and messages stable for those contracts.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request and credential-store error paths</li>
 *   <li>Service layers constructing errors via the all-args constructor</li>
 *   <li>IDA and partner clients matching known error codes</li>
 * </ul>
 *
 * @see CredentialIssueResponseDto
 * @see CredentialServiceResponseDto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDTO implements Serializable {

	private static final long serialVersionUID = 2452990684776944908L;

	/** Machine-readable error code (for example, IDR-003). */
	private String errorCode;

	/** Human-readable description of the failure. */
	private String message;
}
