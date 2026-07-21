package io.mosip.kernel.auth.defaultadapter.config;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.auth.defaultadapter.constant.AuthAdapterConstant;
import io.mosip.kernel.auth.defaultadapter.constant.AuthAdapterErrorCode;
import io.mosip.kernel.auth.defaultadapter.exception.AuthAdapterException;
import io.mosip.kernel.auth.defaultadapter.helper.TokenHelper;
import io.mosip.kernel.auth.defaultadapter.helper.TokenValidationHelper;
import io.mosip.kernel.auth.defaultadapter.model.TokenHolder;

/**
 * Spring Framework 7 compatible shadow of kernel-auth {@code SelfTokenRestInterceptor}.
 * <p>
 * Loaded from {@code id-repository-service} before the {@code kernel-auth-adapter.jar} class.
 * Required even with {@code kernel-auth-adapter} 1.3.1 (mosip-openid-bridge): the published jar
 * still invokes {@code HttpHeaders.get(Object)} on 401 retry, which Spring 7 removed.
 * Replaces that path with {@code getOrEmpty(String)} and remove/add.
 * </p>
 *
 * @see SelfTokenExchangeFilterFunction
 */
public class SelfTokenRestInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SelfTokenRestInterceptor.class);

	private final String clientID;

	private final String clientSecret;

	private final String appID;

	private final TokenHolder<String> cachedToken;

	private final RestTemplate restTemplate;

	private final TokenHelper tokenHelper;

	private final TokenValidationHelper tokenValidationHelper;

	public SelfTokenRestInterceptor(Environment environment, RestTemplate plainRestTemplate,
			TokenHolder<String> cachedToken, TokenHelper tokenHelper, TokenValidationHelper tokenValidationHelper,
			String applName) {
		clientID = environment.getProperty("mosip.iam.adapter.clientid." + applName,
				environment.getProperty("mosip.iam.adapter.clientid", ""));
		clientSecret = environment.getProperty("mosip.iam.adapter.clientsecret." + applName,
				environment.getProperty("mosip.iam.adapter.clientsecret", ""));
		appID = environment.getProperty("mosip.iam.adapter.appid." + applName,
				environment.getProperty("mosip.iam.adapter.appid", ""));
		this.cachedToken = cachedToken;
		this.restTemplate = plainRestTemplate;
		this.tokenHelper = tokenHelper;
		this.tokenValidationHelper = tokenValidationHelper;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		if (cachedToken.getToken() == null) {
			String authToken = tokenHelper.getClientToken(clientID, clientSecret, appID, restTemplate);
			if (Objects.isNull(authToken)) {
				LOGGER.error("there is some issue with getting token with clienid and secret");
				throw new AuthAdapterException(AuthAdapterErrorCode.SELF_AUTH_TOKEN_NULL.getErrorCode(),
						AuthAdapterErrorCode.SELF_AUTH_TOKEN_NULL.getErrorMessage());
			}
			cachedToken.setToken(authToken);
		}

		request.getHeaders().add(AuthAdapterConstant.AUTH_HEADER_COOKIE,
				AuthAdapterConstant.AUTH_HEADER + cachedToken.getToken());

		ClientHttpResponse response = execution.execute(request, body);
		if (response.getStatusCode() != HttpStatus.UNAUTHORIZED) {
			return response;
		}

		synchronized (this) {
			if (!isTokenValid((String) cachedToken.getToken())) {
				String authToken = tokenHelper.getClientToken(clientID, clientSecret, appID, restTemplate);
				cachedToken.setToken(authToken);
			}
		}

		List<String> cookies = request.getHeaders().getOrEmpty(AuthAdapterConstant.AUTH_HEADER_COOKIE).stream()
				.filter(cookie -> !cookie.contains(AuthAdapterConstant.AUTH_HEADER))
				.collect(Collectors.toList());

		request.getHeaders().remove(AuthAdapterConstant.AUTH_HEADER_COOKIE);
		cookies.forEach(cookie -> request.getHeaders().add(AuthAdapterConstant.AUTH_HEADER_COOKIE, cookie));
		request.getHeaders().add(AuthAdapterConstant.AUTH_HEADER_COOKIE,
				AuthAdapterConstant.AUTH_HEADER + cachedToken.getToken());

		return execution.execute(request, body);
	}

	private boolean isTokenValid(String authToken) {
		return Objects.nonNull(
				tokenValidationHelper.getOnlineTokenValidatedUserResponse(authToken, restTemplate));
	}

}
