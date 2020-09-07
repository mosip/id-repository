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

import io.mosip.credentialstore.dto.CryptoWithPinResponseDto;
import io.mosip.credentialstore.dto.CryptoZkResponseDto;
import io.mosip.credentialstore.dto.EncryptWithPinResponseDto;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class EncryptionUtilTest {
	@Mock
	private Environment environment;

	/** The rest template. */
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	EncryptionUtil encryptionUtil;

	private CryptoWithPinResponseDto cryptoWithPinResponseDto;
	
	private CryptoZkResponseDto cryptoZkResponseDto;

	String cryptoResponse;
	String test;
	
	List<ZkDataAttribute> zkDataAttributeList ;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		 test = "testdata";
		cryptoResponse = "response";
		cryptoWithPinResponseDto=new CryptoWithPinResponseDto();
		EncryptWithPinResponseDto encryptWithPinResponseDto=new EncryptWithPinResponseDto();
		encryptWithPinResponseDto.setData(test);
		cryptoWithPinResponseDto.setResponse(encryptWithPinResponseDto);
		cryptoZkResponseDto=new CryptoZkResponseDto();
		EncryptZkResponseDto encryptZkResponseDto=new EncryptZkResponseDto();
		 zkDataAttributeList=new ArrayList<>();
		ZkDataAttribute zkDataAttribute=new ZkDataAttribute();
		zkDataAttribute.setIdentifier("name");
		zkDataAttribute.setValue("test");
		zkDataAttributeList.add(zkDataAttribute);
		encryptZkResponseDto.setZkDataAttributes(zkDataAttributeList);
		cryptoZkResponseDto.setResponse(encryptZkResponseDto);
		Mockito.when(environment.getProperty("mosip.credential.service.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(cryptoResponse);
	}
	
	@Test
	public void encryptionWithPinSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataEncryptionFailureException {
		Mockito.when(objectMapper.readValue(cryptoResponse, CryptoWithPinResponseDto.class)).thenReturn(cryptoWithPinResponseDto);
	    String encryptedData = encryptionUtil.encryptDataWithPin(test, "test123");
		assertEquals(test, encryptedData);

	}
	@Test(expected = DataEncryptionFailureException.class)
	public void encryptionWithPinFailureTest() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException, DataEncryptionFailureException {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-KEY-001");
		error.setMessage("crypto error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		cryptoWithPinResponseDto.setErrors(errors);
		Mockito.when(objectMapper.readValue(cryptoResponse, CryptoWithPinResponseDto.class)).thenReturn(cryptoWithPinResponseDto);
	    encryptionUtil.encryptDataWithPin(test, "test123");
	}
	@Test(expected = DataEncryptionFailureException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException, DataEncryptionFailureException {

		Mockito.when(objectMapper.readValue(cryptoResponse, CryptoWithPinResponseDto.class)).thenThrow(new IOException());
		encryptionUtil.encryptDataWithPin(test, "test123");
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
	
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpClientErrorException);
		encryptionUtil.encryptDataWithPin(test, "test123");
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
	
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpServerErrorException);
		encryptionUtil.encryptDataWithPin(test, "test123");
	}
	
	@Test
	public void encryptionWithZkSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataEncryptionFailureException {
		Mockito.when(objectMapper.readValue(cryptoResponse, CryptoZkResponseDto.class)).thenReturn(cryptoZkResponseDto);
	    EncryptZkResponseDto encryptZkResponseDto= encryptionUtil.encryptDataWithZK("12345678", zkDataAttributeList);
		assertEquals(zkDataAttributeList, encryptZkResponseDto.getZkDataAttributes());

	}
	@Test(expected = DataEncryptionFailureException.class)
	public void encryptionWithZkFailureTest() throws JsonParseException, JsonMappingException, IOException, DataEncryptionFailureException, ApiNotAccessibleException  {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-KEY-001");
		error.setMessage("crypto error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		cryptoZkResponseDto.setErrors(errors);
		Mockito.when(objectMapper.readValue(cryptoResponse, CryptoZkResponseDto.class)).thenReturn(cryptoZkResponseDto);
		encryptionUtil.encryptDataWithZK("12345678", zkDataAttributeList);
	}
	@Test(expected = DataEncryptionFailureException.class)
	public void encryptionWithZktestIOException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException, DataEncryptionFailureException {

		Mockito.when(objectMapper.readValue(cryptoResponse, CryptoWithPinResponseDto.class)).thenThrow(new IOException());
		encryptionUtil.encryptDataWithZK("12345678", zkDataAttributeList);
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void encryptionWithZktestHttpClientException() throws Exception {
	
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpClientErrorException);
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		encryptionUtil.encryptDataWithZK("12345678", zkDataAttributeList);
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void encryptionWithZktestHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpServerErrorException);
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		encryptionUtil.encryptDataWithZK("12345678", zkDataAttributeList);
	}
	
}
