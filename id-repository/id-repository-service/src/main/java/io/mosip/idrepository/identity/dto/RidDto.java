package io.mosip.idrepository.identity.dto;

import lombok.Data;

/**
 * API response carrying a resident registration id (RID).
 *
 * @author Ritik Jain
 */
@Data
public class RidDto {

	/** Registration id (RID) returned to the client. */
	private String rid;

}