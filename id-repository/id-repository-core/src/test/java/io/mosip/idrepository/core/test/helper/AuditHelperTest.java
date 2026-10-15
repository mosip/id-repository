package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.IntFunction;

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
		when(securityManager.getIdHashWithSaltModuloByPlainIdHash(Mockito.any(), Mockito.any()))
				.thenReturn("salted-hash");
	}

	@Test
	public void testAuditSendsSaltedHashFromUinHashSalt() {
		String uin = "1234567890";
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE,
				AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, uin, IdType.UIN, "desc");

		ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
		verify(auditBuilder).buildRequest(Mockito.any(), Mockito.any(), idCaptor.capture(),
				Mockito.any(), Mockito.any());
		assertEquals("salted-hash", idCaptor.getValue());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<IntFunction<String>> saltFn = ArgumentCaptor.forClass(IntFunction.class);
		verify(securityManager).getIdHashWithSaltModuloByPlainIdHash(Mockito.eq(uin), saltFn.capture());
		when(uinHashSaltRepo.retrieveSaltById(7)).thenReturn("salt-7");
		assertEquals("salt-7", saltFn.getValue().apply(7));
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

}
