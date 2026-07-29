package io.mosip.idrepository.core.dto;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

import lombok.Data;

/**
 * Describes an outbound HTTP/HTTPS call assembled by
 * {@link io.mosip.idrepository.core.builder.RestRequestBuilder} and executed by
 * {@link io.mosip.idrepository.core.helper.RestHelper}.
 *
 * <p>
 * Encapsulates URI, HTTP method, headers, path/query variables, optional body,
 * expected response type, and optional timeout. Used for synchronous REST
 * integration with external MOSIP modules (Key Manager, PMS, Datashare, auth
 * manager, and others).
 * </p>
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>{@code RestRequestBuilder} builds an instance from
 *       {@code RestServicesConstants} / config keys</li>
 *   <li>{@code RestHelper} executes the call (sync or reactive WebClient path)</li>
 *   <li>Callers decode the typed {@link #responseType} result</li>
 * </ol>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link io.mosip.idrepository.core.builder.RestRequestBuilder}</li>
 *   <li>{@link io.mosip.idrepository.core.helper.RestHelper}</li>
 *   <li>Credential, identity, and VID outbound integrations</li>
 *   <li><strong>IDA</strong> — references this DTO via the published core JAR</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * Listed in core IDA compatibility. Keep field names and validation semantics
 * stable; IDA and other consumers construct or inspect this type when sharing
 * REST helper utilities from {@code id-repository-core}.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.builder.RestRequestBuilder
 * @see io.mosip.idrepository.core.helper.RestHelper
 * @see io.mosip.idrepository.core.constant.RestServicesConstants
 */
@Data
public class RestRequestDTO {

	/** Target service URL; validated against a URL pattern. */
	@Pattern(regexp = "<\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>", message = "{mosip.rest.request.uri.message}")
	@NotNull
	private String uri;

	/** Query string parameters appended to the request URI. */
	MultiValueMap<String, String> params;

	/** Path variable substitutions for templated URI segments. */
	Map<String, String> pathVariables;

	/** HTTP verb (GET, POST, PUT, PATCH, DELETE). */
	@NotNull
	private HttpMethod httpMethod;

	/** Serializable request body; {@code null} for bodyless requests. */
	private Object requestBody;

	/** Expected response type for JSON deserialization. */
	@NotNull
	private Class<?> responseType;

	/** HTTP headers including authorization and content type. */
	@NotNull
	private HttpHeaders headers;

	/** Optional socket read timeout in seconds (MOSIP {@code *.rest.timeout} convention). */
	@Pattern(regexp = "^[0-9]*$", message = "{mosip.rest.request.timeout.message}")
	private Integer timeout;
}
