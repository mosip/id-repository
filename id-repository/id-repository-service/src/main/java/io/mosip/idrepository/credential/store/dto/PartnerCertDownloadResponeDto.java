package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * PMS partner certificate download payload nested in {@link PartnerGetCertificateResponseDto}.
 * <p>
 * Carries the partner's registered X.509 certificate used to encrypt credential data shares
 * before delivery to the partner.
 * </p>
 *
 * @see PartnerGetCertificateResponseDto
 */
@Data
public class PartnerCertDownloadResponeDto {

	/** PEM-encoded partner X.509 certificate for credential payload encryption. */
	private String certificateData;

	/** UTC timestamp when PMS served the certificate download response. */
	private LocalDateTime timestamp;
}
