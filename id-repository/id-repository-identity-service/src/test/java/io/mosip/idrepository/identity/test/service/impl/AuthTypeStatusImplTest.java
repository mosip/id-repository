package io.mosip.idrepository.identity.test.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.entity.AuthtypeLock;
import io.mosip.idrepository.identity.repository.AuthLockRepository;
import io.mosip.idrepository.identity.service.impl.AuthTypeStatusImpl;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@ActiveProfiles("test")
public class AuthTypeStatusImplTest {

	@InjectMocks
	private AuthTypeStatusImpl authTypeStatusImpl;

	@Mock
	AuthLockRepository authLockRepository;

	@Mock
	private IdRepoSecurityManager securityManager;

	/** The environment. */
	@Autowired
	private Environment env;

	/** The rest helper. */
	@Mock
	private RestHelper restHelper;

	/** The rest builder. */
	@Mock
	private RestRequestBuilder restBuilder;

	@Mock
	private IdRepoWebSubHelper webSubHelper;

	@Autowired
	private ObjectMapper mapper;

	private Object partnerResponseObj;

	@Before
	public void init() throws IOException {
		partnerResponseObj = mapper.readValue(
				"{\"response\":{\"partners\":[{\"partnerID\":\"MOVP\",\"status\":\"active\",\"organizationName\":\"movp\",\"contactNumber\":\"\",\"emailId\":\"movp@gmail.com\",\"address\":\"Bangalore\",\"partnerType\":\"Online_Verification_Partner\"}]}}",
				Object.class);
		ReflectionTestUtils.setField(authTypeStatusImpl, "env", env);
	}

