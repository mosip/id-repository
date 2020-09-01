package io.mosip.idrepository.core.dto;

import java.io.Serializable;

import lombok.Data;


@Data
public class BaseRestResponseDTO implements Serializable {
	
	private static final long serialVersionUID = 4246582347420843195L;

	/** The id. */
	private String id;
	
	/** The ver. */
	private String version;
	
	/** The timestamp. */
	private String responsetime;

}
