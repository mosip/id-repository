package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP standard response wrapper for ID Repository identity REST APIs.
 *
 * <p>
 * Extends kernel {@link ResponseWrapper} with {@link ResponseDTO} as the typed
 * response body containing identity, documents, and status fields.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Outbound envelope for {@code /idrepository/v1/identity} create, update, and
 * retrieve. Kernel envelope fields include {@code id}, {@code version},
 * {@code responsetime}, and {@code errors}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity controllers</li>
 *   <li>Registration and partner clients</li>
 *   <li>Credential pipeline when decoding identity retrieve responses</li>
 * </ul>
 *
 * @author Manoj SP
 * @see ResponseDTO
 * @see IdRequestDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdResponseDTO extends ResponseWrapper<ResponseDTO> {

}
