package io.mosip.idrepository.core.test.builder;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.test.support.TestEnvSupport;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuditRequestDTO;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.http.RequestWrapper;

/**
 * @author Manoj SP
 *
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuditRequestBuilderTest {

	AuditRequestBuilder auditBuilder;

	@Before
	public void before() {
		TestEnvSupport.initEnvUtil(TestEnvSupport.loadTestEnvironment());
		auditBuilder = new AuditRequestBuilder();
	}

	@Test
	public void testBuildRequest() {
		RequestWrapper<AuditRequestDTO> actualRequest = auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,
				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",IdType.ID, "desc");
		actualRequest.getRequest().setActionTimeStamp(null);
		AuditRequestDTO expectedRequest = new AuditRequestDTO();
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();

			expectedRequest.setEventId(AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE.getEventId());
			expectedRequest.setEventName(AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE.getEventName());
			expectedRequest.setEventType(AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE.getEventType());
			expectedRequest.setActionTimeStamp(null);
			expectedRequest.setHostName(inetAddress.getHostName());
			expectedRequest.setHostIp(inetAddress.getHostAddress());
			expectedRequest.setApplicationId(EnvUtil.getAppId());
			expectedRequest.setApplicationName(EnvUtil.getAppName());
			expectedRequest.setSessionUserId("sessionUserId");
			expectedRequest.setSessionUserName("sessionUserName");
			expectedRequest.setId("id");
			expectedRequest.setIdType(IdType.ID.getIdType());
			expectedRequest.setCreatedBy(IdRepoSecurityManager.getUser());
			expectedRequest.setModuleName(AuditModules.ID_REPO_CORE_SERVICE.getModuleName());
			expectedRequest.setModuleId(AuditModules.ID_REPO_CORE_SERVICE.getModuleId());
			expectedRequest.setDescription("desc");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		assertEquals(expectedRequest, actualRequest.getRequest());
	}

	@Test
	public void testBuildRequestWithNullIdType() {
		RequestWrapper<AuditRequestDTO> actualRequest = auditBuilder.buildRequest(AuditModules.ID_REPO_CORE_SERVICE,
				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id", null, "desc");
		assertEquals(null, actualRequest.getRequest().getIdType());
	}

	@Test
	public void testResolveHostDetailsUsesEnvHostName() throws Exception {
		InetAddress inetAddress = Mockito.mock(InetAddress.class);
		Mockito.when(inetAddress.getHostAddress()).thenReturn("127.0.0.1");
		try (MockedStatic<InetAddress> inetStatic = Mockito.mockStatic(InetAddress.class)) {
			inetStatic.when(InetAddress::getLocalHost).thenReturn(inetAddress);
			AuditRequestBuilder.HostDetails details = AuditRequestBuilder.resolveHostDetails("env-host");
			assertEquals("env-host", details.hostName());
			assertEquals("127.0.0.1", details.hostAddress());
		}
	}

	@Test
	public void testResolveHostDetailsUsesInetAddressWhenEnvHostBlank() throws Exception {
		InetAddress inetAddress = Mockito.mock(InetAddress.class);
		Mockito.when(inetAddress.getHostName()).thenReturn("local-host");
		Mockito.when(inetAddress.getHostAddress()).thenReturn("10.0.0.1");
		try (MockedStatic<InetAddress> inetStatic = Mockito.mockStatic(InetAddress.class)) {
			inetStatic.when(InetAddress::getLocalHost).thenReturn(inetAddress);
			AuditRequestBuilder.HostDetails details = AuditRequestBuilder.resolveHostDetails(" ");
			assertEquals("local-host", details.hostName());
			assertEquals("10.0.0.1", details.hostAddress());
		}
	}

	@Test
	public void testResolveHostDetailsHandlesUnknownHostException() throws Exception {
		try (MockedStatic<InetAddress> inetStatic = Mockito.mockStatic(InetAddress.class)) {
			inetStatic.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException("unresolvable"));
			AuditRequestBuilder.HostDetails details = AuditRequestBuilder.resolveHostDetails(null);
			assertEquals("", details.hostName());
			assertEquals("", details.hostAddress());
		}
	}

}