package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

/**
 * 
 * @author Manoj SP
 *
 */
@Data
public class AuthtypeStatus {

	private String authType;
	private String authSubType;
	private Boolean locked;
	private Long unlockForMinutes;
	private Map<String, Object> metadata;
}
