package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdRequestByIdDTO {
	private String id;
	private String type;
	private String idType;
	private String fingerExtractionFormat;
	private String irisExtractionFormat;
	private String faceExtractionFormat;
}
