package io.mosip.idrepository.credentialsfeeder.step.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.credentialsfeeder.entity.AuthtypeLock;
import io.mosip.idrepository.credentialsfeeder.entity.Uin;
import io.mosip.idrepository.credentialsfeeder.repository.AuthLockRepository;
import io.mosip.idrepository.credentialsfeeder.step.CredentialsFeedingWriter;
import io.mosip.kernel.core.util.DateUtils;

/**
 * @author Manoj SP
 *
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
public class CredentialsFeedingWriterTest {

	@InjectMocks
	private CredentialsFeedingWriter writer;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Mock
	private CredentialServiceManager credentialServiceManager;

	@Mock
	private CredentialStatusManager credentialStatusManager;

	@Mock
	private RestRequestBuilder restBuilder;

	@Mock
	private RestHelper restHelper;

	@Mock
	private IdRepoWebSubHelper webSubHelper;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private AuthLockRepository authLockRepo;

	@Before
	public void init() {
		ReflectionTestUtils.setField(writer, "onlineVerificationPartnerIds",
				new String[] { "onlineVerificationPartnerIds" });
		ReflectionTestUtils.setField(writer, "uinActiveStatus", "uinActiveStatus");
		ReflectionTestUtils.setField(writer, "vidActiveStatus", "vidActiveStatus");
	}

	@Test
	public void testIssueCredential() throws Exception {
		VidsInfosDTO info = new VidsInfosDTO();
		info.setResponse(List.of());
		RestRequestDTO requestDTO = new RestRequestDTO();
		requestDTO.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(requestDTO);
		when(restHelper.requestSync(any())).thenReturn(info);
		when(credentialStatusManager.decryptId(any())).thenReturn("1234");
		writer.write(List.of(new Uin(null, "1234", "1234", "".getBytes(), null, null, null, null, null, null, null,
				null, null, null, null)));
	}

	@Test(expected = IdRepoAppUncheckedException.class)
	public void testIssueCredentialDecryptionFailed() throws Exception {
		when(credentialStatusManager.decryptId(any())).thenThrow(new IdRepoAppException("", ""));
		writer.write(List.of(new Uin(null, "1234", "1234", "".getBytes(), null, null, null, null, null, null, null,
				null, null, null, null)));
	}

	@Test(expected = IdRepoAppUncheckedException.class)
	public void testIssueCredentialVidRestCallFailed() throws Exception {
		VidsInfosDTO info = new VidsInfosDTO();
		info.setResponse(List.of());
		RestRequestDTO requestDTO = new RestRequestDTO();
		requestDTO.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(requestDTO);
		when(restHelper.requestSync(any())).thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR));
		when(credentialStatusManager.decryptId(any())).thenReturn("1234");
		writer.write(List.of(new Uin(null, "1234", "1234", "".getBytes(), null, null, null, null, null, null, null,
				null, null, null, null)));
	}
	
	@Test
	public void testPublishAuthLockWithTempUnlock() throws Exception {
		AuthtypeLock authLock = new AuthtypeLock("", "true", DateUtils.getUTCCurrentDateTime());
		when(authLockRepo.findByHashedUin(any())).thenReturn(List.of(authLock));
		VidsInfosDTO info = new VidsInfosDTO();
		info.setResponse(List.of());
		RestRequestDTO requestDTO = new RestRequestDTO();
		requestDTO.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(requestDTO);
		when(restHelper.requestSync(any())).thenReturn(info);
		when(credentialStatusManager.decryptId(any())).thenReturn("1234");
		writer.write(List.of(new Uin(null, "1234", "1234", "".getBytes(), null, null, null, null, null, null, null,
				null, null, null, null)));
	}
	
	@Test
	public void testPublishAuthLock() throws Exception {
		AuthtypeLock authLock = new AuthtypeLock("", "true", null);
		when(authLockRepo.findByHashedUin(any())).thenReturn(List.of(authLock));
		VidsInfosDTO info = new VidsInfosDTO();
		info.setResponse(List.of());
		RestRequestDTO requestDTO = new RestRequestDTO();
		requestDTO.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(requestDTO);
		when(restHelper.requestSync(any())).thenReturn(info);
		when(credentialStatusManager.decryptId(any())).thenReturn("1234");
		writer.write(List.of(new Uin(null, "1234", "1234", "".getBytes(), null, null, null, null, null, null, null,
				null, null, null, null)));
	}
}
