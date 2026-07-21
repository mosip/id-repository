package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes the lock state of a single authentication type or sub-type for an individual.
 *
 * <p>
 * Used in auth-type lock/unlock REST APIs and as elements of
 * {@link AuthTypeStatusEventDTO} WebSub payloads. Each instance captures whether
 * a modality (demo, bio, OTP, PIN, etc.) is locked, optional temporary unlock
 * duration, and free-form metadata.
 * </p>
 *
 * <h2>API / WebSub context</h2>
 * <ul>
 *   <li>REST: nested in {@link AuthTypeStatusRequestDto#getRequest()} and
 *       {@link AuthtypeResponseDto} response maps</li>
 *   <li>WebSub: listed under {@link AuthTypeStatusEventDTO#getAuthTypeStatusList()}
 *       when lock status changes are published</li>
 * </ul>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity auth-type status controllers and services</li>
 *   <li>{@code IdRepoWebSubHelper} when building auth-type status events</li>
 *   <li><strong>IDA</strong> — consumes lock-state fields via WebSub / core API</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * Listed in core IDA compatibility as a referenced DTO. Do not rename the class
 * or change JSON property names ({@code authType}, {@code authSubType},
 * {@code locked}, {@code unlockForSeconds}, {@code requestId}, {@code metadata})
 * without coordinating an IDA release. Lombok {@code @Data} generates accessors
 * for serialization; the three-argument constructor supports quick construction
 * when only auth type, lock flag, and metadata are required.
 * </p>
 *
 * @author Manoj SP
 * @see AuthTypeStatusRequestDto
 * @see AuthTypeStatusEventDTO
 * @see AuthtypeResponseDto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthtypeStatus {

	/**
	 * Convenience constructor for auth-type updates with metadata only.
	 *
	 * @param authType authentication type code (for example, {@code demo}, {@code bio})
	 * @param locked   {@code true} when the auth type is locked
	 * @param metadata optional key-value metadata for the lock operation
	 */
	public AuthtypeStatus(String authType, Boolean locked, Map<String, Object> metadata) {
		this.authType = authType;
		this.locked = locked;
		this.metadata = metadata;
	}

	/** Primary authentication type code. */
	private String authType;

	/** Biometric or demo sub-type, when the auth type has sub-categories. */
	private String authSubType;

	/** {@code true} when this auth type is locked for the individual. */
	private Boolean locked;

	/** Duration in seconds for a temporary unlock; {@code null} for indefinite lock. */
	private Long unlockForSeconds;

	/** Correlation identifier linking this status change to an originating request. */
	private String requestId;

	/** Additional attributes (for example, reason codes) for the lock operation. */
	private Map<String, Object> metadata;
}
