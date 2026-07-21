package io.mosip.idrepository.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Request body for VID create, update, and restore operations.
 *
 * <p>
 * Binds UIN, desired VID type, and target status submitted to the VID service
 * REST APIs. Jackson {@code @JsonProperty("UIN")} maps the external JSON key —
 * do not rename without coordinating API clients and api-test.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Business body for {@code /idrepository/v1/vid} create, regenerate, deactivate,
 * and restore. Validated against configured VID ids, versions, and allowed
 * statuses ({@code IdRepoValidationMessageHelper}).
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code VidServiceImpl} and {@code VidRequestValidator}</li>
 *   <li>Registration and partner VID clients</li>
 *   <li>api-test VID scenarios</li>
 * </ul>
 *
 * @author Prem kumar
 * @see VidResponseDTO
 * @see VidPolicy
 * @see io.mosip.idrepository.vid.service.impl.VidServiceImpl
 */
@Data
public class VidRequestDTO {

	/** Target lifecycle status (for example, ACTIVE, INVALIDATED, RESTORED). */
	private String vidStatus;

	/** VID type code as defined in the VID policy (for example, Perpetual). */
	private String vidType;

	/** UIN of the individual; JSON key is {@code UIN}. */
	@JsonProperty("UIN")
	private String uin;
}
