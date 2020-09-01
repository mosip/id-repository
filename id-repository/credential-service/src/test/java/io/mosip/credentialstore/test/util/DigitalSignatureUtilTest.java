package io.mosip.credentialstore.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.dto.SignatureResponse;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class DigitalSignatureUtilTest {
	/** The environment. */
	@Mock
	private Environment environment;

	/** The rest template. */
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	DigitalSignatureUtil digitalSignatureUtil;

	private SignResponseDto signResponseDto;

	String signResponse;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException{

		signResponseDto = new SignResponseDto();
		SignatureResponse sign = new SignatureResponse();
		sign.setSignature("testdata");
		signResponseDto.setResponse(sign);
		signResponse = "{\r\n" + 
    		"  \"id\": \"string\",\r\n" + 
    		"  \"version\": \"string\",\r\n" + 
    		"  \"responsetime\": \"2020-07-28T10:06:31.530Z\",\r\n" + 
    		"  \"metadata\": null,\r\n" + 
    		"  \"response\": {\r\n" + 
				"    \"signature\": \"testdata\",\r\n" + 
    		"    \"timestamp\": \"2020-07-28T10:06:31.502Z\"\r\n" + 
    		"  },\r\n" + 
    		"  \"errors\": null\r\n" + 
				"}";
		
		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class)).thenReturn(signResponseDto);
		Mockito.when(environment.getProperty("mosip.credential.service.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(signResponse);
	}
	
	@Test
	public void signSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		
		String signedData = digitalSignatureUtil.sign(sample);
		assertEquals(test, signedData);
		

	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpClientErrorException);
		digitalSignatureUtil.sign(sample);
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpServerErrorException);
		digitalSignatureUtil.sign(sample);
	}
	@Test(expected = SignatureException.class)
	public void signFailureTest() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-SIG-001");
		error.setMessage("sign error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		signResponseDto.setErrors(errors);
		String test = "testdata";
		byte[] sample = test.getBytes();

		digitalSignatureUtil.sign(sample);
	}
	@Test(expected = SignatureException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class)).thenThrow(new IOException());
		digitalSignatureUtil.sign(sample);
	}
}
