package io.mosip.idrepository.identity.config;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CLIENT_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MASTERDATA_RETRIEVE_ERROR;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.AuthenticationException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import jakarta.annotation.PostConstruct;

/**
 * Customizes the kernel {@link RestTemplate} error handling and security context propagation.
 * <p>
 * Maps HTTP 4xx/5xx responses to {@link AuthenticationException} or
 * {@link IdRepoAppUncheckedException}, and enables inheritable security context for async work.
 * The injected {@link RestTemplate} is created by the {@code kernel-auth-adapter} dependency
 * ({@code io.mosip.kernel.auth.defaultadapter.config.BeanConfig}).
 * </p>
 *
 * @see IdRepoSecurityManager
 */
@Configuration
public class RestTemplateConfig {

	/** Logger for this class. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(RestTemplateConfig.class);

	/** Shared synchronous HTTP client from kernel auth adapter. */
	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Configures inheritable security context and a custom response error handler on startup.
	 */
	@PostConstruct
	/**
	 * Init.
	 */
	public void init() {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

			protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
				int statusCodeValue = response.getStatusCode().value();
				mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError", "Rest Template logs",
						"Status error : " + statusCodeValue + " " + response.getStatusCode() + "  "
								+ response.getStatusText());
				if (response.getStatusCode().is4xxClientError()) {
					if (statusCodeValue == 401 || statusCodeValue == 403) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"request failed with status code :" + statusCodeValue,
								"\n\n" + new String(super.getResponseBody(response)));
						List<ServiceError> errorList = ExceptionUtils
								.getServiceErrorList(new String(super.getResponseBody(response)));
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"Throwing AuthenticationException", errorList.toString());
						if (errorList.isEmpty()) {
							throw new AuthenticationException(IdRepoErrorConstants.AUTHENTICATION_FAILED,
									statusCodeValue);
						} else {
							throw new AuthenticationException(errorList.get(0).getErrorCode(),
									errorList.get(0).getMessage(), statusCodeValue);
						}
					} else {
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"Rest Template logs", "Status error - returning RestServiceException - CLIENT_ERROR -- "
										+ new String(super.getResponseBody(response)));
						throw new IdRepoAppUncheckedException(CLIENT_ERROR);
					}
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
							"Rest Template logs", "Status error - returning RestServiceException - CLIENT_ERROR -- "
									+ new String(super.getResponseBody(response)));
					throw new IdRepoAppUncheckedException(MASTERDATA_RETRIEVE_ERROR);
				}
			}
		});
	}

}
