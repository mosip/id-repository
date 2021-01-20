package io.mosip.credentialstore.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.DataShareResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.dto.ErrorDTO;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*",
		"com.sun.org.apache.xalan.*" })
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(value = URL.class)
public class DataShareUtilTest {
	/** The rest template. */
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;
	
	@InjectMocks
	DataShareUtil dataShareUtil;
	
	DataShareResponseDto dataShareResponseDto;
	
	DataShare dataShare;



	String dataShareResponse;

	@Mock
	private Environment env;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception{
		Mockito.when(env.getProperty("CREATEDATASHARE"))
				.thenReturn("/v1/datashare/create");
		dataShareResponseDto = new DataShareResponseDto();
         dataShare = new DataShare();
		dataShare.setUrl("testurl");
		dataShareResponseDto.setDataShare(dataShare);
		dataShareResponse = "response";
		
		Mockito.when(objectMapper.readValue(dataShareResponse, DataShareResponseDto.class)).thenReturn(dataShareResponseDto);

		Mockito.when(restUtil.postApi(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(dataShareResponse);
	}
	@Test
	public void dataShareSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		
		DataShare dataShareResponse = dataShareUtil.getDataShare(sample, "policyId", "partnerId",
				"datashare-service", "requestId");
		 assertEquals(dataShareResponse.getUrl(),dataShare.getUrl());
		

	}
	
	@Test(expected = DataShareException.class)
	public void dataShareResponseObjectNullTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(objectMapper.readValue(dataShareResponse, DataShareResponseDto.class)).thenReturn(null);
		DataShare dataShareResponse = dataShareUtil.getDataShare(sample, "policyId", "partnerId",
				"datashare-service", "requestId");
		 assertEquals(dataShareResponse.getUrl(),dataShare.getUrl());
		

	}
	@Test(expected = DataShareException.class)
	public void dataShareResponseWithErrorTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		ErrorDTO error = new ErrorDTO();
		error.setErrorCode("DAT-SER-001");
		error.setMessage("Data Encryption failed");
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		errors.add(error);
		dataShareResponseDto.setErrors(errors);
		
		dataShareUtil.getDataShare(sample, "policyId", "partnerId", "datashare-service", "requestId");

	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpClientErrorException);
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(restUtil.postApi(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenThrow(e);
		dataShareUtil.getDataShare(sample, "policyId", "partnerId", "datashare-service", "requestId");
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpServerErrorException);
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(restUtil.postApi(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenThrow(e);
		dataShareUtil.getDataShare(sample, "policyId", "partnerId", "datashare-service", "requestId");
	}
}
