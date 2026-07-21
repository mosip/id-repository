package io.mosip.kernel.auth.defaultadapter.config;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.mosip.kernel.auth.defaultadapter.constant.AuthAdapterConstant;
import io.mosip.kernel.auth.defaultadapter.constant.AuthAdapterErrorCode;
import io.mosip.kernel.auth.defaultadapter.exception.AuthAdapterException;
import io.mosip.kernel.auth.defaultadapter.helper.TokenHelper;
import io.mosip.kernel.auth.defaultadapter.helper.TokenValidationHelper;
import io.mosip.kernel.auth.defaultadapter.model.TokenHolder;
import reactor.core.publisher.Mono;

/**
 * Spring Framework 7 compatible shadow of kernel-auth {@code SelfTokenExchangeFilterFunction}.
 * Replaces {@code HttpHeaders.get(Object)} with {@code getOrEmpty(String)} and rebuilds
 * {@link ClientRequest} on 401 retry instead of mutating read-only headers.
 */
public class SelfTokenExchangeFilterFunction implements ExchangeFilterFunction {

	private static final Logger LOGGER = LoggerFactory.getLogger(SelfTokenExchangeFilterFunction.class);

	private final String clientID;

	private final String clientSecret;

	private final String appID;

	private final TokenHolder cachedToken;

	private final TokenHelper tokenHelper;

	private final TokenValidationHelper tokenValidationHelper;

	private final WebClient webClient;

	public SelfTokenExchangeFilterFunction(Environment environment, WebClient webClient, TokenHolder cachedToken,
			TokenHelper tokenHelper, TokenValidationHelper tokenValidationHelper, String applName) {
		clientID = environment.getProperty("mosip.iam.adapter.clientid." + applName,
				environment.getProperty("mosip.iam.adapter.clientid", ""));
		clientSecret = environment.getProperty("mosip.iam.adapter.clientsecret." + applName,
				environment.getProperty("mosip.iam.adapter.clientsecret", ""));
		appID = environment.getProperty("mosip.iam.adapter.appid." + applName,
				environment.getProperty("mosip.iam.adapter.appid", ""));
		this.cachedToken = cachedToken;
		this.webClient = webClient;
		this.tokenHelper = tokenHelper;
		this.tokenValidationHelper = tokenValidationHelper;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		if (currentToken() == null) {
			String authToken = tokenHelper.getClientToken(clientID, clientSecret, appID, webClient);
			if (Objects.isNull(authToken)) {
				LOGGER.error("Self-token fetch failed for clientId={}, appId={}", clientID, appID);
				throw new AuthAdapterException(AuthAdapterErrorCode.SELF_AUTH_TOKEN_NULL.getErrorCode(),
						AuthAdapterErrorCode.SELF_AUTH_TOKEN_NULL.getErrorMessage());
			}
			cachedToken.setToken(authToken);
		}

		ClientRequest authorized = ClientRequest.from(request)
				.header(AuthAdapterConstant.AUTH_HEADER_COOKIE,
						AuthAdapterConstant.AUTH_HEADER + currentToken())
				.build();

		ClientResponse response = next.exchange(authorized).block();
		if (response != null && response.statusCode() != HttpStatus.UNAUTHORIZED) {
			return Mono.just(response);
		}

		synchronized (this) {
			if (!isTokenValid(currentToken())) {
				String authToken = tokenHelper.getClientToken(clientID, clientSecret, appID, webClient);
				cachedToken.setToken(authToken);
			}
		}

		List<String> cookies = request.headers().getOrEmpty(AuthAdapterConstant.AUTH_HEADER_COOKIE).stream()
				.filter(str -> !str.contains(AuthAdapterConstant.AUTH_HEADER))
				.collect(Collectors.toList());

		ClientRequest.Builder retryBuilder = ClientRequest.from(request);
		retryBuilder.headers(headers -> {
			headers.remove(AuthAdapterConstant.AUTH_HEADER_COOKIE);
			cookies.forEach(cookie -> headers.add(AuthAdapterConstant.AUTH_HEADER_COOKIE, cookie));
			headers.add(AuthAdapterConstant.AUTH_HEADER_COOKIE,
					AuthAdapterConstant.AUTH_HEADER + currentToken());
		});
		return next.exchange(retryBuilder.build());
	}

	private String currentToken() {
		return (String) cachedToken.getToken();
	}

	private boolean isTokenValid(String authToken) {
		return Objects.nonNull(tokenValidationHelper.doOnlineTokenValidation(authToken, webClient));
	}

}
