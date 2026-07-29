package io.mosip.idrepository.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.credential.store.constant.CredentialConstants;
import io.mosip.idrepository.credential.store.exception.IdRepoException;
import io.mosip.idrepository.identity.service.impl.IdRepoProxyServiceImpl;
import io.mosip.idrepository.identity.validator.IdRequestValidator;

/**
 * Unit tests for {@link InProcessIdentityClient}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class InProcessIdentityClientTest {

	private static final String UIN = "1234567890123456";
	private static final String VID = "12345678901234567890";
	private static final String RID = "10001100770000120200101000001";

	@InjectMocks
	private InProcessIdentityClient client;

	@Mock
	private IdRepoProxyServiceImpl idRepoProxyService;

	@Mock
	private IdRequestValidator idRequestValidator;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(client, "identityType", IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT);
	}

	@After
	public void tearDown() {
		CredentialPipelineContext.clear();
	}

	@Test
	public void retrieveIdentityReusesPipelineCacheForSameFormats() throws Exception {
		CredentialPipelineContext.set(UIN, "enc", "CREATE");
		CredentialServiceRequestDto request = baseRequest(UIN);
		when(idRequestValidator.validateUin(UIN)).thenReturn(true);
		Map<String, String> bioFormats = Map.of(CredentialConstants.FINGER, "ISO19794");
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(any(), any(), any(), any())).thenReturn(expected);

		assertSame(expected, client.retrieveIdentity(request, bioFormats));
		assertSame(expected, client.retrieveIdentity(request, bioFormats));
		verify(idRepoProxyService, times(1)).retrieveIdentity(any(), any(), any(), any());
	}

	@Test
	public void retrieveIdentityByUinDelegatesToProxyService() throws Exception {
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(UIN, IdType.UIN, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap())).thenReturn(expected);

		IdResponseDTO actual = client.retrieveIdentityByUin(UIN);

		assertSame(expected, actual);
		verify(idRepoProxyService).retrieveIdentity(UIN, IdType.UIN, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap());
	}

	@Test
	public void retrieveIdentityWhenAdditionalDataPresentWithoutIdTypeInfersFromValidator() throws Exception {
		CredentialServiceRequestDto request = baseRequest(UIN);
		Map<String, Object> additionalData = new HashMap<>();
		additionalData.put("partner", "p1");
		request.setAdditionalData(additionalData);
		when(idRequestValidator.validateUin(UIN)).thenReturn(true);
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(eq(UIN), eq(IdType.UIN), any(), any())).thenReturn(expected);

		IdResponseDTO actual = client.retrieveIdentity(request, null);

		assertSame(expected, actual);
		verify(idRepoProxyService).retrieveIdentity(UIN, IdType.UIN, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap());
	}

	@Test
	public void retrieveIdentityWithExplicitIdTypeInAdditionalData() throws Exception {
		CredentialServiceRequestDto request = baseRequest(UIN);
		Map<String, Object> additionalData = new HashMap<>();
		additionalData.put("idType", "vid");
		request.setAdditionalData(additionalData);
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(eq(UIN), eq(IdType.VID), any(), any())).thenReturn(expected);

		IdResponseDTO actual = client.retrieveIdentity(request, null);

		assertSame(expected, actual);
		verify(idRepoProxyService).retrieveIdentity(UIN, IdType.VID, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap());
	}

	@Test
	public void retrieveIdentityInfersUinWhenValidatorAcceptsUin() throws Exception {
		CredentialServiceRequestDto request = baseRequest(UIN);
		when(idRequestValidator.validateUin(UIN)).thenReturn(true);
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(eq(UIN), eq(IdType.UIN), any(), any())).thenReturn(expected);

		IdResponseDTO actual = client.retrieveIdentity(request, Collections.emptyMap());

		assertSame(expected, actual);
		verify(idRepoProxyService).retrieveIdentity(UIN, IdType.UIN, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap());
	}

	@Test
	public void retrieveIdentityInfersVidWhenValidatorAcceptsVid() throws Exception {
		CredentialServiceRequestDto request = baseRequest(VID);
		when(idRequestValidator.validateUin(VID)).thenReturn(false);
		when(idRequestValidator.validateVid(VID)).thenReturn(true);
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(eq(VID), eq(IdType.VID), any(), any())).thenReturn(expected);

		IdResponseDTO actual = client.retrieveIdentity(request, Collections.emptyMap());

		assertSame(expected, actual);
		verify(idRepoProxyService).retrieveIdentity(VID, IdType.VID, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap());
	}

	@Test
	public void retrieveIdentityDefaultsToIdWhenNoValidatorMatches() throws Exception {
		CredentialServiceRequestDto request = baseRequest(RID);
		when(idRequestValidator.validateUin(RID)).thenReturn(false);
		when(idRequestValidator.validateVid(RID)).thenReturn(false);
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(eq(RID), eq(IdType.ID), any(), any())).thenReturn(expected);

		IdResponseDTO actual = client.retrieveIdentity(request, Collections.emptyMap());

		assertSame(expected, actual);
		verify(idRepoProxyService).retrieveIdentity(RID, IdType.ID, IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT,
				Collections.emptyMap());
	}

	@Test
	public void retrieveIdentityMapsBioAttributeFormatters() throws Exception {
		CredentialServiceRequestDto request = baseRequest(UIN);
		when(idRequestValidator.validateUin(UIN)).thenReturn(true);
		Map<String, String> bioFormats = new HashMap<>();
		bioFormats.put(CredentialConstants.FINGER, "finger-format");
		bioFormats.put(CredentialConstants.FACE, "");
		bioFormats.put(CredentialConstants.IRIS, "iris-format");
		IdResponseDTO expected = new IdResponseDTO();
		when(idRepoProxyService.retrieveIdentity(any(), any(), any(), any())).thenReturn(expected);

		client.retrieveIdentity(request, bioFormats);

		ArgumentCaptor<Map<String, String>> formatsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(idRepoProxyService).retrieveIdentity(eq(UIN), eq(IdType.UIN), eq(IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT),
				formatsCaptor.capture());
		Map<String, String> extractionFormats = formatsCaptor.getValue();
		assertEquals(2, extractionFormats.size());
		assertEquals("finger-format", extractionFormats.get(CredentialConstants.FINGER));
		assertEquals("iris-format", extractionFormats.get(CredentialConstants.IRIS));
		assertTrue(!extractionFormats.containsKey(CredentialConstants.FACE));
	}

	@Test(expected = IdRepoException.class)
	public void retrieveIdentityWrapsIdRepoAppException() throws Exception {
		CredentialServiceRequestDto request = baseRequest(UIN);
		when(idRequestValidator.validateUin(UIN)).thenReturn(true);
		when(idRepoProxyService.retrieveIdentity(any(), any(), any(), any()))
				.thenThrow(new IdRepoAppException("ERR", "identity missing"));

		client.retrieveIdentity(request, null);
	}

	@Test(expected = IdRepoException.class)
	public void retrieveIdentityWrapsGenericException() throws Exception {
		CredentialServiceRequestDto request = baseRequest(UIN);
		when(idRequestValidator.validateUin(UIN)).thenReturn(true);
		when(idRepoProxyService.retrieveIdentity(any(), any(), any(), any()))
				.thenThrow(new RuntimeException("unexpected"));

		client.retrieveIdentity(request, null);
	}

	private static CredentialServiceRequestDto baseRequest(String id) {
		CredentialServiceRequestDto request = new CredentialServiceRequestDto();
		request.setId(id);
		return request;
	}
}
