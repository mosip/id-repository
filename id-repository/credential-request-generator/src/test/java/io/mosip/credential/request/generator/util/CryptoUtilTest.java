package io.mosip.credential.request.generator.util;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorUncheckedException;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
public class CryptoUtilTest {

    @Mock
    private RestUtil restUtil;

    @InjectMocks
    private CryptoUtil cryptoUtil;

    private ResponseWrapper<Map<String, String>> successResponse;
    private ResponseWrapper<Map<String, String>> errorResponse;

    @Before
    public void setup() {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("data", "processed-value");

        successResponse = new ResponseWrapper<>();
        successResponse.setResponse(dataMap);
        successResponse.setErrors(Collections.emptyList());

        ServiceError error = new ServiceError("ERR", "Keymanager error");
        errorResponse = new ResponseWrapper<>();
        errorResponse.setErrors(Collections.singletonList(error));
    }

    @Test
    public void testEncryptDataSuccess() throws Exception {
        when(restUtil.postApi(
                eq(ApiName.ENCRYPTION),
                any(), any(), any(),
                any(),
                any(RequestWrapper.class),
                eq(ResponseWrapper.class)
        )).thenReturn(successResponse);

        String output = cryptoUtil.encryptData("12345");
        assertEquals("processed-value", output);
    }

    @Test
    public void testDecryptDataSuccess() throws Exception {
        when(restUtil.postApi(
                eq(ApiName.DECRYPTION),
                any(), any(), any(),
                any(),
                any(RequestWrapper.class),
                eq(ResponseWrapper.class)
        )).thenReturn(successResponse);

        String output = cryptoUtil.decryptData("encryptedString");
        assertEquals("processed-value", output);
    }

    @Test(expected = CredentialRequestGeneratorUncheckedException.class)
    public void testEncryptDecryptDataErrorResponse() throws Exception {
        when(restUtil.postApi(
                eq(ApiName.ENCRYPTION),
                any(), any(), any(),
                any(),
                any(RequestWrapper.class),
                eq(ResponseWrapper.class)
        )).thenReturn(errorResponse);

        cryptoUtil.encryptData("abc");
    }

    @Test(expected = CredentialRequestGeneratorUncheckedException.class)
    public void testEncryptDecryptDataRestUtilThrowsException() throws Exception {
        when(restUtil.postApi(
                eq(ApiName.DECRYPTION),
                any(), any(), any(),
                any(),
                any(RequestWrapper.class),
                eq(ResponseWrapper.class)
        )).thenThrow(new RuntimeException("test exception"));

        cryptoUtil.decryptData("xyz");
    }
}