package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * @author M1022006
 *
 */
@Data
public class AuthTypeStatusRequestDto {

	private String individualId;

	private String individualIdType;

	private List<AuthtypeStatus> request;

	private String requestTime;

	private String version;
}
