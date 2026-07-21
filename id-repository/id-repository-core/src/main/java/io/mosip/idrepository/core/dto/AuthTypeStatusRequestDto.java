package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * REST request body for updating or retrieving authentication-type lock status
 * for an individual.
 *
 * <p>
 * Combines MOSIP standard envelope fields ({@link #id}, {@link #version},
 * {@link #requestTime}) with individual identification and a list of
 * {@link AuthtypeStatus} operations. Successful updates may trigger
 * {@link AuthTypeStatusEventDTO} WebSub publication for IDA.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Used by identity auth-type status REST APIs under
 * {@code /idrepository/v1/identity}. Consent and individual id type are
 * validated at the service layer before applying lock/unlock.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity auth-type status controllers and validators</li>
 *   <li>WebSub helpers that map {@link #request} into event payloads</li>
 * </ul>
 *
 * @author Manoj SP
 * @see AuthtypeStatus
 * @see AuthtypeResponseDto
 * @see AuthTypeStatusEventDTO
 */
@Data
public class AuthTypeStatusRequestDto {

	/** MOSIP request identifier (UUID). */
	private String id;

	/** API version string (for example, {@code 1.0}). */
	private String version;

	/** ISO-8601 timestamp when the request was created. */
	private String requestTime;

	/** {@code true} when the individual has consented to the auth-type change. */
	private boolean consentObtained;

	/** Individual identifier (UIN, VID, or handle value). */
	private String individualId;

	/** Type of {@link #individualId} (for example, UIN or VID). */
	private String individualIdType;

	/** List of authentication types and desired lock/unlock operations. */
	private List<AuthtypeStatus> request;
}
