package io.mosip.idrepository.core.constant;

import lombok.Getter;

/**
 * Kernel auth-adapter error codes mapped to HTTP 401/403 and connectivity failures.
 *
 * <p>
 * Used by {@link io.mosip.idrepository.core.exception.IdRepoExceptionHandler} when
 * translating {@code AuthenticationException} into MOSIP service-error responses.
 * Codes follow the {@code KER-ATH-xxx} kernel pattern.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Bridges kernel auth-adapter failures into the ID Repository error envelope so API
 * clients receive consistent {@code errorCode} / {@code errorMessage} pairs for
 * unauthorized, forbidden, and auth-service connectivity problems.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * These are kernel ({@code KER-ATH-*}) codes, not {@code IDR-*} application codes.
 * IDA may see the same kernel codes from its own auth adapter; ID Repository must not
 * remap them to different strings without coordinating with kernel auth-adapter releases.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * throw new AuthenticationException(
 *     AuthAdapterErrorCode.UNAUTHORIZED.getErrorCode(),
 *     AuthAdapterErrorCode.UNAUTHORIZED.getErrorMessage());
 * </pre>
 * <p>
 * {@link io.mosip.idrepository.core.exception.IdRepoExceptionHandler} maps thrown
 * auth exceptions to these codes when building the HTTP error body.
 * </p>
 *
 * @see io.mosip.idrepository.core.exception.IdRepoExceptionHandler
 * @see io.mosip.idrepository.core.exception.AuthenticationException
 */
@Getter
public enum AuthAdapterErrorCode {

	/** HTTP 401 — token missing, expired, or signature invalid. */
	UNAUTHORIZED("KER-ATH-401", "Authentication Failed"),

	/** HTTP 403 — authenticated but not authorized for the requested resource. */
	FORBIDDEN("KER-ATH-403", "Forbidden"),

	/** Auth service unreachable (connection refused / timeout). */
	CONNECT_EXCEPTION("KER-ATH-002", "Fail to connect to auth service"),

	/** Auth service returned a non-parseable error body. */
	RESPONSE_PARSE_ERROR("KER-ATH-001", "Error occur while parsing error from response");

	/**
	 * MOSIP kernel error code (e.g. {@code KER-ATH-401}).
	 * -- GETTER --
	 *
	 * @return the MOSIP kernel error code
	 */
	private final String errorCode;

	/**
	 * Human-readable error message returned to API callers.
	 * -- GETTER --
	 *
	 * @return the error message for API responses
	 */
	private final String errorMessage;

	/**
	 * Creates an auth-adapter error constant with its kernel code and message.
	 *
	 * @param errorCode    kernel auth error code (e.g. {@code KER-ATH-401})
	 * @param errorMessage short description for API responses
	 */
	private AuthAdapterErrorCode(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
}
