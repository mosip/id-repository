package io.mosip.idrepository.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.credential.store.service.CredentialStoreService;

/**
 * Unit tests for {@link InProcessCredentialClient}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class InProcessCredentialClientTest {

	@InjectMocks
	private InProcessCredentialClient client;

	@Mock
	private CredentialStoreService credentialStoreService;

	@Mock
	private ObjectMapper objectMapper;

	@Test
	public void issueCredentialDelegatesToCredentialStoreService() {
		CredentialServiceRequestDto request = new CredentialServiceRequestDto();
		request.setId("1234567890123456");
		CredentialServiceResponseDto expected = new CredentialServiceResponseDto();
		when(credentialStoreService.createCredentialIssuance(request)).thenReturn(expected);

		CredentialServiceResponseDto actual = client.issueCredential(request);

		assertSame(expected, actual);
		verify(credentialStoreService).createCredentialIssuance(request);
	}

	@Test
	public void issueCredentialAsJsonSerializesResponse() throws Exception {
		CredentialServiceRequestDto request = new CredentialServiceRequestDto();
		request.setId("1234567890123456");
		CredentialServiceResponseDto response = new CredentialServiceResponseDto();
		when(credentialStoreService.createCredentialIssuance(request)).thenReturn(response);
		when(objectMapper.writeValueAsString(response)).thenReturn("{\"response\":{}}");

		String json = client.issueCredentialAsJson(request);

		assertEquals("{\"response\":{}}", json);
		verify(credentialStoreService).createCredentialIssuance(request);
		verify(objectMapper).writeValueAsString(response);
	}

	@Test(expected = JsonProcessingException.class)
	public void issueCredentialAsJsonPropagatesSerializationException() throws Exception {
		CredentialServiceRequestDto request = new CredentialServiceRequestDto();
		CredentialServiceResponseDto response = new CredentialServiceResponseDto();
		when(credentialStoreService.createCredentialIssuance(request)).thenReturn(response);
		when(objectMapper.writeValueAsString(response)).thenThrow(new JsonProcessingException("serialize failed") {
			private static final long serialVersionUID = -9021847365012948573L;
		});

		client.issueCredentialAsJson(request);
	}
}
