package io.mosip.credentialstore.dto;

import java.time.LocalDateTime;


import lombok.Data;

@Data
public class UploadCertificateResponseDto {
    
    /**
	 * Status of upload certificate.
	 */
	private String status;

	/**
	 * Status of upload certificate.
	 */
	private LocalDateTime timestamp;
}
