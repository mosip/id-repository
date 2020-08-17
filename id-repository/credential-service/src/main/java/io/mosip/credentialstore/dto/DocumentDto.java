package io.mosip.credentialstore.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
	/** The doc type. */
	private String category;

	/** The doc value. */
	private String value;
}
