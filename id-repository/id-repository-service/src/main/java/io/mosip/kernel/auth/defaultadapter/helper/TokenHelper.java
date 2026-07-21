package io.mosip.kernel.auth.defaultadapter.helper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.kernel.auth.defaultadapter.constant.AuthAdapterConstant;
import io.mosip.kernel.auth.defaultadapter.constant.AuthAdapterErrorCode;
import io.mosip.kernel.auth.defaultadapter.exception.AuthRestException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;

/**
 * Spring Framework 7 / Boot 4 replacement for {@code kernel-auth-adapter} {@code TokenHelper}.
 * <p>
 * Loaded from {@code id-repository-service} before the shaded {@code kernel-auth-adapter.jar}
 * class. Published adapter bytecode still uses {@code HttpEntity(Object, MultiValueMap)} with
 * {@link HttpHeaders}; develop branch uses typed {@code HttpEntity&lt;MultiValueMap&lt;String, String&gt;&gt;}.
 * </p>
 */
public class TokenHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(TokenHelper.class);

	@Value("${auth.server.admin.issuer.uri:}")
	private String issuerURI;

	@Value("${auth.server.admin.issuer.internal.uri:}")
	private String issuerInternalURI;

	@Autowired
	private ObjectMapper mapper;

	@Value("#{${mosip.kernel.auth.appids.realm.map}}")
	private Map<String, String> realmMap;

	@Value("${auth.server.admin.oidc.token.path:/protocol/openid-connect/token}")
	private String tokenPath;

	/** Lazy — avoids BeanConfig ↔ TokenHelper ↔ plainRestTemplate circular dependency. */
	@Autowired(required = false)
	@Qualifier("plainRestTemplate")
	private ObjectProvider<RestTemplate> plainRestTemplateProvider;

	public String getClientToken(String clientId, String clientSecret, String appId, RestTemplate restTemplate) {
		if ("".equals(issuerURI)) {
			LOGGER.warn("OIDC Service URL is not available in config file, not requesting for new auth token.");
			return null;
		}
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
			LOGGER.error(
					"Self-token client credentials missing (RestTemplate): clientId present={}, clientSecret present={}, appId={}",
					StringUtils.hasText(clientId), StringUtils.hasText(clientSecret), appId);
			return null;
		}
		logSelfTokenCredentialContext("RestTemplate", clientId, clientSecret, appId);
		String realm = getRealmIdFromAppId(appId);
		if (Objects.isNull(realm)) {
			return null;
		}

		String internalIssuer = resolveInternalIssuer();
		String externalIssuer = normalizeIssuer(issuerURI);
		ResponseEntity<String> response = requestClientToken(restTemplate, clientId, clientSecret, appId, realm,
				internalIssuer, "internal");
		if (response == null && !internalIssuer.equals(externalIssuer)) {
			LOGGER.warn(
					"Self-token failed via internal issuer — retrying with auth.server.admin.issuer.uri (curl usually hits this URL)");
			response = requestClientToken(restTemplate, clientId, clientSecret, appId, realm, externalIssuer,
					"external");
		}

		if (response == null) {
			LOGGER.error("Self-token failed (RestTemplate): no response from Keycloak for clientId={}, appId={}",
					clientId, appId);
			return null;
		}
		if (response.getStatusCode().is2xxSuccessful()) {
			LOGGER.info("Self-token Keycloak response OK (RestTemplate): clientId={}, status={}",
					clientId, response.getStatusCode());
		}
		String responseBody = response.getBody();
		List<ServiceError> validationErrorList = ExceptionUtils.getServiceErrorList(responseBody);
		if (!validationErrorList.isEmpty()) {
			throw new AuthRestException(validationErrorList);
		}
		try {
			JsonNode jsonNode = mapper.readTree(responseBody);
			String accessToken = jsonNode.get(AuthAdapterConstant.ACCESS_TOKEN).asText();
			if (Objects.nonNull(accessToken)) {
				LOGGER.info("Self-token obtained (RestTemplate): clientId={}, appId={}", clientId, appId);
				return accessToken;
			}
		}
		catch (IOException e) {
			LOGGER.error("Self-token response parse error (RestTemplate): clientId={}, error={}", clientId,
					e.getMessage(), e);
		}

		LOGGER.error("Self-token missing access_token in Keycloak response (RestTemplate): clientId={}, appId={}",
				clientId, appId);
		return null;
	}

	public String getClientToken(String clientId, String clientSecret, String appId, WebClient webClient) {
		// kernel-auth plainWebClient attaches ReactorLoadBalancerExchangeFilterFunction when present;
		// external IAM hostnames (iam.dev2.mosip.net) are not K8s service IDs — use plainRestTemplate.
		RestTemplate plainRestTemplate = plainRestTemplateProvider != null
				? plainRestTemplateProvider.getIfAvailable()
				: null;
		if (plainRestTemplate != null) {
			LOGGER.info("Self-token WebClient path delegating to plainRestTemplate (bypass LoadBalancer)");
			return getClientToken(clientId, clientSecret, appId, plainRestTemplate);
		}
		if (webClient == null) {
			LOGGER.error("Self-token: plainRestTemplate unavailable and no WebClient provided");
			return null;
		}
		if ("".equals(issuerURI)) {
			LOGGER.warn("OIDC Service URL is not available in config file, not requesting for new auth token.");
			return null;
		}
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
			LOGGER.error("Self-token client credentials missing (clientId present={}, clientSecret present={}) for appId={}",
					StringUtils.hasText(clientId), StringUtils.hasText(clientSecret), appId);
			return null;
		}
		String realm = getRealmIdFromAppId(appId);
		if (Objects.isNull(realm)) {
			LOGGER.error("Self-token realm lookup failed for appId={} — add it to mosip.kernel.auth.appids.realm.map", appId);
			return null;
		}

		String internalIssuer = resolveInternalIssuer();
		String externalIssuer = normalizeIssuer(issuerURI);
		String accessToken = requestClientTokenWebClient(webClient, clientId, clientSecret, appId, realm,
				internalIssuer, "internal");
		if (accessToken == null && !internalIssuer.equals(externalIssuer)) {
			LOGGER.warn(
					"Self-token WebClient failed via internal issuer — retrying with auth.server.admin.issuer.uri");
			accessToken = requestClientTokenWebClient(webClient, clientId, clientSecret, appId, realm,
					externalIssuer, "external");
		}
		if (accessToken != null) {
			return accessToken;
		}
		LOGGER.error("Error connecting to OIDC service (WebClient) {} or UNKNOWN Error.",
				AuthAdapterErrorCode.CANNOT_CONNECT_TO_AUTH_SERVICE.getErrorMessage());
		return null;
	}

	private ResponseEntity<String> requestClientToken(RestTemplate restTemplate, String clientId, String clientSecret,
			String appId, String realm, String issuerBase, String issuerLabel) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> valueMap = new LinkedMultiValueMap<>();
		valueMap.add(AuthAdapterConstant.GRANT_TYPE, AuthAdapterConstant.CLIENT_CREDENTIALS);
		valueMap.add(AuthAdapterConstant.CLIENT_ID, clientId);
		valueMap.add(AuthAdapterConstant.CLIENT_SECRET, clientSecret);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(valueMap, headers);
		String tokenUrl = issuerBase + realm + tokenPath;
		LOGGER.info("Self-token POST (RestTemplate/{}) clientId={}, appId={}, realm={}, tokenUrl={}",
				issuerLabel, clientId, appId, realm, tokenUrl);
		try {
			return restTemplate.postForEntity(tokenUrl, request, String.class);
		}
		catch (HttpServerErrorException e) {
			LOGGER.error("Keycloak token request failed (RestTemplate/{}): clientId={}, status={}, body={}",
					issuerLabel, clientId, e.getStatusCode(), e.getResponseBodyAsString());
		}
		catch (HttpClientErrorException e) {
			logKeycloakClientError("RestTemplate/" + issuerLabel, clientId, appId, e);
		}
		return null;
	}

	private String requestClientTokenWebClient(WebClient webClient, String clientId, String clientSecret,
			String appId, String realm, String issuerBase, String issuerLabel) {
		MultiValueMap<String, String> valueMap = new LinkedMultiValueMap<>();
		valueMap.add(AuthAdapterConstant.GRANT_TYPE, AuthAdapterConstant.CLIENT_CREDENTIALS);
		valueMap.add(AuthAdapterConstant.CLIENT_ID, clientId);
		valueMap.add(AuthAdapterConstant.CLIENT_SECRET, clientSecret);
		String tokenUrl = issuerBase + realm + tokenPath;
		LOGGER.info("Self-token request (WebClient/{}): clientId={}, appId={}, realm={}, tokenUrl={}",
				issuerLabel, clientId, appId, realm, tokenUrl);
		ClientResponse response = webClient.method(HttpMethod.POST)
				.uri(UriComponentsBuilder.fromUriString(tokenUrl).toUriString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(valueMap))
				.exchangeToMono(Mono::just)
				.block();
		if (response != null && response.statusCode() == HttpStatus.OK) {
			ObjectNode responseBody = response.bodyToMono(ObjectNode.class).block();
			if (responseBody != null) {
				String accessToken = responseBody.get(AuthAdapterConstant.ACCESS_TOKEN).asText();
				if (Objects.nonNull(accessToken)) {
					LOGGER.info("Self-token obtained (WebClient/{}): clientId={}, appId={}", issuerLabel, clientId,
							appId);
					return accessToken;
				}
			}
		}
		if (response != null) {
			String errorBody = response.bodyToMono(String.class).block();
			LOGGER.error("Keycloak token request failed (WebClient/{}): status={}, body={}", issuerLabel,
					response.statusCode(), errorBody);
		}
		return null;
	}

	private String resolveInternalIssuer() {
		return StringUtils.hasText(issuerInternalURI) ? normalizeIssuer(issuerInternalURI) : normalizeIssuer(issuerURI);
	}

	private static String normalizeIssuer(String issuer) {
		if (!StringUtils.hasText(issuer)) {
			return "";
		}
		String normalized = issuer.trim();
		return normalized.endsWith("/") ? normalized : normalized + "/";
	}

	private String getRealmIdFromAppId(String appId) {
		if (realmMap.get(appId) != null) {
			return realmMap.get(appId).toLowerCase();
		}

		LOGGER.warn(
				"Self-token realm not configured for appId='{}' in mosip.kernel.auth.appids.realm.map (keys: {})",
				appId, realmMap != null ? realmMap.keySet() : "null");
		return null;
	}

	private void logSelfTokenCredentialContext(String transport, String clientId, String clientSecret, String appId) {
		String effectiveIssuer = resolveInternalIssuer();
		LOGGER.info(
				"Self-token attempt ({}): clientId={}, appId={}, issuerUri={}, clientSecretPresent={}, clientSecretLength={}",
				transport, clientId, appId, effectiveIssuer,
				StringUtils.hasText(clientSecret), clientSecret != null ? clientSecret.length() : 0);
		if (StringUtils.hasText(clientId) && clientId.contains(".") && !clientId.contains("-")) {
			LOGGER.warn(
					"Self-token clientId '{}' looks wrong — use Keycloak client id e.g. mosip-idmanagement-client (hyphens), not a property key",
					clientId);
		}
	}

	private void logKeycloakClientError(String transport, String clientId, String appId,
			HttpClientErrorException e) {
		String body = e.getResponseBodyAsString();
		LOGGER.error("Keycloak token request failed ({}): clientId={}, appId={}, status={}, body={}",
				transport, clientId, appId, e.getStatusCode(), body);
		if (body != null && body.contains("invalid_client")) {
			LOGGER.warn(
					"Keycloak invalid_client for clientId='{}' — fix mosip.iam.adapter.clientid / mosip.iam.adapter.clientsecret "
							+ "(local: set MOSIP_IAM_ADAPTER_CLIENTID=mosip-idmanagement-client and MOSIP_IAM_ADAPTER_CLIENTSECRET from config server)",
					clientId);
		}
	}
}