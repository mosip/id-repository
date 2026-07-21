package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.http.ResponseWrapper;

/**
 * MOSIP standard response wrapper for authentication-type status queries.
 *
 * <p>
 * The response body maps individual identifiers to lists of {@link AuthtypeStatus}
 * entries. Extends kernel {@link ResponseWrapper} so envelope fields
 * ({@code id}, {@code version}, {@code responsetime}, {@code errors}) are
 * inherited without additional Lombok annotations.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Returned by identity auth-type status retrieve endpoints. Pair with
 * {@link AuthTypeStatusRequestDto} for update flows and
 * {@link AuthTypeStatusEventDTO} for WebSub notifications of the same state.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity auth-type status controllers</li>
 *   <li>Clients polling lock state for an individual</li>
 *   <li>IDA-related flows that also consume {@link AuthtypeStatus} via WebSub</li>
 * </ul>
 *
 * @author Dinesh Karuppiah.T
 * @see AuthtypeStatus
 * @see AuthTypeStatusRequestDto
 * @see AuthTypeStatusEventDTO
 */
public class AuthtypeResponseDto extends ResponseWrapper<Map<String, List<AuthtypeStatus>>> {

}
