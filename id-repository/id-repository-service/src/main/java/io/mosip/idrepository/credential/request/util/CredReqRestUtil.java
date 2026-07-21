package io.mosip.idrepository.credential.request.util;

import io.mosip.kernel.core.util.DateUtils2;
import com.google.gson.Gson;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.credential.request.constant.ApiName;
import io.mosip.idrepository.core.dto.Metadata;
import io.mosip.idrepository.core.dto.SecretKeyRequest;
import io.mosip.idrepository.core.dto.TokenRequestDTO;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * Outbound REST and Keycloak token client for the credential-request module.
 * <p>
 * Resolves service base URLs from {@link EnvUtil}, attaches MOSIP auth cookies,
 * and provides a pooled {@link RestTemplate} for cryptomanager and other internal calls.
 * Registered as {@code credReqRestUtil} to avoid collision with
 * {@link io.mosip.idrepository.credential.store.util.CredentialStoreRestUtil}.
 * </p>
 *
 * @author Sowmya
 */
@Component("credReqRestUtil")
public class CredReqRestUtil {

	/** Prefix for cached bearer token cookie header value. */
	private static final String AUTHORIZATION = "Authorization=";

	private static final String CONTENT_TYPE = "Content-Type";

	/**
	 * Environment accessor for service URL properties keyed by {@link ApiName}.
	 */
	@Autowired
	private EnvUtil environment;

	/**
	 * Maximum HTTP connections per route for the pooled client.
	 * Property: {@link IdRepoConstants#CREDREQ_HTTPCLIENT_MAX_PER_HOST} (default {@code 20}).
	 */
	@Value("${" + IdRepoConstants.CREDREQ_HTTPCLIENT_MAX_PER_HOST + ":20}")
	private int maxConnectionPerRoute;

	/**
	 * Maximum total HTTP connections across all routes.
	 * Property: {@link IdRepoConstants#CREDREQ_HTTPCLIENT_MAX_TOTAL} (default {@code 100}).
	 */
	@Value("${" + IdRepoConstants.CREDREQ_HTTPCLIENT_MAX_TOTAL + ":100}")
	private int totalMaxConnection;

	/** Lazily initialized pooled {@link RestTemplate} instance. */
	private RestTemplate restTemplate;

