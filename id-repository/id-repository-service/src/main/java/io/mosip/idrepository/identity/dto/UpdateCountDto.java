package io.mosip.idrepository.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Remaining allowed update count for a constrained identity field per partner policy.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateCountDto {
	/** Attribute name. */
	private String attributeName;
	/** No of updates left. */
	private int noOfUpdatesLeft;
}