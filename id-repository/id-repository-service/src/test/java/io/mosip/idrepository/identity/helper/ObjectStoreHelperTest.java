package io.mosip.idrepository.identity.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

@RunWith(MockitoJUnitRunner.class)
public class ObjectStoreHelperTest {

	@InjectMocks
	private ObjectStoreHelper helper;

	@Mock
	private ObjectStoreAdapter objectStore;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(helper, "bioDataRefId", "biometric_data");
		ReflectionTestUtils.setField(helper, "demoDataRefId", "demographic_data");
		ReflectionTestUtils.setField(helper, "objectStoreAccountName", "idrepo");
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "idrepo");
		ReflectionTestUtils.setField(helper, "objectStore", objectStore);
	}

	@Test
	public void getDemographicObjectSkipsExistsCheck() throws Exception {
		byte[] cipher = "cipher".getBytes();
		byte[] plain = "plain".getBytes();
		when(objectStore.getObject(eq("idrepo"), eq("idrepo"), any(), any(), eq("hash/Demographics/doc1")))
				.thenReturn(new ByteArrayInputStream(cipher));
		when(securityManager.decrypt(cipher, "demographic_data")).thenReturn(plain);

		assertArrayEquals(plain, helper.getDemographicObject("hash", "doc1"));
		verify(objectStore, never()).exists(any(), any(), any(), any(), any());
	}

	@Test
	public void getBiometricObjectMapsMissingKeyToFileNotFound() throws Exception {
		AmazonS3Exception missing = new AmazonS3Exception("Not Found");
		missing.setStatusCode(404);
		missing.setErrorCode("NoSuchKey");
		when(objectStore.getObject(any(), any(), any(), any(), any())).thenThrow(missing);

		try {
			helper.getBiometricObject("hash", "bio1");
			fail("expected FILE_NOT_FOUND");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
		}
	}
}
