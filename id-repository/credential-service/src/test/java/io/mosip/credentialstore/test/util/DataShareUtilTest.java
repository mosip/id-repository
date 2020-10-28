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

import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.DataShareResponseDto;
import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.dto.SignatureResponse;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
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
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception{

		dataShareResponseDto = new DataShareResponseDto();
         dataShare = new DataShare();
		dataShare.setUrl("testurl");
		dataShareResponseDto.setDataShare(dataShare);
		dataShareResponse = "response";
		
		Mockito.when(objectMapper.readValue(dataShareResponse, DataShareResponseDto.class)).thenReturn(dataShareResponseDto);

		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataShareResponse);
	}
	@Test
	public void dataShareSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		
		 DataShare dataShareResponse=dataShareUtil.getDataShare(sample, "policyId", "partnerId");
		 assertEquals(dataShareResponse.getUrl(),dataShare.getUrl());
		

	}
	
	@Test(expected = DataShareException.class)
	public void dataShareResponseObjectNullTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(objectMapper.readValue(dataShareResponse, DataShareResponseDto.class)).thenReturn(null);
		 DataShare dataShareResponse=dataShareUtil.getDataShare(sample, "policyId", "partnerId");
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
		
	dataShareUtil.getDataShare(sample, "policyId", "partnerId");

	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpClientErrorException);
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		dataShareUtil.getDataShare(sample, "policyId", "partnerId");
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpServerErrorException);
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		dataShareUtil.getDataShare(sample, "policyId", "partnerId");
	}
}
