package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;

/**
 * Keymanager upload-certificate API response envelope for credential-service.
 * <p>
 * Kernel {@link ResponseWrapper} whose {@link #getResponse()} carries
 * {@link UploadCertificateResponseDto} confirming partner certificate registration.
 * </p>
 *
 * @see UploadCertificateRequestDto
 * @see UploadCertificateResponseDto
 */
public class KeyManagerUploadCertificateResponseDto extends ResponseWrapper<UploadCertificateResponseDto> {

}
