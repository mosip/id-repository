package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Keymanager partner-certificate upload outcome nested in {@link KeyManagerUploadCertificateResponseDto}.
 * <p>
 * Confirms whether the partner X.509 material was accepted and when keymanager recorded the change.
 * </p>
 *
 * @see UploadCertificateRequestDto
 * @see KeyManagerUploadCertificateResponseDto
 */
@Data
public class UploadCertificateResponseDto {

	/** Keymanager status for the upload operation (e.g. success or failure code). */
	private String status;

	/** UTC timestamp when keymanager processed the certificate upload. */
	private LocalDateTime timestamp;
}
