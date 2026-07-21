package io.mosip.idrepository.credential.store.dto;

/**
 * PMS partner certificate download API response envelope for credential-service.
 * <p>
 * {@link PartnerResponseWrapper} whose {@link PartnerResponseWrapper#getResponse()} carries
 * {@link PartnerCertDownloadResponeDto} with the partner encryption certificate.
 * </p>
 *
 * @see PartnerCertDownloadResponeDto
 * @see PartnerResponseWrapper
 */
public class PartnerGetCertificateResponseDto extends PartnerResponseWrapper<PartnerCertDownloadResponeDto> {

}
