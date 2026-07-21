package io.mosip.idrepository.core.dto;

import java.util.List;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP {@link ResponseWrapper} for a list of {@link VidInfoDTO} records.
 *
 * <p>
 * Legacy response type retained for backward-compatible VID retrieve APIs.
 * Prefer {@link VidsInfosDTO} for new integrations that declare an explicit
 * {@code response} field.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Historical envelope for VID list responses under {@code /idrepository/v1/vid}.
 * Class name retains the historical spelling {@code Respons}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Legacy VID retrieve clients</li>
 *   <li>Code paths still typed against this wrapper</li>
 * </ul>
 *
 * @author Manoj SP
 * @see VidInfoDTO
 * @see VidsInfosDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VidInfoResponsDTO extends ResponseWrapper<List<VidInfoDTO>> {

}
