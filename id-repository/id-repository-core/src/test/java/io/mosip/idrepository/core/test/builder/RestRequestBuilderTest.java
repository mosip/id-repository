package io.mosip.idrepository.core.test.builder;



import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



import org.junit.Before;

import org.junit.FixMethodOrder;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.MethodSorters;

import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.util.LinkedMultiValueMap;



import io.mosip.idrepository.core.builder.AuditRequestBuilder;

import io.mosip.idrepository.core.builder.RestRequestBuilder;

import io.mosip.idrepository.core.constant.AuditEvents;

import io.mosip.idrepository.core.constant.AuditModules;

import io.mosip.idrepository.core.constant.IdType;

import io.mosip.idrepository.core.constant.RestServicesConstants;

import io.mosip.idrepository.core.dto.AuditRequestDTO;

import io.mosip.idrepository.core.dto.AuditResponseDTO;

import io.mosip.idrepository.core.dto.RestRequestDTO;

import io.mosip.idrepository.core.exception.IdRepoDataValidationException;

import io.mosip.idrepository.core.test.support.TestEnvSupport;

import io.mosip.kernel.core.http.RequestWrapper;



/**

 * @author Manoj SP

 *

 */

@RunWith(MockitoJUnitRunner.class)

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class RestRequestBuilderTest {



	RestRequestBuilder restBuilder;



	AuditRequestBuilder auditBuilder;



	MockEnvironment env;



	@Before

	public void before() {

		env = TestEnvSupport.loadTestEnvironment();

		TestEnvSupport.initEnvUtil(env);

		restBuilder = new RestRequestBuilder();

		auditBuilder = new AuditRequestBuilder();

		ReflectionTestUtils.setField(restBuilder, "serviceNames", Arrays.stream(RestServicesConstants.values())

				.map(RestServicesConstants::getServiceName).collect(Collectors.toList()));

		ReflectionTestUtils.setField(restBuilder, "env", env);

		ReflectionTestUtils.invokeMethod(restBuilder, "init");

	}



	private void reloadAuditServiceConfig(MockEnvironment environment) {

		ReflectionTestUtils.setField(restBuilder, "env", environment);

		@SuppressWarnings("unchecked")
		Map<?, ?> cache = (Map<?, ?>) ReflectionTestUtils.getField(restBuilder, "serviceConfigs");
		cache.clear();

		ReflectionTestUtils.invokeMethod(restBuilder, "init");

	}

	/** Spring skips blank values in {@code setProperty}; mutate the test property source instead. */
	private void overrideProperty(MockEnvironment environment, String key, String value) {
		TestEnvSupport.setProperty(environment, key, value);
	}



	private MockEnvironment freshEnv() {
		return TestEnvSupport.loadTestEnvironment();
	}



	@Test

	public void testBuildRequest() throws IdRepoDataValidationException {

		RequestWrapper<AuditRequestDTO> auditRequest = auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,

				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID, "desc");

		auditRequest.getRequest().setActionTimeStamp(null);



		RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditRequest,

				AuditResponseDTO.class);



		RestRequestDTO testRequest = new RestRequestDTO();

		String serviceName = RestServicesConstants.AUDIT_MANAGER_SERVICE.getServiceName();

		String uri = env.getProperty(serviceName.concat(".rest.uri"));

		String httpMethod = env.getProperty(serviceName.concat(".rest.httpMethod"));

		String mediaType = env.getProperty(serviceName.concat(".rest.headers.mediaType"));

		String timeout = env.getProperty(serviceName.concat(".rest.timeout"));



		testRequest.setUri(uri);

		testRequest.setHttpMethod(HttpMethod.valueOf(httpMethod));

		testRequest.setRequestBody(auditRequest);

		testRequest.setResponseType(AuditResponseDTO.class);

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.valueOf(mediaType));

		testRequest.setHeaders(headers);

		testRequest.setTimeout(Integer.parseInt(timeout));



		request.setHeaders(null);

		testRequest.setHeaders(null);

	}



	@Test(expected = IdRepoDataValidationException.class)

	public void testBuildRequestWithMultiValueMap() throws IdRepoDataValidationException {



		MockEnvironment environment = freshEnv();

		overrideProperty(environment, "mosip.idrepo.audit.rest.headers.mediaType", "multipart/form-data");

		overrideProperty(environment, "mosip.idrepo.audit.rest.uri.queryparam.test", "yes");

		overrideProperty(environment, "mosip.idrepo.audit.rest.uri.pathparam.test", "yes");

		reloadAuditServiceConfig(environment);



		RequestWrapper<AuditRequestDTO> auditRequest = auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,

				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID, "desc");

		auditRequest.getRequest().setActionTimeStamp(null);



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditRequest,

				AuditResponseDTO.class);



	}



	@Test(expected = IdRepoDataValidationException.class)

	public void testBuildRequestEmptyUri() throws IdRepoDataValidationException {



		MockEnvironment environment = freshEnv();

		overrideProperty(environment, "mosip.idrepo.audit.rest.uri", "");

		reloadAuditServiceConfig(environment);



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditBuilder

				.buildRequest(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",

						IdType.ID, "desc"),

				AuditResponseDTO.class);

	}



	@Test(expected = IdRepoDataValidationException.class)

	public void testBuildRequestNullProperties() throws IdRepoDataValidationException {



		MockEnvironment environment = freshEnv();

		overrideProperty(environment, "mosip.idrepo.audit.rest.uri", "");

		overrideProperty(environment, "mosip.idrepo.audit.rest.headers.mediaType", "");

		overrideProperty(environment, "mosip.idrepo.audit.rest.httpMethod", "");

		overrideProperty(environment, "mosip.idrepo.audit.rest.timeout", "");

		reloadAuditServiceConfig(environment);



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditBuilder

				.buildRequest(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",

						IdType.ID, "desc"),

				AuditResponseDTO.class);

	}



	@Test(expected = IdRepoDataValidationException.class)

	public void testBuildRequestEmptyHttpMethod() throws IdRepoDataValidationException {



		MockEnvironment environment = freshEnv();

		overrideProperty(environment, "mosip.idrepo.audit.rest.httpMethod", "");

		reloadAuditServiceConfig(environment);



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditBuilder

				.buildRequest(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",

						IdType.ID, "desc"),

				AuditResponseDTO.class);

	}



	@Test(expected = IdRepoDataValidationException.class)

	public void testBuildRequestEmptyResponseType() throws IdRepoDataValidationException {



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditBuilder.buildRequest(

				AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID,

				"desc"), null);

	}



	@Test

	public void testBuildRequestMultiValueMap() throws IdRepoDataValidationException {

		MockEnvironment environment = freshEnv();

		overrideProperty(environment, "mosip.idrepo.audit.rest.headers.mediaType", "multipart/form-data");

		overrideProperty(environment, "mosip.idrepo.audit.rest.uri.queryparam.test", "yes");

		overrideProperty(environment, "mosip.idrepo.audit.rest.uri.pathparam.test", "yes");

		reloadAuditServiceConfig(environment);

		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, new LinkedMultiValueMap<String, String>(),

				Object.class);

	}



	@Test

	public void testBuildRequestEmptyTimeout() throws IdRepoDataValidationException {



		MockEnvironment environment = freshEnv();

		TestEnvSupport.setProperty(environment,"mosip.idrepo.audit.rest.timeout", "");

		reloadAuditServiceConfig(environment);



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditBuilder

				.buildRequest(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",

						IdType.ID, "desc"),

				AuditResponseDTO.class);



	}



	@Test

	public void testBuildRequestHeaders() throws IdRepoDataValidationException {



		MockEnvironment environment = freshEnv();

		TestEnvSupport.setProperty(environment,"mosip.idrepo.audit.rest.headers.accept", "application/json");

		reloadAuditServiceConfig(environment);



		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditBuilder

				.buildRequest(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",

						IdType.ID, "desc"),

				AuditResponseDTO.class);

	}

	@Test
	public void testBuildRequestInvalidMediaTypeAtInit() throws IdRepoDataValidationException {
		MockEnvironment environment = freshEnv();
		overrideProperty(environment, "mosip.idrepo.audit.rest.headers.mediaType", "%%%invalid");
		reloadAuditServiceConfig(environment);
		try {
			restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE,
					auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,
							AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID, "desc"),
					AuditResponseDTO.class);
			org.junit.Assert.fail("Expected IdRepoDataValidationException");
		} catch (IdRepoDataValidationException expected) {
			// loadServiceConfig defers invalid media type; createHeaders validates it
		}
	}

	@Test(expected = IdRepoDataValidationException.class)
	public void testBuildRequestInvalidMediaType() throws IdRepoDataValidationException {
		MockEnvironment environment = freshEnv();
		overrideProperty(environment, "mosip.idrepo.audit.rest.headers.mediaType", "not-a-valid-media-type");
		reloadAuditServiceConfig(environment);
		restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE,
				auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,
						AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID, "desc"),
				AuditResponseDTO.class);
	}

	@Test
	public void testBuildRequestWithPathVariables() throws IdRepoDataValidationException {
		RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE,
				Map.of("test", "value"),
				auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,
						AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID, "desc"),
				AuditResponseDTO.class);
		assertNotNull(request.getPathVariables());
		assertEquals("value", request.getPathVariables().get("test"));
	}

	@Test
	public void testBuildRequestLazyLoadsUncachedService() throws IdRepoDataValidationException {
		@SuppressWarnings("unchecked")
		Map<Object, Object> cache = (Map<Object, Object>) ReflectionTestUtils.getField(restBuilder, "serviceConfigs");
		cache.clear();
		RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE,
				auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,
						AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", IdType.ID, "desc"),
				AuditResponseDTO.class);
		assertNotNull(request.getUri());
	}

	@Test
	public void testBuildRequestWithoutRequestBody() throws IdRepoDataValidationException {
		RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, null,
				AuditResponseDTO.class);
		assertNotNull(request.getUri());
	}

	@Test
	public void testRestServiceConfigCreateHeadersUsesRawMediaTypeWhenContentTypeNull() throws Exception {
		Class<?> configClass = Class.forName("io.mosip.idrepository.core.builder.RestRequestBuilder$RestServiceConfig");
		java.lang.reflect.Constructor<?> ctor = configClass.getDeclaredConstructor(String.class,
				org.springframework.http.HttpMethod.class, Integer.class, MediaType.class, String.class);
		ctor.setAccessible(true);
		Object config = ctor.newInstance("http://localhost", HttpMethod.GET, 1, null, "application/json");
		HttpHeaders headers = (HttpHeaders) ReflectionTestUtils.invokeMethod(config, "createHeaders");
		assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
	}



}