	@Test
	public void testFetchAuthTypeStatusOfUINWithBioLock() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		Object[] obj = new Object[3];
		obj[0] = "demo";
		obj[1] = "true";
		obj[2] = null;
		when(authLockRepository.findByUinHash(any())).thenReturn(Collections.singletonList(obj));
		List<AuthtypeStatus> response = authTypeStatusImpl.fetchAuthTypeStatus("", IdType.UIN);
		assertEquals("demo", response.get(0).getAuthType());
		assertNull(response.get(0).getAuthSubType());
		assertEquals(true, response.get(0).getLocked());
		assertNull(response.get(0).getUnlockForSeconds());
	}

	@Test
	public void testFetchAuthTypeStatusOfVIDWithDemoLock() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		Object[] obj = new Object[3];
		obj[0] = "bio-FACE";
		obj[1] = "true";
		obj[2] = null;
		when(authLockRepository.findByUinHash(any())).thenReturn(Collections.singletonList(obj));
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		restRequestDTO.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequestDTO);
		ResponseWrapper<Map<String, String>> restResponse = new ResponseWrapper<>();
		restResponse.setResponse(Map.of("UIN", ""));
		when(restHelper.requestSync(any())).thenReturn(restResponse);
		List<AuthtypeStatus> response = authTypeStatusImpl.fetchAuthTypeStatus("", IdType.VID);
		assertEquals("bio", response.get(0).getAuthType());
		assertEquals("FACE", response.get(0).getAuthSubType());
		assertEquals(true, response.get(0).getLocked());
		assertNull(response.get(0).getUnlockForSeconds());
	}

	@Test
	public void testFetchAuthTypeStatusOfVIDWithDemoLockGetUinFailed() throws IdRepoAppException, IOException {
		when(securityManager.hash(any())).thenReturn("");
		Object[] obj = new Object[3];
		obj[0] = "bio-FACE";
		obj[1] = "true";
		obj[2] = null;
		when(authLockRepository.findByUinHash(any())).thenReturn(Collections.singletonList(obj));
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		restRequestDTO.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequestDTO);
		ResponseWrapper<Map<String, String>> restResponse = new ResponseWrapper<>();
		restResponse.setErrors(List.of(new ServiceError(IdRepoErrorConstants.AUTHENTICATION_FAILED.getErrorCode(),
				IdRepoErrorConstants.AUTHENTICATION_FAILED.getErrorMessage())));
		when(restHelper.requestSync(any()))
				.thenThrow(new RestServiceException(IdRepoErrorConstants.AUTHENTICATION_FAILED,
						new ObjectMapper().writeValueAsString(restResponse), restResponse));
		try {
			authTypeStatusImpl.fetchAuthTypeStatus("", IdType.VID);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.AUTHENTICATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.AUTHENTICATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testFetchAuthTypeStatusOfVIDWithDemoLockGetUinFailedUnknownError() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		Object[] obj = new Object[3];
		obj[0] = "bio-FACE";
		obj[1] = "true";
		obj[2] = null;
		when(authLockRepository.findByUinHash(any())).thenReturn(Collections.singletonList(obj));
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		restRequestDTO.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequestDTO);
		ResponseWrapper<Map<String, String>> restResponse = new ResponseWrapper<>();
		restResponse.setResponse(Map.of("UIN", ""));
		when(restHelper.requestSync(any())).thenThrow(new RestServiceException());
		try {
			authTypeStatusImpl.fetchAuthTypeStatus("", IdType.VID);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testFetchAuthTypeStatusOfUINWithBioLockUnlockedTemporarily() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		Object[] obj = new Object[3];
		obj[0] = "bio-FACE";
		obj[1] = "true";
		obj[2] = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime().plusMinutes(1));
		when(authLockRepository.findByUinHash(any())).thenReturn(Collections.singletonList(obj));
		List<AuthtypeStatus> response = authTypeStatusImpl.fetchAuthTypeStatus("", IdType.UIN);
		assertEquals("bio", response.get(0).getAuthType());
		assertEquals("FACE", response.get(0).getAuthSubType());
		assertEquals(false, response.get(0).getLocked());
		assertThat("unlockForSeconds", response.get(0).getUnlockForSeconds(), Matchers.lessThanOrEqualTo(60l));
	}

	@Test
	public void testFetchAuthTypeStatusOfUINWithBioLockUnlockedTemporarilyAndUnlockExpired()
			throws IdRepoAppException, InterruptedException {
		when(securityManager.hash(any())).thenReturn("");
		Object[] obj = new Object[3];
		obj[0] = "bio-FACE";
		obj[1] = "true";
		obj[2] = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime().plusSeconds(3));
		when(authLockRepository.findByUinHash(any())).thenReturn(Collections.singletonList(obj));
		List<AuthtypeStatus> response = authTypeStatusImpl.fetchAuthTypeStatus("", IdType.UIN);
		assertEquals("bio", response.get(0).getAuthType());
		assertEquals("FACE", response.get(0).getAuthSubType());
		assertEquals(false, response.get(0).getLocked());
		assertTrue(response.get(0).getUnlockForSeconds() <= 3l);
		CountDownLatch waiter = new CountDownLatch(1);
		waiter.await(3, TimeUnit.SECONDS);
		response = authTypeStatusImpl.fetchAuthTypeStatus("", IdType.UIN);
		assertEquals("bio", response.get(0).getAuthType());
		assertEquals("FACE", response.get(0).getAuthSubType());
		assertEquals(true, response.get(0).getLocked());
		assertNull(response.get(0).getUnlockForSeconds());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateAuthTypeStatusLock() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(any())).thenReturn(partnerResponseObj);
		AuthtypeStatus authTypeStatus = new AuthtypeStatus();
		authTypeStatus.setAuthType("bio");
		authTypeStatus.setAuthSubType("FACE");
		authTypeStatus.setLocked(true);
		List<AuthtypeStatus> authTypeStatusList = List.of(authTypeStatus);
		authTypeStatusImpl.updateAuthTypeStatus("", IdType.UIN, authTypeStatusList);
		ArgumentCaptor<List<AuthtypeLock>> responseCapture = ArgumentCaptor.forClass(List.class);
		verify(authLockRepository).saveAll(responseCapture.capture());
		List<AuthtypeLock> responseValueList = responseCapture.getValue();
		AuthtypeLock responseValue = responseValueList.get(0);
		assertEquals("", responseValue.getHashedUin());
		assertEquals("bio-FACE", responseValue.getAuthtypecode());
		assertNotNull(responseValue.getLockrequestDTtimes());
		assertNotNull(responseValue.getLockstartDTtimes());
		assertNull(responseValue.getLockendDTtimes());
		assertEquals("true", responseValue.getStatuscode());
		assertNull(responseValue.getUnlockExpiryDTtimes());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateAuthTypeStatusLTemporaryUnlock() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(any())).thenReturn(partnerResponseObj);
		AuthtypeStatus authTypeStatus = new AuthtypeStatus();
		authTypeStatus.setAuthType("bio");
		authTypeStatus.setAuthSubType("FACE");
		authTypeStatus.setLocked(false);
		authTypeStatus.setUnlockForSeconds(10l);
		List<AuthtypeStatus> authTypeStatusList = List.of(authTypeStatus);
		authTypeStatusImpl.updateAuthTypeStatus("", IdType.UIN, authTypeStatusList);
		ArgumentCaptor<List<AuthtypeLock>> responseCapture = ArgumentCaptor.forClass(List.class);
		verify(authLockRepository).saveAll(responseCapture.capture());
		List<AuthtypeLock> responseValueList = responseCapture.getValue();
		AuthtypeLock responseValue = responseValueList.get(0);
		assertEquals("", responseValue.getHashedUin());
		assertEquals("bio-FACE", responseValue.getAuthtypecode());
		assertNotNull(responseValue.getLockrequestDTtimes());
		assertNotNull(responseValue.getLockstartDTtimes());
		assertNull(responseValue.getLockendDTtimes());
		assertEquals("true", responseValue.getStatuscode());
		assertNotNull(responseValue.getUnlockExpiryDTtimes());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateAuthTypeStatusLockFailedToGetPartner() throws IdRepoAppException {
		when(securityManager.hash(any())).thenReturn("");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(any())).thenThrow(new RestServiceException());
		AuthtypeStatus authTypeStatus = new AuthtypeStatus();
		authTypeStatus.setAuthType("bio");
		authTypeStatus.setAuthSubType("FACE");
		authTypeStatus.setLocked(true);
		List<AuthtypeStatus> authTypeStatusList = List.of(authTypeStatus);
		authTypeStatusImpl.updateAuthTypeStatus("", IdType.UIN, authTypeStatusList);
		ArgumentCaptor<List<AuthtypeLock>> responseCapture = ArgumentCaptor.forClass(List.class);
		verify(authLockRepository).saveAll(responseCapture.capture());
		List<AuthtypeLock> responseValueList = responseCapture.getValue();
		AuthtypeLock responseValue = responseValueList.get(0);
		assertEquals("", responseValue.getHashedUin());
		assertEquals("bio-FACE", responseValue.getAuthtypecode());
		assertNotNull(responseValue.getLockrequestDTtimes());
		assertNotNull(responseValue.getLockstartDTtimes());
		assertNull(responseValue.getLockendDTtimes());
		assertEquals("true", responseValue.getStatuscode());
		assertNull(responseValue.getUnlockExpiryDTtimes());
	}
}
