package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * The Class AuthTypeStatusDto.
 *
 * @author Manoj SP
 */
@Data
public class AuthTypeStatusDto {
	
	/** The individual id. */
	private String individualId;
	
	/** The individual id type. */
	private String individualIdType;
	
	/** The request. */
	private List<AuthtypeStatus> request;

}
