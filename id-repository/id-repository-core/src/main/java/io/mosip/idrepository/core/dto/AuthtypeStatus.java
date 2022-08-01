package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Manoj SP
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthtypeStatus {
	
	public AuthtypeStatus(String authType, Boolean locked, Map<String, Object> metadata) {
		this.authType = authType;
		this.locked = locked;
		this.metadata = metadata;
	}

	private String authType;
	private String authSubType;
	private Boolean locked;
	private Long unlockForSeconds;
	private String requestId;
	private Map<String, Object> metadata;
}
