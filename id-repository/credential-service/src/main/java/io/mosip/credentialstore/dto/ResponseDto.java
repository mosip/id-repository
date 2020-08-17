package io.mosip.credentialstore.dto;

import java.util.List;


import lombok.Data;

@Data
public class ResponseDto {
	/** The status. */
	private String status;

	/** The identity. */
	private Object identity;

	/** The documents. */
	private List<DocumentDto> documents;
}
