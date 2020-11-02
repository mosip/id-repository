package io.mosip.credentialstore.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PartnerCertDownloadResponeDto {
    
    /**
	 * Partner Certificate Data.
	 */
	private String certificateData;

	/**
	 * Response timestamp.
	 */
	private LocalDateTime timestamp;
}