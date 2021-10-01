package io.mosip.idrepository.credentialsfeeder.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.EmptyCheckUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import reactor.core.publisher.Mono;

public final class AuthTokenExchangeFilter implements ExchangeFilterFunction {
	
	private static final Logger mosipLogger = IdRepoLogger.getLogger(AuthTokenExchangeFilter.class);
	
	private String authToken;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Environment env;

	private static final String AUTH_COOOKIE_HEADER = "Authorization=";
	private static final String AUTH_HEADER_COOKIE = "Cookie";
	
		@Override
		public Mono<ClientResponse> filter(ClientRequest req, ExchangeFunction next) {
			ClientRequest filtered = ClientRequest.from(req).header(AUTH_HEADER_COOKIE,
						AUTH_COOOKIE_HEADER + getAuthToken()).build();
			return next.exchange(filtered);
		}
		
		private String getAuthToken() {
			if (EmptyCheckUtils.isNullEmpty(authToken) || !TokenHandlerUtil.isValidBearerToken(authToken,
					env.getProperty("auth-token-generator.rest.issuerUrl"),
					env.getProperty("auth-token-generator.rest.clientId"))) {
				generateAuthToken();
				return authToken;
			} else {
				return authToken;
			}
		}

		private void generateAuthToken() {
			ObjectNode requestBody = mapper.createObjectNode();
			requestBody.put("clientId", env.getProperty("auth-token-generator.rest.clientId"));
			requestBody.put("secretKey", env.getProperty("auth-token-generator.rest.secretKey"));
			requestBody.put("appId", env.getProperty("auth-token-generator.rest.appId"));
			RequestWrapper<ObjectNode> request = new RequestWrapper<>();
			request.setRequesttime(DateUtils.getUTCCurrentDateTime());
			request.setRequest(requestBody);
			ClientResponse response = WebClient.create(env.getProperty("auth-token-generator.rest.uri")).post()
					.syncBody(request).exchange().block();
			if (response.statusCode() == HttpStatus.OK) {
				ObjectNode responseBody = response.bodyToMono(ObjectNode.class).block();
				if (responseBody != null && responseBody.get("response").get("status").asText().equalsIgnoreCase("success")) {
					ResponseCookie responseCookie = response.cookies().get("Authorization").get(0);
					authToken = responseCookie.getValue();
					mosipLogger.debug("Auth token generated successfully and set");
				} else {
					mosipLogger.debug("Auth token generation failed: {}",  response);
				}
			} else {
				mosipLogger.error("AuthResponse : status-" + response.statusCode() + " :\n"
								+ response.toEntity(String.class).block().getBody());
			}
		}
	}