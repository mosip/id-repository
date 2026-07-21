package io.mosip.idrepository.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Response body returned after VID create, update, or restore operations.
 *
 * <p>
 * Exposes decrypted UIN, generated VID, status, and any restored VID details
 * produced by auto-restore policy. Jackson {@code @JsonProperty} maps
 * {@code UIN} and {@code VID} JSON keys — keep those annotations stable for
 * external clients.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Outbound body for {@code /idrepository/v1/vid} create/update/restore.
 * When auto-restore applies, {@link #restoredVid} may nest another
 * {@link VidResponseDTO} describing the restored identifier.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code VidServiceImpl}</li>
 *   <li>Registration and partner VID clients</li>
 *   <li>api-test VID scenarios</li>
 * </ul>
 *
 * @author Prem Kumar
 * @see VidRequestDTO
 * @see VidInfoDTO
 * @see io.mosip.idrepository.vid.service.impl.VidServiceImpl
 */
@Data
public class VidResponseDTO {

	/** Decrypted UIN of the individual; JSON key is {@code UIN}. */
	@JsonProperty("UIN")
	private String uin;

	/** Generated or updated Virtual ID; JSON key is {@code VID}. */
	@JsonProperty("VID")
	private String vid;

	/** Current lifecycle status of the VID. */
	private String vidStatus;

	/** Details of a VID restored during an auto-restore policy action, if applicable. */
	private VidResponseDTO restoredVid;
}
