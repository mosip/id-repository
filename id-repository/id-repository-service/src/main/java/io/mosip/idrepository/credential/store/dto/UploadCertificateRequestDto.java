package io.mosip.idrepository.credential.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keymanager partner-certificate upload request body.
 * <p>
 * Registers a partner X.509 certificate under the given application and reference id so
 * cryptomanager can encrypt credential payloads for that partner during issuance.
 * </p>
 *
 * @see KeyManagerUploadCertificateResponseDto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadCertificateRequestDto {

	/** Keymanager application id under which the partner certificate is stored (e.g. credential-service app id). */
	private String applicationId;

	/** Keymanager reference id identifying the partner certificate slot (typically partner id). */
	private String referenceId;

	/** PEM-encoded X.509 partner certificate to register for partner-specific encryption. */
	private String certificateData;

}
