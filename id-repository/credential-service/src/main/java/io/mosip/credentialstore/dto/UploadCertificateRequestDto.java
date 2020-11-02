package io.mosip.credentialstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadCertificateRequestDto {

	private String applicationId;
	
	
    private String referenceId;

	
	private String certificateData;

}
