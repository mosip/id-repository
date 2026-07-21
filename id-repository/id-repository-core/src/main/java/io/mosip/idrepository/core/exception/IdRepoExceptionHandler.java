package io.mosip.idrepository.core.exception;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.AUTHORIZATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_REQUEST;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.nio.file.AccessDeniedException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.core.JsonParseException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.mosip.idrepository.core.constant.AuthAdapterErrorCode;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global REST exception handler for ID Repository services.
 * <p>
 * Registered as {@link RestControllerAdvice} and converts all thrown exceptions into
 * MOSIP-standard {@link IdResponseDTO} error payloads with HTTP 200 (except authentication
 * failures which may return 401). Maps HTTP method and operation context to the correct
 * response {@code id} field via the injected {@code id} map.
 * </p>
 *
 * @see IdRepoAppException
 * @see IdRepoAppUncheckedException
 * @see IdRepoUnknownException
 * @see AuthenticationException
 * @see RestServiceException
 * @see io.mosip.idrepository.core.dto.IdResponseDTO
 *
 * @author Manoj SP
 */
@RestControllerAdvice
public class IdRepoExceptionHandler extends ResponseEntityExceptionHandler {

	/** URI suffix for UIN reactivation endpoints. */
	private static final String REACTIVATE = "reactivate";

	/** URI suffix for UIN deactivation endpoints. */
	private static final String DEACTIVATE = "deactivate";

	/** Logger category identifier for this handler. */
	private static final String ID_REPO_EXCEPTION_HANDLER = "IdRepoExceptionHandler";

	/** Request field name used when reporting {@link DateTimeParseException} on {@code requesttime}. */
	private static final String REQUEST_TIME = "requesttime";

	/** Application module name for structured logging. */
	private static final String ID_REPO = "IdRepo";

	/** HTTP GET operation key for response {@code id} resolution. */
	private static final String READ = "read";

	/** HTTP POST operation key for response {@code id} resolution. */
	private static final String CREATE = "create";

	/** HTTP PATCH operation key for response {@code id} resolution. */
	private static final String UPDATE = "update";

	/** Structured logger for exception handling events. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoExceptionHandler.class);

	/**
	 * Map of operation names to MOSIP response {@code id} values
	 * (e.g. {@code create} → {@code mosip.id.create}, {@code read} → {@code mosip.id.read}).
	 */
	@Resource
	private Map<String, String> id;

	/** Exception class name substring used to detect invalid {@code requesttime} values. */
	private static final String DATE_TIME_PARSE_EXCEPTION = "DateTimeParseException";

