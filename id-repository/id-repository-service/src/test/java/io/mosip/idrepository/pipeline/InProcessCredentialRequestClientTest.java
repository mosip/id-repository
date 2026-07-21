package io.mosip.idrepository.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.CredentialIssueRequest;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.credential.request.service.CredentialRequestService;
import io.mosip.kernel.core.http.ResponseWrapper;

/**
 * Unit tests for {@link InProcessCredentialRequestClient}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class InProcessCredentialRequestClientTest {

	private static final String REQUEST_ID = "req-001";

	@InjectMocks
	private InProcessCredentialRequestClient client;

	@Mock
	private CredentialRequestService credentialRequestService;

	@Mock
	private ObjectMapper objectMapper;

	@Test
	public void queueRequestWithoutRequestIdUsesCreateCredentialIssuance() {
		CredentialIssueRequestDto requestDto = buildRequestDto(null);
		ResponseWrapper<CredentialIssueResponse> wrapper = new ResponseWrapper<>();
		Map<String, Object> expectedMap = Map.of("response", "ok");

		when(credentialRequestService.createCredentialIssuance(any(CredentialIssueRequest.class))).thenReturn(wrapper);
		when(objectMapper.convertValue(wrapper, Map.class)).thenReturn(expectedMap);

		Map<String, Object> result = client.queueRequest(requestDto);

		assertEquals(expectedMap, result);
		ArgumentCaptor<CredentialIssueRequest> captor = ArgumentCaptor.forClass(CredentialIssueRequest.class);
		verify(credentialRequestService).createCredentialIssuance(captor.capture());
		assertMappedRequest(requestDto, captor.getValue());
		verify(objectMapper).convertValue(wrapper, Map.class);
	}

	@Test
	public void queueRequestWithRequestIdUsesCreateCredentialIssuanceByRid() {
		CredentialIssueRequestDto requestDto = buildRequestDto(REQUEST_ID);
		ResponseWrapper<CredentialIssueResponse> wrapper = new ResponseWrapper<>();
		Map<String, Object> expectedMap = Map.of("response", "queued");

		when(credentialRequestService.createCredentialIssuanceByRid(any(CredentialIssueRequest.class), eq(REQUEST_ID)))
				.thenReturn(wrapper);
		when(objectMapper.convertValue(wrapper, Map.class)).thenReturn(expectedMap);

		Map<String, Object> result = client.queueRequest(requestDto);

		assertEquals(expectedMap, result);
		ArgumentCaptor<CredentialIssueRequest> captor = ArgumentCaptor.forClass(CredentialIssueRequest.class);
		verify(credentialRequestService).createCredentialIssuanceByRid(captor.capture(), eq(REQUEST_ID));
		assertMappedRequest(requestDto, captor.getValue());
	}

	@Test
	public void queueRequestWithNullAdditionalData() {
		CredentialIssueRequestDto requestDto = new CredentialIssueRequestDto();
		requestDto.setId("1234567890123456");
		requestDto.setCredentialType("type");
		requestDto.setAdditionalData(null);

		ResponseWrapper<CredentialIssueResponse> wrapper = new ResponseWrapper<>();
		when(credentialRequestService.createCredentialIssuance(any(CredentialIssueRequest.class))).thenReturn(wrapper);
		when(objectMapper.convertValue(wrapper, Map.class)).thenReturn(new HashMap<>());

		client.queueRequest(requestDto);

		ArgumentCaptor<CredentialIssueRequest> captor = ArgumentCaptor.forClass(CredentialIssueRequest.class);
		verify(credentialRequestService).createCredentialIssuance(captor.capture());
		assertNotNull(captor.getValue());
	}

	private static CredentialIssueRequestDto buildRequestDto(String requestId) {
		CredentialIssueRequestDto dto = new CredentialIssueRequestDto();
		dto.setId("1234567890123456");
		dto.setCredentialType("type");
		dto.setIssuer("issuer");
		dto.setRecepiant("recipient");
		dto.setUser("user");
		dto.setEncrypt(true);
		dto.setEncryptionKey("key");
		dto.setSharableAttributes(java.util.List.of("name"));
		Map<String, Object> additionalData = new HashMap<>();
		additionalData.put("partner", "p1");
		dto.setAdditionalData(additionalData);
		dto.setRequestId(requestId);
		return dto;
	}

	private static void assertMappedRequest(CredentialIssueRequestDto dto, CredentialIssueRequest request) {
		assertEquals(dto.getId(), request.getId());
		assertEquals(dto.getCredentialType(), request.getCredentialType());
		assertEquals(dto.getIssuer(), request.getIssuer());
		assertEquals(dto.getRecepiant(), request.getRecepiant());
		assertEquals(dto.getUser(), request.getUser());
		assertEquals(dto.isEncrypt(), request.isEncrypt());
		assertEquals(dto.getEncryptionKey(), request.getEncryptionKey());
		assertEquals(dto.getSharableAttributes(), request.getSharableAttributes());
		if (dto.getAdditionalData() != null) {
			assertEquals(dto.getAdditionalData(), request.getAdditionalData());
		}
	}
}
