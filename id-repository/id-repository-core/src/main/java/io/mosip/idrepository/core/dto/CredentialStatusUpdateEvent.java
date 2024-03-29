package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Instantiates a new credential status update event.
 * @author Nagarjuna
 *
 */
@Data
public class CredentialStatusUpdateEvent {
	
	/** The id. */
	private String id;
	
	/** The request id. */
	private String requestId;
	
	/** The status. */
	private String status;
	
	/** The timestamp. */
	private String timestamp;

}
