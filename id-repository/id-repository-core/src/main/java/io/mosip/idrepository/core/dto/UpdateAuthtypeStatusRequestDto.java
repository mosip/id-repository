package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class UpdateAuthtypeStatusRequestDto {
	private String id;
	private String version;
	private String requestTime;
	private boolean consentObtained;
	private String individualId;
	private String individualIdType;
	private List<AuthTypeDTO> request;

}
