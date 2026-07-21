package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.AuditRequestDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.AuthenticationException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.IdRepoRetryException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.util.RestUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.core.publisher.Mono;

/**
 * The Class RestUtilTest.
 *
 * @author Manoj SP
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestHelperTest {

	/** The rest helper. */
	@InjectMocks
	RestHelper restHelper;

	/** The environment. */
	private MockEnvironment environment;

	/** The mapper. */
	private ObjectMapper mapper;

	/** The audit factory. */
	@InjectMocks
	AuditRequestBuilder auditBuilder;

	/** The rest factory. */
	@InjectMocks
	RestRequestBuilder restBuilder;

	private MockedStatic<WebClient> webClientStatic;
	private MockedStatic<SslContextBuilder> sslContextBuilderStatic;

	/**
	 * Before.
	 *
	 * @throws SSLException the SSL exception
	 */
	@Before
	public void before() throws SSLException {
		environment = new MockEnvironment();
		mapper = new ObjectMapper();
		ReflectionTestUtils.setField(restBuilder, "env", environment);
		ReflectionTestUtils.setField(restHelper, "mapper", mapper);
		sslContextBuilderStatic = Mockito.mockStatic(SslContextBuilder.class);
		SslContextBuilder sslContextBuilder = Mockito.mock(SslContextBuilder.class);
		sslContextBuilderStatic.when(SslContextBuilder::forClient).thenReturn(sslContextBuilder);
		Mockito.when(sslContextBuilder.trustManager(Mockito.any(TrustManagerFactory.class)))
				.thenReturn(sslContextBuilder);
		Mockito.when(sslContextBuilder.build()).thenReturn(Mockito.mock(SslContext.class));
		webClientStatic = Mockito.mockStatic(WebClient.class);
	}

	@After
	public void after() {
		if (webClientStatic != null) {
			webClientStatic.close();
		}
		if (sslContextBuilderStatic != null) {
			sslContextBuilderStatic.close();
		}
	}

	/**
	 * Test req sync.
	 *
	 * @throws JsonParseException   the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException          Signals that an I/O exception has occurred.
	 * @throws RestServiceException the rest service exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testReqSync() throws JsonParseException, JsonMappingException, IOException, RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		Mockito.mock(ClientResponse.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("0.0.0.0");
		restReqDTO.setResponseType(String.class);
		WebClient webClient = Mockito.mock(WebClient.class);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.just(response));
		restHelper.requestSync(restReqDTO);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testReqSyncWithTimeout()
			throws JsonParseException, JsonMappingException, IOException, RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		Mockito.mock(ClientResponse.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("0.0.0.0");
		restReqDTO.setResponseType(String.class);
		restReqDTO.setTimeout(1);
		WebClient webClient = Mockito.mock(WebClient.class);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.just(response));
		restHelper.requestSync(restReqDTO);
	}

	/**
	 * Test req sync with headers.
	 *
	 * @throws JsonParseException   the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException          Signals that an I/O exception has occurred.
	 * @throws RestServiceException the rest service exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testReqSyncWithHeaders()
			throws JsonParseException, JsonMappingException, IOException, RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		Mockito.mock(ClientResponse.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		restReqDTO.setHeaders(headers);
		WebClient webClient = Mockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.headers(Mockito.any())).thenAnswer(invocation -> {
			Consumer<HttpHeaders> consumer = invocation.getArgument(0);
			consumer.accept(new HttpHeaders());
			return requestBodySpec;
		});
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.just(response));
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("0.0.0.0");
		restReqDTO.setResponseType(String.class);
		restHelper.requestSync(restReqDTO);
	}

	/**
	 * Test req sync unknown error.
	 * 
	 * @throws Throwable
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = RestServiceException.class)
	public void testReqSyncUnknownError() throws Throwable {
		try {
				ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			String response = "{\"response\":{\"status\":\"success\"}}";
			RestRequestDTO restReqDTO = new RestRequestDTO();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			restReqDTO.setHeaders(headers);
			restReqDTO.setResponseType(String.class);
			restReqDTO.setUri("");
			WebClient webClient = Mockito.mock(WebClient.class);
			RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
			RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
			Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
			webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.build()).thenReturn(webClient);
			Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
			Function<UriBuilder, URI> uriFunction = Mockito.any();
			Mockito.when(requestBodyUriSpec.uri(uriFunction)).thenReturn(requestBodySpec);
			Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
			Mockito.when(responseSpec.bodyToMono(String.class))
					.thenReturn(Mono.just(response));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause();
		}
	}

	/**
	 * test request sync with params.
	 *
	 * @throws IDDataValidationException the ID data validation exception
	 * @throws RestServiceException      the rest service exception
	 * @throws JsonParseException        the json parse exception
	 * @throws JsonMappingException      the json mapping exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void vtestRequestSyncWithParams() throws IdRepoDataValidationException, RestServiceException,
			JsonParseException, JsonMappingException, IOException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		Mockito.mock(ClientResponse.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setParams(new LinkedMultiValueMap<>(0));
		restReqDTO.setResponseType(String.class);
		WebClient webClient = Mockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.just(response));
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("0.0.0.0");
		restHelper.requestSync(restReqDTO);
	}

	/**
	 * Vtest request sync with path variables.
	 *
	 * @throws IDDataValidationException the ID data validation exception
	 * @throws RestServiceException      the rest service exception
	 * @throws JsonParseException        the json parse exception
	 * @throws JsonMappingException      the json mapping exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void vtestRequestSyncWithPathVariables() throws IdRepoDataValidationException, RestServiceException,
			JsonParseException, JsonMappingException, IOException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		Mockito.mock(ClientResponse.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		Map<String, String> pathVariables = new HashMap<>();
		restReqDTO.setPathVariables(pathVariables);
		WebClient webClient = Mockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.just(response));
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("0.0.0.0");
		restReqDTO.setResponseType(String.class);
		restHelper.requestSync(restReqDTO);
	}

	/**
	 * Utest request sync with timeout.
	 * 
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = RestServiceException.class)
	public void utestRequestSyncWithTimeout() throws Throwable {
		try {
				ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			Mockito.mock(ClientResponse.class);
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setTimeout(1);
			restReqDTO.setResponseType(String.class);
			Map<String, String> pathVariables = new HashMap<>();
			restReqDTO.setPathVariables(pathVariables);
			WebClient webClient = Mockito.mock(WebClient.class);
			RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
			RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
			Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
			webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.build()).thenReturn(webClient);
			Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
			Function<UriBuilder, URI> uriFunction = Mockito.any();
			Mockito.when(requestBodyUriSpec.uri(uriFunction)).thenReturn(requestBodySpec);
			Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
			Mockito.when(responseSpec.bodyToMono(String.class))
					.thenReturn(Mono.error(new RuntimeException((new TimeoutException()))));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause();
		}
	}

	/**
	 * Test request async.
	 *
	 * @throws IDDataValidationException the ID data validation exception
	 * @throws RestServiceException      the rest service exception
	 * @throws JsonParseException        the json parse exception
	 * @throws JsonMappingException      the json mapping exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testRequestAsync() throws IdRepoDataValidationException, RestServiceException, JsonParseException,
			JsonMappingException, IOException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		Mockito.mock(ClientResponse.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		Map<String, String> pathVariables = new HashMap<>();
		restReqDTO.setPathVariables(pathVariables);
		WebClient webClient = Mockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.contentType(Mockito.any(MediaType.class))).thenReturn(requestBodySpec);
		Mockito.doReturn(requestBodySpec).when(requestBodySpec).bodyValue(Mockito.anyString());
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.just(response));
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("0.0.0.0");
		restReqDTO.setResponseType(String.class);
		restReqDTO.setRequestBody(response);
		restHelper.requestAsync(restReqDTO);
	}

	/**
	 * test request sync for 4 xx.
	 * 
	 * @throws Throwable
	 *
	 * @throws IDDataValidationException the ID data validation exception
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = RestServiceException.class)
	public void ztestRequestSyncWebClientResponseException() throws Throwable {
		try {
				ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			Mockito.mock(ClientResponse.class);
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setTimeout(1);
			restReqDTO.setResponseType(String.class);
			Map<String, String> pathVariables = new HashMap<>();
			restReqDTO.setPathVariables(pathVariables);
			WebClient webClient = Mockito.mock(WebClient.class);
			RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
			RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
			Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
			webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.build()).thenReturn(webClient);
			Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
			Function<UriBuilder, URI> uriFunction = Mockito.any();
			Mockito.when(requestBodyUriSpec.uri(uriFunction)).thenReturn(requestBodySpec);
			Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
			Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(
					Mono.error(new WebClientResponseException("message", 200, "statusText", null, null, null)));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause();
		}
	}

	/**
	 * Test handle status error without response body.
	 *
	 * @throws Throwable the throwable
	 */
	@Test
	public void testHandleStatusErrorWithErrorResponseBody() throws Throwable {
		try {
			RestRequestDTO restRequestDTO = new RestRequestDTO();
			restRequestDTO.setHttpMethod(HttpMethod.GET);
			restRequestDTO.setUri("http://test/status-error");
			restRequestDTO.setResponseType(ObjectNode.class);
			WebClient webClient = Mockito.mock(WebClient.class);
			Mockito.when(webClient.method(HttpMethod.GET)).thenThrow(new WebClientResponseException("message", 400,
					"failed", null, "{\"response\":{}}".getBytes(), null));
			ReflectionTestUtils.setField(restHelper, "webClient", webClient);
			restHelper.requestSync(restRequestDTO);
		} catch (RestServiceException e) {
			assertEquals(e.getErrorCode(), IdRepoErrorConstants.CLIENT_ERROR.getErrorCode());
			assertEquals(e.getErrorText(), IdRepoErrorConstants.CLIENT_ERROR.getErrorMessage());
		}
	}

	@Test
	public void testHandleTimeoutException() throws Throwable {
		try {
			RestRequestDTO restRequestDTO = new RestRequestDTO();
			restRequestDTO.setParams(new LinkedMultiValueMap<>(0));
			restRequestDTO.setPathVariables(Collections.singletonMap("", ""));
			restRequestDTO.setUri("0.0.0.0");
			restRequestDTO.setResponseType(String.class);
			WebClient webClient = Mockito.mock(WebClient.class);
			Mockito.when(webClient.method(Mockito.any()))
					.thenThrow(new RuntimeException(new TimeoutException("")));
			ReflectionTestUtils.setField(restHelper, "webClient", webClient);
			restHelper.requestSync(restRequestDTO);
		} catch (IdRepoRetryException e) {
			assertEquals(e.getErrorCode(), IdRepoErrorConstants.CONNECTION_TIMED_OUT.getErrorCode());
			assertEquals(e.getErrorText(), IdRepoErrorConstants.CONNECTION_TIMED_OUT.getErrorMessage());
		}
	}

	/**
	 * Test handle status error without response body unauthorised error.
	 *
	 * @throws Throwable the throwable
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testHandleStatusErrorWithoutResponseBodyUnauthorisedError() throws Throwable {
		try {
			String response = "{\"errors\":[{\"errorCode\":\"KER-ATH-402\"}]}";
			ReflectionTestUtils.invokeMethod(restHelper, "handleStatusError",
					new WebClientResponseException("message", 401, "failed", null, response.getBytes(), null),
					String.class);
		} catch (UndeclaredThrowableException | AuthenticationException e) {
			if (Objects.nonNull(e.getCause())) {
				AuthenticationException ex = (AuthenticationException) e.getCause();
				assertEquals(ex.getErrorCode(), "KER-ATH-402");
				assertTrue(Objects.isNull(ex.getErrorText()));
			} else {
				assertEquals(((AuthenticationException) e).getErrorCode(), "KER-ATH-402");
				assertTrue(Objects.isNull(((AuthenticationException) e).getErrorText()));
			}
		}
	}

	/**
	 * Test handle status error 4 xx.
	 *
	 * @throws Throwable the throwable
	 */
	@Test(expected = RestServiceException.class)
	public void testHandleStatusError4xx() throws Throwable {
		try {
			ReflectionTestUtils.invokeMethod(restHelper, "handleStatusError", new WebClientResponseException("message",
					400, "failed", null, mapper.writeValueAsBytes(new AuditRequestDTO()), null), AuditRequestDTO.class);
		} catch (Exception e) {
			throw ExceptionUtils.getRootCause(e);
		}
	}

	/**
	 * Test handle status error 5 xx.
	 *
	 * @throws Throwable the throwable
	 */
	@Test(expected = RestServiceException.class)
	public void testHandleStatusError5xx() throws Throwable {
		try {
			assertTrue(
					ReflectionTestUtils.invokeMethod(
							restHelper, "handleStatusError", new WebClientResponseException("message", 500, "failed",
									null, mapper.writeValueAsBytes(new AuditRequestDTO()), null),
							AuditRequestDTO.class));
		} catch (Exception e) {
			throw ExceptionUtils.getRootCause(e);
		}
	}

	@Test
	public void testHandleStatusErrorIOException() throws Throwable {
		try {
			assertTrue(ReflectionTestUtils
					.invokeMethod(restHelper, "handleStatusError",
							new WebClientResponseException("message", 500, "failed", null,
									mapper.writeValueAsBytes(new AuditRequestDTO()), null),
							RestRequestDTO.class)
					.getClass().equals(RestServiceException.class));
		} catch (UndeclaredThrowableException e) {
			RestServiceException ex = (RestServiceException) e.getCause();
			assertEquals(ex.getErrorCode(), IdRepoErrorConstants.CLIENT_ERROR.getErrorCode());
			assertEquals(ex.getErrorText(), IdRepoErrorConstants.CLIENT_ERROR.getErrorMessage());
		}
	}

	/**
	 * Test check error response exception.
	 *
	 * @throws Throwable the throwable
	 */
	@Test
	public void testCheckErrorResponseException() throws Throwable {
		try {
			String response = "{\"errors\":[{\"errorCode\":\"\"}]}";
			ReflectionTestUtils.invokeMethod(restHelper, "checkErrorResponse",
					mapper.readValue(response.getBytes(), Object.class), ObjectNode.class);
		} catch (UndeclaredThrowableException e) {
			RestServiceException ex = (RestServiceException) e.getCause();
			assertEquals(ex.getErrorCode(), IdRepoErrorConstants.CLIENT_ERROR.getErrorCode());
			assertEquals(ex.getErrorText(), IdRepoErrorConstants.CLIENT_ERROR.getErrorMessage());
		}
	}

	@Test
	public void testCheckErrorResponseIOException() throws Throwable {
		try {
			String response = "{\"errors\":[{\"errorCode\":\"\"}]}";
			ReflectionTestUtils.invokeMethod(restHelper, "checkErrorResponse",
					mapper.readValue(response.getBytes(), Object.class), RestRequestDTO.class);
		} catch (UndeclaredThrowableException e) {
			RestServiceException ex = (RestServiceException) e.getCause();
			assertEquals(ex.getErrorCode(), IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode());
			assertEquals(ex.getErrorText(), IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage());
		}
	}

	/**
	 * Test check error response retry.
	 *
	 * @throws JsonParseException   the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException          Signals that an I/O exception has occurred.
	 */
	@Test
	public void testCheckErrorResponseRetry() throws JsonParseException, JsonMappingException, IOException {
		try {
			String response = "{\"errors\":[{\"errorCode\":\"KER-ATH-401\"}]}";
			ReflectionTestUtils.invokeMethod(restHelper, "checkErrorResponse",
					mapper.readValue(response.getBytes(), Object.class), ObjectNode.class);
		} catch (UndeclaredThrowableException e) {
			RestServiceException cause = (RestServiceException) e.getCause();
			assertEquals(cause.getErrorCode(), IdRepoErrorConstants.CLIENT_ERROR.getErrorCode());
			assertEquals(cause.getErrorText(), IdRepoErrorConstants.CLIENT_ERROR.getErrorMessage());
		}
	}

	@Test
	public void testInitResolvesSelfTokenWebClient() {
		RestHelper helper = new RestHelper();
		ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
		WebClient sourceClient = Mockito.mock(WebClient.class);
		Builder mutateBuilder = Mockito.mock(Builder.class);
		WebClient mutatedClient = Mockito.mock(WebClient.class);
		Mockito.when(ctx.containsBean("selfTokenWebClient")).thenReturn(true);
		Mockito.when(ctx.getBean("selfTokenWebClient", WebClient.class)).thenReturn(sourceClient);
		Mockito.when(sourceClient.mutate()).thenReturn(mutateBuilder);
		Mockito.when(mutateBuilder.exchangeStrategies(Mockito.any())).thenReturn(mutateBuilder);
		Mockito.when(mutateBuilder.build()).thenReturn(mutatedClient);
		ReflectionTestUtils.setField(helper, "ctx", ctx);
		ReflectionTestUtils.setField(helper, "mapper", mapper);
		ReflectionTestUtils.setField(helper, "maxInMemorySize", 20971520);
		helper.init();
		assertEquals(mutatedClient, ReflectionTestUtils.getField(helper, "webClient"));
	}

	@Test
	public void testInitResolvesWebClientBean() {
		RestHelper helper = new RestHelper();
		ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
		WebClient sourceClient = Mockito.mock(WebClient.class);
		Builder mutateBuilder = Mockito.mock(Builder.class);
		WebClient mutatedClient = Mockito.mock(WebClient.class);
		Mockito.when(ctx.containsBean("selfTokenWebClient")).thenReturn(false);
		Mockito.when(ctx.getBean("webClient", WebClient.class)).thenReturn(sourceClient);
		Mockito.when(sourceClient.mutate()).thenReturn(mutateBuilder);
		Mockito.when(mutateBuilder.exchangeStrategies(Mockito.any())).thenReturn(mutateBuilder);
		Mockito.when(mutateBuilder.build()).thenReturn(mutatedClient);
		ReflectionTestUtils.setField(helper, "ctx", ctx);
		ReflectionTestUtils.setField(helper, "mapper", mapper);
		ReflectionTestUtils.setField(helper, "maxInMemorySize", 1024);
		helper.init();
		assertEquals(mutatedClient, ReflectionTestUtils.getField(helper, "webClient"));
	}

	@Test
	public void testInitMutatesConstructorInjectedWebClient() {
		WebClient sourceClient = Mockito.mock(WebClient.class);
		Builder mutateBuilder = Mockito.mock(Builder.class);
		WebClient mutatedClient = Mockito.mock(WebClient.class);
		RestHelper helper = new RestHelper(sourceClient);
		Mockito.when(sourceClient.mutate()).thenReturn(mutateBuilder);
		Mockito.when(mutateBuilder.exchangeStrategies(Mockito.any())).thenReturn(mutateBuilder);
		Mockito.when(mutateBuilder.build()).thenReturn(mutatedClient);
		ReflectionTestUtils.setField(helper, "mapper", mapper);
		ReflectionTestUtils.setField(helper, "maxInMemorySize", 4096);
		helper.init();
		assertEquals(mutatedClient, ReflectionTestUtils.getField(helper, "webClient"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRequestSyncTypedObjectNodeResponse() throws RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("http://test");
		restReqDTO.setResponseType(ObjectNode.class);
		WebClient webClient = setupWebClientMocks(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
		ObjectNode result = restHelper.requestSync(restReqDTO);
		assertNotNull(result);
		assertEquals("success", result.get("response").get("status").asText());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = RestServiceException.class)
	public void testRequestSyncTypedResponseWithErrorsInBody() throws Throwable {
		try {
			ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			String response = "{\"errors\":[{\"errorCode\":\"ERR-001\"}],\"response\":{}}";
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setHttpMethod(HttpMethod.GET);
			restReqDTO.setUri("http://test");
			restReqDTO.setResponseType(AuditRequestDTO.class);
			setupWebClientMocks(responseSpec);
			Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause() != null ? e.getCause() : e;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testDecodeResponseByteArray() {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		byte[] payload = "binary-payload".getBytes();
		Mockito.when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(payload));
		Mono<?> mono = ReflectionTestUtils.invokeMethod(restHelper, "decodeResponse", responseSpec, byte[].class);
		assertArrayEquals(payload, (byte[]) mono.block());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRequestSyncByteArrayResponse() throws RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		byte[] payload = "binary-payload".getBytes();
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("http://test");
		restReqDTO.setResponseType(byte[].class);
		setupWebClientMocks(responseSpec);
		Mockito.when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(payload));
		byte[] result = restHelper.requestSync(restReqDTO);
		assertArrayEquals(payload, result);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRequestSyncWithParamsAndPathVariables() throws RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("q", "1");
		restReqDTO.setParams(params);
		restReqDTO.setPathVariables(Collections.singletonMap("id", "99"));
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("http://test/{id}");
		restReqDTO.setResponseType(String.class);
		setupWebClientMocks(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
		assertEquals(response, restHelper.requestSync(restReqDTO));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = RestServiceException.class)
	public void testRequestSyncWebClientResponseExceptionFromBlock() throws Throwable {
		try {
			ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setHttpMethod(HttpMethod.GET);
			restReqDTO.setUri("http://test");
			restReqDTO.setResponseType(AuditRequestDTO.class);
			setupWebClientMocks(responseSpec);
			byte[] errorBody = mapper.writeValueAsBytes(new AuditRequestDTO());
			Mockito.when(responseSpec.bodyToMono(String.class))
					.thenReturn(Mono.error(new WebClientResponseException("message", 400, "failed", null, errorBody, null)));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause() != null ? e.getCause() : e;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRequestSyncWebClientResponseExceptionHandleStatusErrorReturns() throws Throwable {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setHttpMethod(HttpMethod.GET);
		restReqDTO.setUri("http://test");
		restReqDTO.setResponseType(AuditRequestDTO.class);
		setupWebClientMocks(responseSpec);
		Mockito.when(responseSpec.bodyToMono(String.class))
				.thenReturn(Mono.error(new WebClientResponseException("message", 404, "failed", null,
						"not-json".getBytes(), null)));
		try {
			restHelper.requestSync(restReqDTO);
		} catch (RestServiceException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertTrue(e.getCause() instanceof IOException);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = RestServiceException.class)
	public void testRequestSyncRethrowsRestServiceExceptionFromCheckError() throws Throwable {
		try (MockedStatic<RestUtil> restUtilStatic = Mockito.mockStatic(RestUtil.class)) {
			ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			String response = "{\"errors\":[{\"errorCode\":\"ERR\"}]}";
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setHttpMethod(HttpMethod.GET);
			restReqDTO.setUri("http://test");
			restReqDTO.setResponseType(ObjectNode.class);
			setupWebClientMocks(responseSpec);
			restUtilStatic.when(() -> RestUtil.containsError(Mockito.anyString(), Mockito.any())).thenReturn(false);
			Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause() != null ? e.getCause() : e;
		}
	}

	@Test(expected = UncheckedIOException.class)
	public void testWriteJsonBodyIOException() throws JsonProcessingException {
		ObjectMapper failingMapper = Mockito.mock(ObjectMapper.class);
		ObjectNode node = mapper.createObjectNode();
		node.put("key", "value");
		try {
			Mockito.when(failingMapper.valueToTree(Mockito.any())).thenReturn(node);
			Mockito.when(failingMapper.writeValueAsString(Mockito.any())).thenThrow(new JsonProcessingException("serialization failed") {
			});
			ReflectionTestUtils.setField(restHelper, "mapper", failingMapper);
			ReflectionTestUtils.invokeMethod(restHelper, "writeJsonBody", Collections.singletonMap("key", "value"));
		} finally {
			ReflectionTestUtils.setField(restHelper, "mapper", mapper);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = RestServiceException.class)
	public void testRequestSyncCheckErrorResponseOnTypedResponse() throws Throwable {
		try {
			ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			String response = "{\"errors\":[{\"errorCode\":\"ERR\"}]}";
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setHttpMethod(HttpMethod.GET);
			restReqDTO.setUri("http://test");
			restReqDTO.setResponseType(ObjectNode.class);
			setupWebClientMocks(responseSpec);
			Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause() != null ? e.getCause() : e;
		}
	}

	@Test
	public void testCheckErrorResponseNullResponse() {
		try {
			ReflectionTestUtils.invokeMethod(restHelper, "checkErrorResponse", null, ObjectNode.class);
		} catch (UndeclaredThrowableException e) {
			RestServiceException ex = (RestServiceException) e.getCause();
			assertEquals(IdRepoErrorConstants.CLIENT_ERROR.getErrorCode(), ex.getErrorCode());
		}
	}

	@Test
	public void testHandleStatusError403Forbidden() {
		boolean caught = false;
		try {
			String response = "{\"errors\":[{\"errorCode\":\"KER-ATH-403\",\"message\":\"Forbidden\"}]}";
			ReflectionTestUtils.invokeMethod(restHelper, "handleStatusError",
					new WebClientResponseException("message", 403, "failed", null, response.getBytes(), null),
					String.class);
		} catch (UndeclaredThrowableException e) {
			caught = true;
			Throwable cause = e.getCause();
			if (cause instanceof IdRepoRetryException) {
				assertTrue(cause.getCause() instanceof AuthenticationException);
				assertEquals("KER-ATH-403", ((AuthenticationException) cause.getCause()).getErrorCode());
			} else {
				assertTrue(cause instanceof AuthenticationException);
				assertEquals("KER-ATH-403", ((AuthenticationException) cause).getErrorCode());
			}
		} catch (IdRepoRetryException e) {
			caught = true;
			assertTrue(e.getCause() instanceof AuthenticationException);
			assertEquals("KER-ATH-403", ((AuthenticationException) e.getCause()).getErrorCode());
		} catch (AuthenticationException e) {
			caught = true;
			assertEquals("KER-ATH-403", e.getErrorCode());
			assertEquals(403, e.getStatusCode());
		}
		assertTrue(caught);
	}

	@Test
	public void testRequestAsyncFailure() throws Exception {
		RestHelper spy = Mockito.spy(restHelper);
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setUri("http://test");
		Mockito.doThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR)).when(spy).requestSync(restReqDTO);
		CompletableFuture<Object> future = spy.requestAsync(restReqDTO);
		assertTrue(future.isCompletedExceptionally());
		try {
			future.get();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof RestServiceException);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRequestSyncNormalizesUtcTimestampsInBody() throws RestServiceException {
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
		String response = "{\"response\":{\"status\":\"success\"}}";
		RestRequestDTO restReqDTO = new RestRequestDTO();
		restReqDTO.setHttpMethod(HttpMethod.POST);
		restReqDTO.setUri("http://test");
		restReqDTO.setResponseType(String.class);
		Map<String, Object> inner = new HashMap<>();
		inner.put("actionTimeStamp", "2024-01-01T12:00:00.123");
		inner.put("timeStamp", "2024-01-01T12:00:00.456");
		Map<String, Object> body = new HashMap<>();
		body.put("requesttime", "2024-01-01T12:00:00.789");
		body.put("request", inner);
		restReqDTO.setRequestBody(body);
		RequestBodySpec requestBodySpec = setupWebClientMocksWithBody(responseSpec);
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		Mockito.doReturn(requestBodySpec).when(requestBodySpec).bodyValue(bodyCaptor.capture());
		Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
		restHelper.requestSync(restReqDTO);
		String sentBody = bodyCaptor.getValue();
		assertTrue(sentBody.contains("2024-01-01T12:00:00.123Z"));
		assertTrue(sentBody.contains("2024-01-01T12:00:00.456Z"));
		assertTrue(sentBody.contains("2024-01-01T12:00:00.789Z"));
	}

	@Test
	public void testSummarizeRequestBodyRedactsDataAndSalt() throws Exception {
		Map<String, Object> inner = new HashMap<>();
		inner.put("data", "cipherTextValue");
		inner.put("salt", "saltValue");
		Map<String, Object> root = new HashMap<>();
		root.put("request", inner);
		String json = mapper.writeValueAsString(root);
		String summarized = ReflectionTestUtils.invokeMethod(restHelper, "summarizeRequestBody", json);
		assertTrue(summarized.contains("<redacted len="));
	}

	@Test
	public void testSummarizeRequestBodyInvalidJsonReturnsOriginal() {
		String badJson = "not-json";
		String summarized = ReflectionTestUtils.invokeMethod(restHelper, "summarizeRequestBody", badJson);
		assertEquals(badJson, summarized);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected = RestServiceException.class)
	public void testRequestSyncUnwrapsRestServiceException() throws Throwable {
		try {
			ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);
			RestRequestDTO restReqDTO = new RestRequestDTO();
			restReqDTO.setHttpMethod(HttpMethod.GET);
			restReqDTO.setUri("http://test");
			restReqDTO.setResponseType(String.class);
			setupWebClientMocks(responseSpec);
			Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(
					new RuntimeException(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR))));
			restHelper.requestSync(restReqDTO);
		} catch (Exception e) {
			throw e.getCause() != null ? e.getCause() : e;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private WebClient setupWebClientMocks(ResponseSpec responseSpec) {
		WebClient webClient = Mockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		return webClient;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private RequestBodySpec setupWebClientMocksWithBody(ResponseSpec responseSpec) {
		WebClient webClient = Mockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
		Builder mockBuilder = Mockito.mock(Builder.class);
		webClientStatic.when(WebClient::builder).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		Mockito.when(mockBuilder.build()).thenReturn(webClient);
		ReflectionTestUtils.setField(restHelper, "webClient", webClient);
		Mockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.contentType(Mockito.any(MediaType.class))).thenReturn(requestBodySpec);
		Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		return requestBodySpec;
	}
}