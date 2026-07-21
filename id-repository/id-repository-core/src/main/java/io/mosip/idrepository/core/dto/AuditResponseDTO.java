package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned by the MOSIP audit manager after an audit submission.
 *
 * <p>
 * Minimal acknowledgement used by {@link io.mosip.idrepository.core.helper.AuditHelper}
 * to verify that an audit record was accepted. Some audit-manager versions may
 * omit {@link #status}; treat {@code null} as inconclusive rather than failure.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code AuditHelper} — interprets acceptance after POST</li>
 *   <li>Unit tests that stub audit-manager responses</li>
 * </ul>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.helper.AuditHelper
 * @see AuditRequestDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponseDTO {

	/** {@code true} when the audit service persisted the event successfully; may be absent/null from some audit-manager versions. */
	private Boolean status;
}
