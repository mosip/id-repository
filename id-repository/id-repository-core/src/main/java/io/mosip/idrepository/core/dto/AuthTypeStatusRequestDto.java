package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * @author M1022006
 *
 */
@Data
public class AuthTypeStatusRequestDto {

	/** The id. */
	private String id;
	
	/** The version. */
	private String version;
	
	/** The request time. */
	private String requestTime;
	
	/** The consent obtained. */
	private boolean consentObtained;
	
	/** The individual id. */
	private String individualId;
	
	/** The individual id type. */
	private String individualIdType;
	
	/** The request. */
	private List<AuthtypeStatus> request;
}
