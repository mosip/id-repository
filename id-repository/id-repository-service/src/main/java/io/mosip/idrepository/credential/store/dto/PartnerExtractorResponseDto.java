package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PMS partner extraction-policy API response envelope for credential-service.
 * <p>
 * Kernel {@link ResponseWrapper} deserialized by
 * {@link io.mosip.idrepository.credential.store.util.PolicyUtil#getPartnerExtractorFormat(String, String, String)}.
 * {@link #getResponse()} yields {@link PartnerExtractorResponse} with per-attribute extractor rules.
 * </p>
 *
 * @see PartnerExtractorResponse
 * @see io.mosip.idrepository.credential.store.util.PolicyUtil
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartnerExtractorResponseDto extends ResponseWrapper<PartnerExtractorResponse> {

}
