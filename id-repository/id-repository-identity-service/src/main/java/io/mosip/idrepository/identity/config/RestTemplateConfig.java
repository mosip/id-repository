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

@Configuration
public class RestTemplateConfig {
	
	Logger mosipLogger = IdRepoLogger.getLogger(RestTemplateConfig.class);

	
	@Autowired
	private RestTemplate restTemplate;
	
	@PostConstruct
	public void init() {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

			protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
				mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError", "Rest Template logs",
						"Status error : " + response.getRawStatusCode() + " " + response.getStatusCode() + "  "
								+ response.getStatusText());
				if (response.getStatusCode().is4xxClientError()) {
					if (response.getRawStatusCode() == 401 || response.getRawStatusCode() == 403) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"request failed with status code :" + response.getRawStatusCode(),
								"\n\n" + new String(super.getResponseBody(response)));
						List<ServiceError> errorList = ExceptionUtils
								.getServiceErrorList(new String(super.getResponseBody(response)));
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"Throwing AuthenticationException", errorList.toString());
						if (errorList.isEmpty()) {
							throw new AuthenticationException(IdRepoErrorConstants.AUTHENTICATION_FAILED,
									response.getRawStatusCode());
						} else {
							throw new AuthenticationException(errorList.get(0).getErrorCode(),
									errorList.get(0).getMessage(), response.getRawStatusCode());
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
