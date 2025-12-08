package io.mosip.credential.request.generator.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CredentialIssueRequestHelperTest {

    @InjectMocks
    private CredentialIssueRequestHelper credentialIssueRequestHelper;

    @Mock
    private io.mosip.credential.request.generator.util.CryptoUtil cryptoUtil;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CredentialIssueRequestDto credentialIssueRequestDto;

    @Mock
    private CredentialEntity credentialEntity;

    private String requestId = "testRequestId";

    @Before
    public void setUp() {
        when(credentialIssueRequestDto.getId()).thenReturn("testId");
        when(credentialIssueRequestDto.getCredentialType()).thenReturn("testType");
        when(credentialIssueRequestDto.getIssuer()).thenReturn("testIssuer");
        when(credentialIssueRequestDto.getUser()).thenReturn("testUser");
        when(credentialIssueRequestDto.isEncrypt()).thenReturn(true);
        when(credentialIssueRequestDto.getEncryptionKey()).thenReturn("testKey");
        when(credentialIssueRequestDto.getSharableAttributes()).thenReturn(Collections.singletonList("testAttributes"));
        when(credentialIssueRequestDto.getAdditionalData()).thenReturn(new HashMap<>());
        when(cryptoUtil.decryptData(anyString())).thenReturn(Arrays.toString("decryptedData".getBytes()));
    }

    @Test
    public void testGetCredentialServiceRequestDto() throws JsonProcessingException {

        CredentialServiceRequestDto expected = new CredentialServiceRequestDto();
        expected.setId("testId");
        expected.setCredentialType("testType");
        expected.setRequestId(requestId);
        expected.setIssuer("testIssuer");
        expected.setRecepiant("testIssuer");
        expected.setUser("testUser");
        expected.setEncrypt(true);
        expected.setEncryptionKey("testKey");
        expected.setSharableAttributes(Collections.singletonList("testAttributes"));

        CredentialServiceRequestDto actual = credentialIssueRequestHelper.getCredentialServiceRequestDto(credentialIssueRequestDto, requestId);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getCredentialType(), actual.getCredentialType());
        assertEquals(expected.getRequestId(), actual.getRequestId());
        assertEquals(expected.getIssuer(), actual.getIssuer());
        assertEquals(expected.getRecepiant(), actual.getRecepiant());
        assertEquals(expected.getUser(), actual.getUser());
        assertTrue(actual.isEncrypt());
        assertEquals(expected.getEncryptionKey(), actual.getEncryptionKey());
        assertEquals(expected.getSharableAttributes(), actual.getSharableAttributes());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testGetCredentialIssueRequestDtoInvalidBase64() throws JsonProcessingException {
        String invalidBase64 = "invalidBase64String[!@#";
        when(credentialEntity.getRequest()).thenReturn(invalidBase64);
        credentialIssueRequestHelper.getCredentialIssueRequestDto(credentialEntity);
    }
}
