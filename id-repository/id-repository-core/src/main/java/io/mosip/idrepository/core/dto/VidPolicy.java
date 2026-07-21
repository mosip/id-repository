package io.mosip.idrepository.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * VID lifecycle policy loaded from the configured policy JSON file.
 *
 * <p>
 * Defines validity duration, transaction limits, instance caps, and auto-restore
 * behaviour for each VID type. Jackson {@code @JsonProperty} maps external JSON
 * keys ({@code transactionsAllowed}, {@code instancesAllowed}) to Java field
 * names — do not rename those annotations without updating policy files.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Loaded by {@code VidPolicyProvider} from
 * {@code mosip.idrepo.vid.policy-file-url} (and schema URL). Applied by
 * {@code VidServiceImpl} during create, update, and restore.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code VidPolicyProvider}</li>
 *   <li>{@code VidServiceImpl}</li>
 * </ul>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.vid.provider.VidPolicyProvider
 * @see io.mosip.idrepository.vid.service.impl.VidServiceImpl
 * @see VidRequestDTO
 */
@Data
public class VidPolicy {

	/** Maximum validity of the VID in minutes from generation. */
	private Integer validForInMinutes;

	/** Maximum authentication transactions allowed before the VID is blocked. */
	@JsonProperty("transactionsAllowed")
	private Integer allowedTransactions;

	/** Maximum concurrent active VID instances of this type per UIN. */
	@JsonProperty("instancesAllowed")
	private Integer allowedInstances;

	/** {@code true} when an expired or consumed VID may be automatically restored. */
	private Boolean autoRestoreAllowed;

	/** Action that triggers VID restoration (for example, authentication failure). */
	private String restoreOnAction;
}
