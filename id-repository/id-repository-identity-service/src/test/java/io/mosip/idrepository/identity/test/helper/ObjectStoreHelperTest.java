package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;

@Ignore
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class ObjectStoreHelperTest {

	@InjectMocks
	private ObjectStoreHelper helper;

	@Mock
	private ObjectStoreAdapter adapter;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Before
	public void init() {
		ReflectionTestUtils.setField(helper, "objectStoreAdapterName", "objectStoreAdapterName");
		ApplicationContext ctxMock = mock(ApplicationContext.class);
		when(ctxMock.getBean("objectStoreAdapterName", ObjectStoreAdapter.class)).thenReturn(adapter);
		helper.setObjectStore(ctxMock);
	}

	@Test
	public void testDemographicObjectExists() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		boolean exists = helper.demographicObjectExists("", "");
		assertTrue(exists);
	}

	@Test
	public void testBiometricObjectExists() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		boolean exists = helper.biometricObjectExists("", "");
		assertTrue(exists);
	}

	@Test
	public void testPutDemographicObject() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.putDemographicObject("hash", "refId", "".getBytes());
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(adapter).putObject(any(), any(), any(), any(), argCaptor.capture(), any());
		assertEquals("hash/Demographics/refId", argCaptor.getValue());
	}

	@Test
	public void testPutBiometricObject() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.putBiometricObject("hash", "refId", "".getBytes());
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(adapter).putObject(any(), any(), any(), any(), argCaptor.capture(), any());
		assertEquals("hash/Biometrics/refId", argCaptor.getValue());
	}

	@Test
	public void testPutObjectException() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenThrow(new FSAdapterException("",""));
		try {
			helper.putDemographicObject("hash", "refId", "".getBytes());
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetDemographicObject() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(securityManager.decrypt(any(), any())).thenReturn("abc".getBytes());
		when(adapter.getObject(any(), any(), any(), any(), any()))
				.thenReturn(new ByteArrayInputStream("abc".getBytes()));
		byte[] demographicObject = helper.getDemographicObject("hash", "refId");
		assertEquals("abc", new String(demographicObject));
	}

	@Test
	public void testGetBiometricObject() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(securityManager.decrypt(any(), any())).thenReturn("abc".getBytes());
		when(adapter.getObject(any(), any(), any(), any(), any()))
				.thenReturn(new ByteArrayInputStream("abc".getBytes()));
		byte[] bioObject = helper.getBiometricObject("hash", "refId");
		assertEquals("abc", new String(bioObject));
	}

	@Test
	public void testGetDemographicObjectNotFound() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		try {
			helper.getDemographicObject("hash", "refId");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetBiometricObjectNotFound() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		try {
			helper.getBiometricObject("hash", "refId");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetObjectException() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.getObject(any(), any(), any(), any(), any())).thenThrow(new FSAdapterException("",""));
		try {
			helper.getDemographicObject("hash", "refId");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testDeleteBiometricObject() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(adapter.deleteObject(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.deleteBiometricObject("hash", "refId");
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(adapter).deleteObject(any(), any(), any(), any(), argCaptor.capture());
		assertEquals("hash/Biometrics/refId", argCaptor.getValue());
	}

	@Test
	public void testDeleteBiometricObjectNotExists() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		helper.deleteBiometricObject("hash", "refId");
	}

}
