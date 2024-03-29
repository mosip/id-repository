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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.dto.JWTSignatureResponseDto;
import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class DigitalSignatureUtilTest {
	/** The environment. */
	@Mock
	private EnvUtil environment;

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
	public void setUp() throws Exception {

		signResponseDto = new SignResponseDto();
		JWTSignatureResponseDto sign = new JWTSignatureResponseDto();
		sign.setJwtSignedData("testdata");
		signResponseDto.setResponse(sign);
		signResponse = "{\r\n" + "  \"id\": \"string\",\r\n" + "  \"version\": \"string\",\r\n"
				+ "  \"responsetime\": \"2020-07-28T10:06:31.530Z\",\r\n" + "  \"metadata\": null,\r\n"
				+ "  \"response\": {\r\n" + "    \"signature\": \"testdata\",\r\n"
				+ "    \"timestamp\": \"2020-07-28T10:06:31.502Z\"\r\n" + "  },\r\n" + "  \"errors\": null\r\n" + "}";

		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class)).thenReturn(signResponseDto);
		EnvUtil.setDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		EnvUtil.setCredServiceIncludeCertificateHash(false);
		EnvUtil.setCredServiceIncludeCertificate(false);
		EnvUtil.setCredServiceIncludePayload(false);
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(signResponse);
	}

	@Test
	public void signSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException {
		String test = "testdata";

		String signedData = digitalSignatureUtil.sign(test, "requestId");
		assertEquals(test, signedData);

	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpClientErrorException);
		String test = "testdata";

		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		digitalSignatureUtil.sign(test, "requestId");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpServerErrorException);
		String test = "testdata";

		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		digitalSignatureUtil.sign(test, "requestId");
	}

	@Test(expected = SignatureException.class)
	public void signFailureTest() throws JsonParseException, JsonMappingException, IOException,
			ApiNotAccessibleException, SignatureException {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-SIG-001");
		error.setMessage("sign error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		signResponseDto.setErrors(errors);
		String test = "testdata";

		digitalSignatureUtil.sign(test, "requestId");
	}

	@Test(expected = SignatureException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException,
			ApiNotAccessibleException, SignatureException {
		String test = "testdata";

		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class))
				.thenThrow(new JsonMappingException(""));
		digitalSignatureUtil.sign(test, "requestId");
	}

	@Test
	public void signVerCredSuccessTest() throws ApiNotAccessibleException, SignatureException {
		String test = "testdata";
		String signedData = digitalSignatureUtil.sign(test, "requestId");
		String response = digitalSignatureUtil.signVerCred(signedData, test);
		assertEquals(signedData, response);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException1() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpClientErrorException);
		String test = "testdata";

		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		digitalSignatureUtil.signVerCred(test, "requestId");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException1() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpServerErrorException);
		String test = "testdata";

		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		digitalSignatureUtil.signVerCred(test, "requestId");
	}

	@Test(expected = SignatureException.class)
	public void signFailureTest1() throws JsonParseException, JsonMappingException, IOException,
			ApiNotAccessibleException, SignatureException {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-SIG-001");
		error.setMessage("sign error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		signResponseDto.setErrors(errors);
		String test = "testdata";

		digitalSignatureUtil.signVerCred(test, "requestId");
	}

	@Test(expected = SignatureException.class)
	public void testIOException1() throws JsonParseException, JsonMappingException, IOException,
			ApiNotAccessibleException, SignatureException {
		String test = "testdata";

		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class))
				.thenThrow(new JsonMappingException(""));
		digitalSignatureUtil.signVerCred(test, "requestId");
	}

}