	/**
	 * Handles malformed or unreadable HTTP request bodies.
	 * <p>
	 * Maps {@link DateTimeParseException} on {@code requesttime} to
	 * {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#INVALID_INPUT_PARAMETER}; all other cases return
	 * {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#INVALID_REQUEST}.
	 * </p>
	 *
	 * @param httpMessageNotReadableException deserialization failure
	 * @param headers                         response headers
	 * @param status                          HTTP status from the framework
	 * @param request                         current web request
	 * @return MOSIP error response with HTTP 200
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException httpMessageNotReadableException, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		String exceptionMessage = httpMessageNotReadableException.getMessage();
		Throwable rootCause = getRootCause(httpMessageNotReadableException);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleHttpMessageNotReadable - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? httpMessageNotReadableException : rootCause));
		IdRepoAppException idRepoAppException;
		if(exceptionMessage != null && httpMessageNotReadableException.getMessage().contains(DATE_TIME_PARSE_EXCEPTION)){
			idRepoAppException = new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(), String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), REQUEST_TIME));
			return new ResponseEntity<>(buildExceptionResponse(idRepoAppException, ((ServletWebRequest)request).getHttpMethod(), null), HttpStatus.OK);
		} else if (rootCause instanceof JsonParseException jsonParseException
				&& jsonParseException.getMessage() != null
				&& jsonParseException.getMessage().contains("code 160")) {
			idRepoAppException = new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					"Request JSON contains non-breaking spaces (U+00A0); replace with normal spaces or re-type the body.");
			return new ResponseEntity<>(buildExceptionResponse(idRepoAppException,
					((ServletWebRequest) request).getHttpMethod(), null), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(buildExceptionResponse(httpMessageNotReadableException, ((ServletWebRequest)request).getHttpMethod(), null), HttpStatus.OK);
		}
	}

	/**
	 * Handles requests to non-existent static or API resources.
	 *
	 * @param noResourceFoundException resource-not-found failure from Spring MVC
	 * @param headers                  response headers
	 * @param status                   HTTP status from the framework
	 * @param request                  current web request
	 * @return MOSIP error response with {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#INVALID_REQUEST} and HTTP 200
	 */
	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException noResourceFoundException, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (isFaviconRequest(noResourceFoundException)) {
			return ResponseEntity.notFound().build();
		}
		Throwable rootCause = getRootCause(noResourceFoundException);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleNoResourceFoundException - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? noResourceFoundException : rootCause));

		IdRepoAppException idRepoAppException = new IdRepoAppException(INVALID_REQUEST.getErrorCode(), INVALID_REQUEST.getErrorMessage());
			return new ResponseEntity<>(buildExceptionResponse(idRepoAppException, ((ServletWebRequest)request).getHttpMethod(), null), HttpStatus.OK);
	}

	private static boolean isFaviconRequest(NoResourceFoundException exception) {
		String resourcePath = exception.getResourcePath();
		return resourcePath != null && resourcePath.toLowerCase().contains("favicon");
	}

	/**
	 * Handles exceptions that are not handled by other methods in this advice.
	 * <p>
	 * Logs the full stack trace and returns {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#UNKNOWN_ERROR}.
	 * </p>
	 *
	 * @param ex      any unhandled exception
	 * @param request current web request
	 * @return MOSIP error response with HTTP 200
	 */
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleAllExceptions - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? ex : rootCause));
		IdRepoUnknownException e = new IdRepoUnknownException(UNKNOWN_ERROR);
		return new ResponseEntity<>(
				buildExceptionResponse(e, ((ServletWebRequest) request).getHttpMethod(), null),
				HttpStatus.OK);
	}
	
	/**
	 * Handles {@link BeanCreationException} raised during lazy bean initialization.
	 * <p>
	 * {@code IdObjectMasterDataValidator} is loaded lazily and uses {@code RestTemplate}
	 * in {@code @PostConstruct}. When that call fails, Spring wraps the cause as
	 * {@link BeanCreationException}. This handler unwraps {@link AuthenticationException}
	 * and {@link IdRepoAppUncheckedException} causes before falling back to
	 * {@link #handleAllExceptions(Exception, WebRequest)}.
	 * </p>
	 *
	 * @param ex      bean creation failure
	 * @param request current web request
	 * @return delegated handler response
	 */
	@ExceptionHandler(BeanCreationException.class)
	protected ResponseEntity<Object> handleBeanCreationException(BeanCreationException ex, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		rootCause = Objects.isNull(rootCause) ? ex : rootCause;
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleBeanCreationException - \n" + ExceptionUtils.getStackTrace(rootCause));
		if (Objects.nonNull(rootCause) && rootCause.getClass().isAssignableFrom(AuthenticationException.class)) {
			return handleAuthenticationException((AuthenticationException) rootCause, request);
		} else if (Objects.nonNull(rootCause)
				&& rootCause.getClass().isAssignableFrom(IdRepoAppUncheckedException.class)) {
			return handleIdAppUncheckedException((IdRepoAppUncheckedException) rootCause, request);
		} else {
			return handleAllExceptions((Exception) rootCause, request);
		}
	}

	/**
	 * Handles {@link AccessDeniedException} when the authenticated user lacks API permission.
	 *
	 * @param ex      access denied failure
	 * @param request current web request
	 * @return MOSIP error response with {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#AUTHORIZATION_FAILED} and HTTP 200
	 */
	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<Object> handleAccessDeniedException(Exception ex, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleAccessDeniedException - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? ex : rootCause));
		IdRepoUnknownException e = new IdRepoUnknownException(AUTHORIZATION_FAILED);
		return new ResponseEntity<>(
				buildExceptionResponse(e, ((ServletWebRequest) request).getHttpMethod(), null),
				HttpStatus.OK);
	}

	/**
	 * Handles {@link AuthenticationException} from outbound REST authentication failures.
	 * <p>
	 * Maps to {@link IdRepoUnknownException} with auth-adapter error codes when available.
	 * Returns HTTP 401 (or the status code carried by the exception) instead of HTTP 200.
	 * </p>
	 *
	 * @param ex      authentication failure from {@link io.mosip.idrepository.core.helper.RestHelper}
	 * @param request current web request
	 * @return MOSIP error response with appropriate HTTP status
	 */
	@ExceptionHandler(AuthenticationException.class)
	protected ResponseEntity<Object> handleAuthenticationException(@NonNull AuthenticationException ex, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleAuthenticationException - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? ex : rootCause));
		IdRepoUnknownException e = new IdRepoUnknownException(
				ex.getErrorTexts().isEmpty() ? AuthAdapterErrorCode.UNAUTHORIZED.getErrorCode() : ex.getErrorCode(),
				ex.getErrorTexts().isEmpty() ? AuthAdapterErrorCode.UNAUTHORIZED.getErrorMessage() : ex.getErrorText());
		return new ResponseEntity<>(
				buildExceptionResponse(e, ((ServletWebRequest) request).getHttpMethod(), null),
				ex.getStatusCode() == 0 ? HttpStatus.UNAUTHORIZED : HttpStatus.valueOf(ex.getStatusCode()));
	}

	/**
	 * Internal handler for framework-level exceptions not covered by specific {@code @ExceptionHandler} methods.
	 * <p>
	 * Special-cases {@link DateTimeParseException} on {@code requesttime} for deactivate/reactivate URIs,
	 * and maps {@link HttpMessageNotReadableException}, {@link ServletException}, and
	 * {@link BeansException} to {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#INVALID_REQUEST}.
	 * </p>
	 *
	 * @param ex           the exception
	 * @param errorMessage optional error body (unused)
	 * @param headers      response headers
	 * @param status       HTTP status
	 * @param request      current web request
	 * @return MOSIP error response, or delegates to {@link #handleAllExceptions(Exception, WebRequest)}
	 */
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object errorMessage,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleExceptionInternal - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? ex : rootCause));
		if (ex instanceof HttpMessageNotReadableException && org.apache.commons.lang3.exception.ExceptionUtils
				.getRootCause(ex).getClass().isAssignableFrom(DateTimeParseException.class)) {
			ex = new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), REQUEST_TIME));
			if (request instanceof ServletWebRequest
					&& ((ServletWebRequest) request).getRequest().getRequestURI().endsWith(DEACTIVATE)) {
				return new ResponseEntity<>(
						buildExceptionResponse(ex, ((ServletWebRequest) request).getHttpMethod(), DEACTIVATE),
						HttpStatus.OK);
			} else if (request instanceof ServletWebRequest
					&& ((ServletWebRequest) request).getRequest().getRequestURI().endsWith(REACTIVATE)) {
				return new ResponseEntity<>(
						buildExceptionResponse(ex, ((ServletWebRequest) request).getHttpMethod(), REACTIVATE),
						HttpStatus.OK);
			} else {
				if (request instanceof ServletWebRequest) {
					return new ResponseEntity<>(
							buildExceptionResponse(ex, ((ServletWebRequest) request).getHttpMethod(), null), HttpStatus.OK);
				}
				else{
					throw new IllegalStateException();
				}
			}
		} else if (ex instanceof HttpMessageNotReadableException || ex instanceof ServletException
				|| ex instanceof BeansException) {
			ex = new IdRepoAppException(INVALID_REQUEST.getErrorCode(),
					INVALID_REQUEST.getErrorMessage());

			return new ResponseEntity<>(buildExceptionResponse(ex, ((ServletWebRequest) request).getHttpMethod(), null),
					HttpStatus.OK);
		} else {
			return handleAllExceptions(ex, request);
		}
	}

	/**
	 * Handles checked {@link IdRepoAppException} thrown from application business logic.
	 * <p>
	 * Uses {@link IdRepoAppException#getOperation()} when set to resolve the response {@code id}.
	 * </p>
	 *
	 * @param ex      application checked exception
	 * @param request current web request
	 * @return MOSIP error response with HTTP 200
	 */
	@ExceptionHandler(IdRepoAppException.class)
	protected ResponseEntity<Object> handleIdAppException(@NonNull IdRepoAppException ex, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleIdAppException - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? ex : rootCause));

		return new ResponseEntity<>(buildExceptionResponse(ex,
				((ServletWebRequest) request).getHttpMethod(), ex.getOperation()), HttpStatus.OK);
	}

	/**
	 * Handles unchecked {@link IdRepoAppUncheckedException} thrown from application runtime logic.
	 *
	 * @param ex      application unchecked exception
	 * @param request current web request
	 * @return MOSIP error response with HTTP 200
	 */
	@ExceptionHandler(IdRepoAppUncheckedException.class)
	protected ResponseEntity<Object> handleIdAppUncheckedException(@NonNull IdRepoAppUncheckedException ex, WebRequest request) {
		Throwable rootCause = getRootCause(ex);
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_EXCEPTION_HANDLER,
				"handleIdAppUncheckedException - \n" + ExceptionUtils.getStackTrace(Objects.isNull(rootCause) ? ex : rootCause));

		return new ResponseEntity<>(
				buildExceptionResponse(ex, ((ServletWebRequest) request).getHttpMethod(), null),
				HttpStatus.OK);
	}

	/**
	 * Builds a MOSIP-standard {@link IdResponseDTO} error payload from any exception.
	 * <p>
	 * Resolves the response {@code id} from the operation name or HTTP method, extracts
	 * {@link ServiceError} entries from the root cause, and sets the application version.
	 * </p>
	 *
	 * @param ex         the exception (possibly wrapped)
	 * @param httpMethod HTTP method of the failed request, used for {@code id} fallback
	 * @param operation  explicit operation name override (e.g. {@code deactivate})
	 * @return populated {@link IdResponseDTO} error response object
	 */
	private Object buildExceptionResponse(Exception ex, @Nullable HttpMethod httpMethod, String operation) {

		IdResponseDTO response = new IdResponseDTO();

		Throwable e = getIdRepoAppExceptionRootCause(ex);

		if (Objects.nonNull(operation)) {
			response.setId(id.get(operation));
		} else if (Objects.nonNull(httpMethod)) {
			if (httpMethod.compareTo(HttpMethod.GET) == 0) {
				response.setId(id.get(READ));
			} else if (httpMethod.compareTo(HttpMethod.POST) == 0) {
				response.setId(id.get(CREATE));
			} else if (httpMethod.compareTo(HttpMethod.PATCH) == 0) {
				response.setId(id.get(UPDATE));
			}
		}

		response.setErrors(getAllErrors(e));

		response.setVersion(EnvUtil.getAppVersion());

		return response;
	}

	/**
	 * Extracts distinct {@link ServiceError} entries from a MOSIP kernel exception.
	 * <p>
	 * Used by {@link io.mosip.idrepository.core.helper.AuditHelper#auditError} to serialize
	 * error details into audit log descriptions.
	 * </p>
	 *
	 * @param e checked or unchecked kernel exception with error codes and messages
	 * @return list of distinct service errors, or {@code null} if {@code e} is not a kernel exception
	 */
	public static List<ServiceError> getAllErrors(Throwable e) {
		List<ServiceError> errors = null;
		if (e instanceof BaseCheckedException) {
			List<String> errorCodes = ((BaseCheckedException) e).getCodes();
			List<String> errorTexts = ((BaseCheckedException) e).getErrorTexts();

			errors = errorTexts.parallelStream()
					.map(errMsg -> new ServiceError(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());

		}

		if (e instanceof BaseUncheckedException) {
			List<String> errorCodes = ((BaseUncheckedException) e).getCodes();
			List<String> errorTexts = ((BaseUncheckedException) e).getErrorTexts();

			errors = errorTexts.parallelStream()
					.map(errMsg -> new ServiceError(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());
		}
		return errors;
	}

	/**
	 * Unwraps the root cause of an exception, falling back to {@link IdRepoAppException} chain walking
	 * if {@link ExceptionUtils#getRootCause} itself throws.
	 *
	 * @param ex the exception to unwrap
	 * @return root cause throwable, or {@code null}
	 */
	private Throwable getRootCause(Exception ex) {
		Throwable rootCause;
		try {
			rootCause = ExceptionUtils.getRootCause(ex);
		} catch (Exception e) {
			mosipLogger.warn("Exception thrown from ExceptionUtils when finding root cause : " + e.getMessage());
			rootCause = getIdRepoAppExceptionRootCause(ex);
		}
		return rootCause;
	}

	/**
	 * Walks the exception cause chain to find the innermost {@link IdRepoAppException}.
	 *
	 * @param ex the outer exception, possibly wrapping {@link IdRepoAppException}
	 * @return the deepest {@link IdRepoAppException} in the chain, or {@code ex} if none found
	 */
	private Throwable getIdRepoAppExceptionRootCause(Exception ex) {
		Throwable e = ex;
		while (e != null) {
			if (Objects.nonNull(e.getCause()) && (e.getCause() instanceof IdRepoAppException)) {
				e = e.getCause();
			} else {
				break;
			}
		}
		return e;
	}
}