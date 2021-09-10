package io.mosip.idrepository.core.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Exceptions {

	private String type;
	private String subType;
}
