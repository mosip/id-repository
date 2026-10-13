package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP request envelope for identity v2 create and update.
 *
 * <p>
 * Used only by {@code POST} and {@code PATCH /idrepository/v1/identity/v2}.
 * The v1 envelope remains {@link IdRequestDTO}.
 * </p>
 *
 * @see RequestV2DTO
 * @see IdRequestDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdRequestV2DTO extends RequestWrapper<RequestV2DTO> {

}
