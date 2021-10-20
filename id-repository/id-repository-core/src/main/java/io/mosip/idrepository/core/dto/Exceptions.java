package io.mosip.idrepository.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author Manoj SP
 *
 */
@Data
@Builder
public class Exceptions {

	private String type;
	private String subType;
}