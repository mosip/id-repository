package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for a single Virtual ID (VID) associated with an individual.
 *
 * <p>
 * Exposes VID value, type, expiry, generation time, transaction limits, and
 * hashed attributes returned by VID retrieve and credential pipeline flows.
 * Persisted VID data lives in {@code mosip_idmap}; this DTO is the API/pipeline
 * projection.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Element of {@link VidsInfosDTO} / {@link VidInfoResponsDTO} for
 * {@code /idrepository/v1/vid} retrieve-by-UIN. Also consumed when credential
 * issuance needs VID metadata and hash attributes.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code VidServiceImpl}</li>
 *   <li>{@code CredentialServiceManager}</li>
 *   <li>Partner / registration clients listing VIDs</li>
 * </ul>
 *
 * @author Manoj SP
 * @see VidsInfosDTO
 * @see VidResponseDTO
 * @see io.mosip.idrepository.vid.service.impl.VidServiceImpl
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VidInfoDTO {

	/** Decrypted or masked Virtual ID value. */
	private String vid;

	/** VID type code (for example, Perpetual, Temporary). */
	private String vidType;

	/** Timestamp when the VID expires; {@code null} for non-expiring types. */
	private LocalDateTime expiryTimestamp;

	/** Timestamp when the VID was generated. */
	private LocalDateTime genratedOnTimestamp;

	/** Maximum allowed authentication transactions for this VID. */
	private Integer transactionLimit;

	/** Attribute name to hashed-value map for credential or audit use. */
	private Map<String, String> hashAttributes;
}
