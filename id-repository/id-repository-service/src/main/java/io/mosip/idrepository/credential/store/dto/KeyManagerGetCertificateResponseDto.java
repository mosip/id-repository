package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;

/**
 * Keymanager get-certificate API response envelope for credential-service.
 * <p>
 * Kernel {@link ResponseWrapper} whose {@link #getResponse()} carries
 * {@link KeyPairGenerateResponseDto} with the X.509 certificate and validity metadata
 * for a requested application/reference key pair.
 * </p>
 *
 * @see KeyPairGenerateResponseDto
 */
public class KeyManagerGetCertificateResponseDto extends ResponseWrapper<KeyPairGenerateResponseDto> {

}