	/**
	 * Issues an HTTP POST to the service identified by {@code apiName}.
	 *
	 * @param <T>             response type
	 * @param apiName         target service and URL property key
	 * @param pathsegments    optional URI path segments appended to the base URL
	 * @param queryParamName  comma-separated query parameter names, or {@code null}
	 * @param queryParamValue comma-separated query parameter values aligned with names
	 * @param mediaType       {@code Content-Type} for the request body, or {@code null}
	 * @param requestType     request body object or pre-built {@link HttpEntity}
	 * @param responseClass   expected response type
	 * @return deserialized response body
	 * @throws Exception when the HTTP call fails or the base URL is missing
	 */
	@SuppressWarnings("unchecked")
	public <T> T postApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
			MediaType mediaType, Object requestType, Class<?> responseClass) throws Exception {
		T result = null;
		String apiHostIpPort = environment.getProperty(apiName.getServiceName());
		UriComponentsBuilder builder = null;
		if (apiHostIpPort != null)
			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
		if (builder != null) {

			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}

			}
			if (!((queryParamName == null) || (("").equals(queryParamName)))) {
				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");

				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}
			}

			RestTemplate restTemplate;

			try {
				restTemplate = getRestTemplate();
				result = (T) restTemplate.postForObject(builder.toUriString(), setRequestHeader(requestType, mediaType),
						responseClass);

			} catch (Exception e) {
				throw new Exception(e);
			}
		}
		return result;
	}

	/**
	 * Issues an HTTP GET to the service identified by {@code apiName}.
	 *
	 * @param <T>             response type
	 * @param apiName         target service; base URL resolved from {@link ApiName#name()}
	 * @param pathsegments    optional URI path segments
	 * @param queryParamName  comma-separated query parameter names, or {@code null}
	 * @param queryParamValue comma-separated query parameter values
	 * @param responseType    expected response type
	 * @return deserialized response body, or {@code null} when base URL is unset
	 * @throws Exception when the HTTP exchange fails
	 */
	@SuppressWarnings("unchecked")
	public <T> T getApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
			Class<?> responseType) throws Exception {

		String apiHostIpPort = environment.getProperty(apiName.name());
		T result = null;
		UriComponentsBuilder builder = null;
		UriComponents uriComponents = null;
		if (apiHostIpPort != null) {

			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}

			}

			if (!((queryParamName == null) || (("").equals(queryParamName)))) {

				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");
				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}

			}
			uriComponents = builder.build(false).encode();
			RestTemplate restTemplate;

			try {
				restTemplate = getRestTemplate();
				result = (T) restTemplate
						.exchange(uriComponents.toUri(), HttpMethod.GET, setRequestHeader(null, null), responseType)
						.getBody();
			} catch (Exception e) {
				throw new Exception(e);
			}

		}
		return result;
	}

	/**
	 * Returns a singleton pooled {@link RestTemplate} for outbound HTTP calls.
	 *
	 * @return configured REST client with connection pooling and cookies disabled
	 * @throws KeyManagementException   when TLS setup fails
	 * @throws NoSuchAlgorithmException when the JVM lacks required algorithms
	 * @throws KeyStoreException        when keystore access fails
	 */
	public RestTemplate getRestTemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if (restTemplate == null) {

			var connnectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
					.setMaxConnPerRoute(maxConnectionPerRoute)
					.setMaxConnTotal(totalMaxConnection);
			var connectionManager = connnectionManagerBuilder.build();
			HttpClientBuilder httpClientBuilder = HttpClients.custom()
					.setConnectionManager(connectionManager)
					.disableCookieManagement();

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClientBuilder.build());

			restTemplate = new RestTemplate(requestFactory);
		}
		return restTemplate;
	}

	/**
	 * Builds an {@link HttpEntity} with MOSIP auth cookie and optional content type.
	 *
	 * @param requestType request body or existing {@link HttpEntity}; may be {@code null} for GET
	 * @param mediaType   {@code Content-Type} header value, or {@code null}
	 * @return entity ready for {@link RestTemplate} exchange
	 * @throws IOException when token retrieval fails
	 */
	private HttpEntity<Object> setRequestHeader(Object requestType, MediaType mediaType) throws IOException {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Cookie", getToken());
		if (mediaType != null) {
			headers.add("Content-Type", mediaType.toString());
		}
		if (requestType != null) {
			try {
				HttpEntity<Object> httpEntity = (HttpEntity<Object>) requestType;
				HttpHeaders httpHeader = httpEntity.getHeaders();
				for (String key : httpHeader.headerNames()) {
					if (!(headers.containsKey(CONTENT_TYPE) && key.equals(CONTENT_TYPE)))
					{
						headers.add(key, Objects.requireNonNull(httpHeader.get(key)).get(0));
					}
				}
				return new HttpEntity<Object>(httpEntity.getBody(), headers);
			} catch (ClassCastException e) {
				return new HttpEntity<Object>(requestType, headers);
			}
		} else
			return new HttpEntity<Object>(headers);
	}

	/**
	 * Returns a valid MOSIP auth cookie header value, refreshing from Keycloak when expired.
	 * <p>
	 * Caches the token in {@code System.setProperty("token", ...)} and validates via
	 * {@link TokenHandlerUtil#isValidBearerToken}. New tokens are obtained from
	 * {@code KEYBASEDTOKENAPI} using secret-key credentials from {@link EnvUtil}.
	 * </p>
	 *
	 * @return cookie header fragment ({@code Authorization=<token>} or full {@code Set-Cookie} prefix)
	 * @throws IOException when the token endpoint returns no cookie
	 */
	public String getToken() throws IOException {
		String token = System.getProperty("token");
		boolean isValid = false;

		if (StringUtils.isNotEmpty(token)) {

			isValid = TokenHandlerUtil.isValidBearerToken(token,
					EnvUtil.getCredReqTokenIssuerUrl(),
					EnvUtil.getCredReqTokenClientId());

		}
		if (!isValid) {
			TokenRequestDTO<SecretKeyRequest> tokenRequestDTO = new TokenRequestDTO<SecretKeyRequest>();
			tokenRequestDTO.setId(EnvUtil.getCredReqTokenRequestId());
			tokenRequestDTO.setMetadata(new Metadata());

			tokenRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
			tokenRequestDTO.setRequest(setSecretKeyRequestDTO());
			tokenRequestDTO.setVersion(EnvUtil.getCredReqTokenVersion());

			Gson gson = new Gson();
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(environment.getProperty("KEYBASEDTOKENAPI"));
			try {
				StringEntity postingString = new StringEntity(gson.toJson(tokenRequestDTO));
				post.setEntity(postingString);
				post.setHeader("Content-type", "application/json");
				CloseableHttpResponse response = httpClient.execute(post);
				org.apache.hc.core5.http.HttpEntity entity = response.getEntity();
				String responseBody = EntityUtils.toString(entity, "UTF-8");
				Header[] cookie = response.getHeaders("Set-Cookie");
				if (cookie.length == 0)
					throw new IOException("cookie is empty. Could not generate new token.");
				token = response.getHeaders("Set-Cookie")[0].getValue();
				System.setProperty("token", token.substring(14, token.indexOf(';')));
				return token.substring(0, token.indexOf(';'));
			} catch (IOException e) {
				throw e;
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
		return AUTHORIZATION + token;
	}

	/**
	 * Builds the secret-key block for Keycloak token requests.
	 *
	 * @return populated {@link SecretKeyRequest} from {@link EnvUtil} credential-request token config
	 */
	private SecretKeyRequest setSecretKeyRequestDTO() {
		SecretKeyRequest request = new SecretKeyRequest();
		request.setAppId(EnvUtil.getCredReqTokenAppId());
		request.setClientId(EnvUtil.getCredReqTokenClientId());
		request.setSecretKey(EnvUtil.getCredReqTokenSecretKey());
		return request;
	}
}
