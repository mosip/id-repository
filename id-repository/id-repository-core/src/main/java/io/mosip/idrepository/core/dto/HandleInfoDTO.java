package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Metadata for a single handle associated with an individual's UIN.
 *
 * <p>
 * Exposes handle value, expiry, generation time, transaction limits, and
 * extension attributes used in credential issuance and identity handle flows.
 * Handles are alternative identifiers (for example, email or phone handle
 * strings) stored in the idrepo handle tables.
 * </p>
 *
 * <h2>API / pipeline context</h2>
 * <p>
 * Returned alongside identity/credential flows that resolve handles for an
 * individual. Distinct from {@link VidInfoDTO}, which describes Virtual IDs in
 * the idmap schema.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialServiceManager} — handle-aware credential issuance</li>
 *   <li>{@code IdRepoService} — handle retrieve/update paths</li>
 *   <li>Identity APIs that expose selected handles</li>
 * </ul>
 *
 * @see VidInfoDTO
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 * @see io.mosip.idrepository.core.spi.IdRepoService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandleInfoDTO {

    /** Handle value (for example, email or phone handle string). */
    private String handle;

    /** Timestamp when the handle expires; {@code null} when non-expiring. */
    private LocalDateTime expiryTimestamp;

    /** Timestamp when the handle was generated or registered. */
    private LocalDateTime genratedOnTimestamp;

    /** Maximum allowed transactions for this handle, when policy applies. */
    private Integer transactionLimit;

    /** Additional handle attributes not covered by standard fields. */
    private Map<String, String> additionalData;
}
