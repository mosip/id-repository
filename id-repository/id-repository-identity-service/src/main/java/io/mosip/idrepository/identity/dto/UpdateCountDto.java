package io.mosip.idrepository.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The remaining update count dto.
 * 
 * @author Neha Farheen
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateCountDto {
	private String attributeName;
	private int noOfUpdatesLeft;
}
