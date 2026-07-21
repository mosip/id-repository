package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP standard request wrapper for ID Repository identity REST APIs.
 *
 * <p>
 * Extends kernel {@link RequestWrapper} with {@link RequestDTO} as the typed
 * {@code request} body. Envelope fields ({@code id}, {@code version},
 * {@code requesttime}) are validated by identity validators before the
 * business payload is applied.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Inbound body for {@code /idrepository/v1/identity} create, update, and draft
 * operations. Pair with {@link IdResponseDTO} for responses.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity controllers and {@code IdRequestValidator}</li>
 *   <li>{@code IdRepoService} add/update implementations</li>
 *   <li>api-test identity scenarios</li>
 * </ul>
 *
 * @author Manoj SP
 * @see RequestDTO
 * @see IdResponseDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdRequestDTO extends RequestWrapper<RequestDTO> {

}
