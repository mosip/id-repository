package io.mosip.idrepository.credential.store.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP cryptomanager encrypt API response envelope for credential-service.
 * <p>
 * Matches upstream {@code io.mosip.credentialstore.dto.CryptomanagerResponseDto}:
 * kernel {@link ResponseWrapper} with nested {@link EncryptResponseDto} in
 * {@link #getResponse()}. Used by {@link io.mosip.idrepository.credential.store.util.EncryptionUtil}
 * when parsing partner-certificate encryption responses.
 * </p>
 * <p>
 * <strong>Not</strong> the plain {@code data}-only DTO from credential-request-generator;
 * credreq crypto uses {@link io.mosip.kernel.core.http.ResponseWrapper} with a map payload in
 * {@link io.mosip.idrepository.credential.request.util.CryptoUtil}.
 * </p>
 *
 * @author Sowmya
 * @see EncryptResponseDto
 * @see io.mosip.idrepository.common.dto.CryptomanagerRequestDto
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CryptomanagerResponseDto extends ResponseWrapper<EncryptResponseDto> {

}
