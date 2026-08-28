package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.junit.Before;
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

import io.mosip.commons.khazana.dto.ObjectStoreReference;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;


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
	public void testBiometricObjectExists()  {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		boolean exists = helper.biometricObjectExists("", "");
		assertTrue(exists);
	}

    @Test
    public void testPutDemographicObject() throws IdRepoAppException {
        byte[] data = "sampleData".getBytes(); // ✅ non-empty byte array

        when(securityManager.encrypt(any(), any())).thenReturn("encryptedData".getBytes());
        when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);

        helper.putDemographicObject("hash", "refId", data);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(adapter).putObject(any(), any(), any(), any(), argCaptor.capture(), any());

        assertEquals("hash/Demographics/refId", argCaptor.getValue());
    }

    @Test
    public void testPutBiometricObject() throws IdRepoAppException {
        // ✅ Non-empty data to avoid "Input data is null or empty"
        byte[] biometricData = "sampleBiometricData".getBytes();

        // ✅ Mock encryption to simulate encrypted output
        when(securityManager.encrypt(any(), any())).thenReturn("encryptedBiometricData".getBytes());

        // ✅ Mock successful object store operation
        when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);

        // ✅ Execute helper call
        helper.putBiometricObject("hash", "refId", biometricData);

        // ✅ Capture and verify correct object store key
        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(adapter).putObject(any(), any(), any(), any(), argCaptor.capture(), any());

        assertEquals("hash/Biometrics/refId", argCaptor.getValue());
    }

    @Test
    public void testPutObjectException() throws IdRepoAppException {
        when(securityManager.encrypt(any(), any())).thenReturn("encrypted".getBytes());
        when(adapter.putObject(any(), any(), any(), any(), any(), any()))
                .thenThrow(new FSAdapterException("FS_ERROR", "File storage access error"));

        FSAdapterException thrown = assertThrows(FSAdapterException.class, () -> {
            helper.putDemographicObject("hash", "refId", "validData".getBytes());
        });

        assertEquals("FS_ERROR", thrown.getErrorCode());
        assertEquals("File storage access error", thrown.getErrorText());
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
	public void testGetDemographicObjectNotFound() {
		when(adapter.getObject(any(), any(), any(), any(), any())).thenReturn(null);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.getDemographicObject("hash", "refId"));
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), thrown.getErrorText());
	}

	@Test
	public void testGetBiometricObjectNotFound() {
		when(adapter.getObject(any(), any(), any(), any(), any())).thenReturn(null);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.getBiometricObject("hash", "refId"));
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), thrown.getErrorText());
	}

    @Test
    public void testGetObjectException() throws IdRepoAppException {
        when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
        when(securityManager.encrypt(any(), any())).thenReturn("encrypted".getBytes());
        when(adapter.getObject(any(), any(), any(), any(), any()))
                .thenThrow(new FSAdapterException("FS_ERROR", "File storage access error"));

        FSAdapterException thrown = assertThrows(FSAdapterException.class, () -> {
            helper.getDemographicObject("hash", "refId");
        });

        assertEquals("FS_ERROR", thrown.getErrorCode());
        assertEquals("File storage access error", thrown.getErrorText());
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

	// ── getRidHash ───────────────────────────────────────────────────────────

	@Test
	public void testGetRidHash() throws Exception {
		String hash = helper.getRidHash("1234567890");
		assertNotNull(hash);
		assertTrue(hash.length() > 0);
	}

	// ── draft biometric / demographic exists ─────────────────────────────────

	@Test
	public void testDraftBiometricObjectExists_True() {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		assertTrue(helper.draftBiometricObjectExists("ridHash", "fileRefId"));
	}

	@Test
	public void testDraftBiometricObjectExists_False() {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		assertFalse(helper.draftBiometricObjectExists("ridHash", "fileRefId"));
	}

	@Test
	public void testDraftDemographicObjectExists_True() {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		assertTrue(helper.draftDemographicObjectExists("ridHash", "fileRefId"));
	}

	@Test
	public void testDraftDemographicObjectExists_False() {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		assertFalse(helper.draftDemographicObjectExists("ridHash", "fileRefId"));
	}

	// ── putDraftBiometricObject / putDraftDemographicObject ──────────────────

	@Test
	public void testPutDraftBiometricObject() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("enc".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);

		helper.putDraftBiometricObject("ridHash", "fileRefId", "data".getBytes());

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(adapter).putObject(any(), any(), any(), any(), captor.capture(), any());
		assertTrue(captor.getValue().contains("_draft"));
		assertTrue(captor.getValue().contains("Biometrics"));
	}

	@Test
	public void testPutDraftDemographicObject() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("enc".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);

		helper.putDraftDemographicObject("ridHash", "fileRefId", "data".getBytes());

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(adapter).putObject(any(), any(), any(), any(), captor.capture(), any());
		assertTrue(captor.getValue().contains("_draft"));
		assertTrue(captor.getValue().contains("Demographics"));
	}

	// ── getDraftBiometricObject / getDraftDemographicObject ──────────────────

	@Test
	public void testGetDraftBiometricObject() throws IdRepoAppException {
		when(adapter.getObject(any(), any(), any(), any(), any()))
				.thenReturn(new ByteArrayInputStream("enc".getBytes()));
		when(securityManager.decrypt(any(), any())).thenReturn("dec".getBytes());

		byte[] result = helper.getDraftBiometricObject("ridHash", "fileRefId");
		assertEquals("dec", new String(result));
	}

	@Test
	public void testGetDraftBiometricObject_NotFound() {
		when(adapter.getObject(any(), any(), any(), any(), any())).thenReturn(null);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.getDraftBiometricObject("ridHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void testGetDraftDemographicObject() throws IdRepoAppException {
		when(adapter.getObject(any(), any(), any(), any(), any()))
				.thenReturn(new ByteArrayInputStream("enc".getBytes()));
		when(securityManager.decrypt(any(), any())).thenReturn("dec".getBytes());

		byte[] result = helper.getDraftDemographicObject("ridHash", "fileRefId");
		assertEquals("dec", new String(result));
	}

	@Test
	public void testGetDraftDemographicObject_NotFound() {
		when(adapter.getObject(any(), any(), any(), any(), any())).thenReturn(null);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.getDraftDemographicObject("ridHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	// ── deleteDraftBiometricObject / deleteDraftDemographicObject ────────────

	@Test
	public void testDeleteDraftBiometricObject_Success() {
		when(adapter.deleteObject(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.deleteDraftBiometricObject("ridHash", "fileRefId");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(adapter).deleteObject(any(), any(), any(), any(), captor.capture());
		assertTrue(captor.getValue().contains("_draft"));
		assertTrue(captor.getValue().contains("Biometrics"));
	}

	@Test
	public void testDeleteDraftBiometricObject_NotFound_SkipsSilently() {
		when(adapter.deleteObject(any(), any(), any(), any(), any()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "not found"));
		helper.deleteDraftBiometricObject("ridHash", "fileRefId");
		verify(adapter).deleteObject(any(), any(), any(), any(), any());
	}

	@Test
	public void testDeleteDraftDemographicObject_Success() {
		when(adapter.deleteObject(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.deleteDraftDemographicObject("ridHash", "fileRefId");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(adapter).deleteObject(any(), any(), any(), any(), captor.capture());
		assertTrue(captor.getValue().contains("_draft"));
		assertTrue(captor.getValue().contains("Demographics"));
	}

	@Test
	public void testDeleteDraftDemographicObject_NotFound_SkipsSilently() {
		when(adapter.deleteObject(any(), any(), any(), any(), any()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "not found"));
		helper.deleteDraftDemographicObject("ridHash", "fileRefId");
		verify(adapter).deleteObject(any(), any(), any(), any(), any());
	}

	// ── moveBiometricDraftToLive / moveDemographicDraftToLive

	@Test
	public void testMoveBiometricDraftToLive_Success() throws IdRepoAppException {
		when(adapter.moveObject(any(), any(), anyBoolean())).thenReturn(Boolean.TRUE);
		helper.moveBiometricDraftToLive("srcHash", "destHash", "fileRefId");
		ArgumentCaptor<ObjectStoreReference> srcCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		ArgumentCaptor<ObjectStoreReference> dstCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		verify(adapter).moveObject(srcCaptor.capture(), dstCaptor.capture(), eq(true));
		assertEquals("_draft/srcHash/Biometrics/fileRefId", srcCaptor.getValue().getObjectName());
		assertEquals("destHash/Biometrics/fileRefId", dstCaptor.getValue().getObjectName());
	}

	@Test
	public void testMoveBiometricDraftToLive_ReturnsFalse_SkipsSilently() throws IdRepoAppException {
		when(adapter.moveObject(any(), any(), anyBoolean())).thenReturn(Boolean.FALSE);
		helper.moveBiometricDraftToLive("srcHash", "destHash", "fileRefId");
		verify(adapter).moveObject(any(), any(), eq(true));
	}

	@Test
	public void testMoveBiometricDraftToLive_Exception() {
		when(adapter.moveObject(any(), any(), anyBoolean()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "move failed"));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.moveBiometricDraftToLive("srcHash", "destHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	// A retried publish can find the source already gone because an earlier attempt already
	// moved it — S3Adapter.moveObject throws NoSuchKey (404) for this instead of returning
	// false, and that specific case must be swallowed rather than failing the retry.
	@Test
	public void testMoveBiometricDraftToLive_MissingSourceKey_SkipsSilently() throws IdRepoAppException {
		S3Exception noSuchKey = (S3Exception) S3Exception.builder()
				.statusCode(404)
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchKey").build())
				.build();
		when(adapter.moveObject(any(), any(), anyBoolean()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "not found", noSuchKey));
		helper.moveBiometricDraftToLive("srcHash", "destHash", "fileRefId");
		verify(adapter).moveObject(any(), any(), eq(true));
	}

	@Test
	public void testMoveBiometricDraftToLive_OtherS3Error_StillThrows() {
		S3Exception accessDenied = (S3Exception) S3Exception.builder()
				.statusCode(403)
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
				.build();
		when(adapter.moveObject(any(), any(), anyBoolean()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "access denied", accessDenied));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.moveBiometricDraftToLive("srcHash", "destHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void testMoveDemographicDraftToLive_Success() throws IdRepoAppException {
		when(adapter.moveObject(any(), any(), anyBoolean())).thenReturn(Boolean.TRUE);
		helper.moveDemographicDraftToLive("srcHash", "destHash", "fileRefId");
		ArgumentCaptor<ObjectStoreReference> srcCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		ArgumentCaptor<ObjectStoreReference> dstCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		verify(adapter).moveObject(srcCaptor.capture(), dstCaptor.capture(), eq(true));
		assertEquals("_draft/srcHash/Demographics/fileRefId", srcCaptor.getValue().getObjectName());
		assertEquals("destHash/Demographics/fileRefId", dstCaptor.getValue().getObjectName());
	}

	@Test
	public void testMoveDemographicDraftToLive_Exception() {
		when(adapter.moveObject(any(), any(), anyBoolean()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "move failed"));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.moveDemographicDraftToLive("srcHash", "destHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	// ── copyBiometricLiveToDraft / copyDemographicLiveToDraft ────────────────

	@Test
	public void testCopyBiometricLiveToDraft_Success() throws IdRepoAppException {
		when(adapter.moveObject(any(), any(), anyBoolean())).thenReturn(Boolean.TRUE);
		helper.copyBiometricLiveToDraft("liveHash", "ridHash", "fileRefId");
		ArgumentCaptor<ObjectStoreReference> srcCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		ArgumentCaptor<ObjectStoreReference> dstCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		verify(adapter).moveObject(srcCaptor.capture(), dstCaptor.capture(), eq(false));
		assertEquals("liveHash/Biometrics/fileRefId", srcCaptor.getValue().getObjectName());
		assertEquals("_draft/ridHash/Biometrics/fileRefId", dstCaptor.getValue().getObjectName());
	}

	@Test
	public void testCopyBiometricLiveToDraft_Exception() {
		when(adapter.moveObject(any(), any(), anyBoolean()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "copy failed"));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.copyBiometricLiveToDraft("liveHash", "ridHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void testCopyDemographicLiveToDraft_Success() throws IdRepoAppException {
		when(adapter.moveObject(any(), any(), anyBoolean())).thenReturn(Boolean.TRUE);
		helper.copyDemographicLiveToDraft("liveHash", "ridHash", "fileRefId");
		ArgumentCaptor<ObjectStoreReference> srcCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		ArgumentCaptor<ObjectStoreReference> dstCaptor = ArgumentCaptor.forClass(ObjectStoreReference.class);
		verify(adapter).moveObject(srcCaptor.capture(), dstCaptor.capture(), eq(false));
		assertEquals("liveHash/Demographics/fileRefId", srcCaptor.getValue().getObjectName());
		assertEquals("_draft/ridHash/Demographics/fileRefId", dstCaptor.getValue().getObjectName());
	}

	@Test
	public void testCopyDemographicLiveToDraft_Exception() {
		when(adapter.moveObject(any(), any(), anyBoolean()))
				.thenThrow(new ObjectStoreAdapterException("ERR", "copy failed"));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				helper.copyDemographicLiveToDraft("liveHash", "ridHash", "fileRefId"));
		assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

}
