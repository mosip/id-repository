package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.mosip.idrepository.core.constant.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSub event payload published when an individual's authentication-type lock
 * status changes.
 *
 * <p>
 * Carries a privacy-safe individual correlation ({@link #saltedIdHash} /
 * {@link #tokenId}), the list of affected {@link AuthtypeStatus} entries, and
 * optional temporary-unlock constraints ({@link #expiryTimestamp},
 * {@link #transactionLimit}).
 * </p>
 *
 * <h2>WebSub context</h2>
 * <p>
 * Published by {@code IdRepoWebSubHelper} (and related identity paths) so that
 * ID Authentication and other subscribers can invalidate or refresh cached
 * auth-type lock state without reading id-repo salt tables.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper} — publisher</li>
 *   <li><strong>IDA</strong> — primary subscriber of auth-type status events</li>
 *   <li>Other WebSub clients that track lock/unlock lifecycle</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * Explicitly listed in core IDA compatibility. Do <strong>not</strong> rename
 * this class or change JSON field names without a coordinated IDA release.
 * Lombok {@code @Data} with all-args and no-args constructors supports both
 * programmatic construction and JSON deserialization by subscribers.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.helper.IdRepoWebSubHelper
 * @see AuthtypeStatus
 * @see EventType
 * @see EventModel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTypeStatusEventDTO {

	/** Type of lifecycle event (for example, lock or unlock). */
	private EventType eventType;

	/** Salted hash of the individual identifier for privacy-safe correlation. */
	private String saltedIdHash;

	/** Token identifier associated with the individual, when applicable. */
	private String tokenId;

	/** List of authentication types and their current lock/unlock state. */
	private List<AuthtypeStatus> authTypeStatusList;

	/** Time after which a temporary unlock expires; {@code null} if not time-bound. */
	private LocalDateTime expiryTimestamp;

	/** Maximum number of transactions allowed while temporarily unlocked. */
	private Integer transactionLimit;
}
