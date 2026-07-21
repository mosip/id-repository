package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Flags indicating which authentication modalities are enabled for an individual.
 *
 * <p>
 * Used in identity request/response payloads to express supported auth types
 * (demographic, biometric, OTP, PIN). Set a flag to {@code true} when the
 * corresponding modality is active for the individual.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Appears in identity add/update/retrieve JSON under the active ID schema when
 * auth-type enablement is part of the identity document. Distinct from
 * {@link AuthtypeStatus}, which models lock/unlock state rather than enablement.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity request/response builders and validators</li>
 *   <li>Downstream auth modules reading modality flags from identity JSON</li>
 * </ul>
 *
 * @author Rakesh Roshan
 * @author Dinesh Karuppiah.T
 * @see BaseRequestResponseDTO
 * @see AuthtypeStatus
 */
@Data
public class AuthTypeDTO {

	/** {@code true} when demographic (demo) authentication is enabled. */
	private boolean demo;

	/** {@code true} when biometric authentication is enabled. */
	private boolean bio;

	/** {@code true} when one-time password (OTP) authentication is enabled. */
	private boolean otp;

	/** {@code true} when PIN-based authentication is enabled. */
	private boolean pin;
}
