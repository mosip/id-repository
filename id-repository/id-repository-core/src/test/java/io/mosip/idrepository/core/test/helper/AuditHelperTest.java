package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;

/**
 * @author Manoj SP
 *
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
public class AuditHelperTest {

	@Mock
	RestHelper restHelper;

	@InjectMocks
	AuditHelper auditHelper;

	@Mock
	IdRepoSecurityManager securityManager;

	@Mock
	UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	MockMvc mockMvc;

	@Mock
	AuditRequestBuilder auditBuilder;

	@Mock
	RestRequestBuilder restBuilder;

	@Autowired
	Environment env;

	@Before
	public void before() {
		ReflectionTestUtils.setField(restBuilder, "env", env);
		ReflectionTestUtils.setField(auditHelper, "mapper", new ObjectMapper());
		when(securityManager.getIdHashWithSaltModuloByPlainIdHash(
				Mockito.anyString(), Mockito.any())).thenReturn("salted-hash");
	}

	@Test
	public void testAuditAndAuditAsyncUseSaltedHash() throws IdRepoDataValidationException {
		String uin = "1234567890";
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(mock(RestRequestDTO.class));
		when(restHelper.requestAsync(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(null));

		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE,
				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, uin, IdType.UIN, "desc");
		auditHelper.auditAsync(AuditModules.ID_REPO_CORE_SERVICE,
				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, uin, IdType.UIN, "desc");

		ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
		verify(auditBuilder, Mockito.times(2)).buildRequest(
				Mockito.any(), Mockito.any(), idCaptor.capture(), Mockito.any(), Mockito.any());
		assertEquals(Arrays.asList("salted-hash", "salted-hash"), idCaptor.getAllValues());
		verify(securityManager, Mockito.times(2))
				.getIdHashWithSaltModuloByPlainIdHash(Mockito.eq(uin), Mockito.any());
		verify(securityManager, never()).hash(Mockito.any());
	}

	@Test
	public void testAudit() throws IdRepoDataValidationException {
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",
				IdType.ID, "desc");
	}

	@Test
	public void testAuditFailure() throws IdRepoDataValidationException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new IdRepoDataValidationException());
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",
				IdType.ID, "desc");
	}

	@Test
	public void testAuditError() throws IdRepoDataValidationException {
		auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",
				IdType.ID, new IdRepoAppException(IdRepoErrorConstants.AUTHORIZATION_FAILED));
	}

	@SuppressWarnings("serial")
	@Test
	public void testAuditErrorFailure() throws IdRepoDataValidationException, JsonProcessingException {
		ObjectMapper mapperMock = mock(ObjectMapper.class);
		when(mapperMock.writeValueAsString(Mockito.any())).thenThrow(new JsonProcessingException("") {
		});
		ReflectionTestUtils.setField(auditHelper, "mapper", mapperMock);
		auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",
				IdType.ID, new IdRepoAppException(IdRepoErrorConstants.AUTHORIZATION_FAILED));
		ReflectionTestUtils.setField(auditHelper, "mapper", new ObjectMapper());
	}

	@Test
	public void testAuditShouldThrowGenericException() throws IdRepoDataValidationException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new RuntimeException());
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, "id",
				IdType.ID, "desc");
	}

}
