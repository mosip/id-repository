package io.mosip.credentialstore.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.dto.PartnerCredentialTypePolicyDto;
import io.mosip.credentialstore.dto.PartnerExtractor;
import io.mosip.credentialstore.dto.PartnerExtractorResponse;
import io.mosip.credentialstore.dto.PartnerExtractorResponseDto;
import io.mosip.credentialstore.dto.PolicyManagerResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.PartnerException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.credentialstore.util.PolicyUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class PolicyUtilTest {
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;
	
	@InjectMocks
	PolicyUtil policyUtil;
	
	private PolicyManagerResponseDto policyManagerResponseDto;

	String policyResponse;
	
	private PartnerExtractorResponseDto partnerExtractorResponseDto;

	String partnerextractorResponse;
	
	@Mock
	CacheManager cacheManager;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		policyManagerResponseDto = new PolicyManagerResponseDto();
		PartnerCredentialTypePolicyDto responseData = new PartnerCredentialTypePolicyDto();
		responseData.setPolicyId("1234");
		policyManagerResponseDto.setResponse(responseData);
		partnerExtractorResponseDto = new PartnerExtractorResponseDto();
		PartnerExtractorResponse partnerExtractorResponse = new PartnerExtractorResponse();
		List<PartnerExtractor> extractors = new ArrayList<>();
		partnerExtractorResponse.setExtractors(extractors);
		partnerExtractorResponseDto.setResponse(partnerExtractorResponse);
		Mockito.when(objectMapper.readValue(policyResponse, PolicyManagerResponseDto.class))
				.thenReturn(policyManagerResponseDto);

		Mockito.when(objectMapper.readValue(partnerextractorResponse, PartnerExtractorResponseDto.class))
				.thenReturn(partnerExtractorResponseDto);
	}

	@Test
	public void policySuccessTest() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(policyResponse);

		PartnerCredentialTypePolicyDto policyResponseDto = policyUtil.getPolicyDetail("euin", "3456", "requestId");
		assertEquals(policyResponseDto.getPolicyId(), "1234");

	}

	@Test(expected = PolicyException.class)
	public void testIOException() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(policyResponse);
		Mockito.when(objectMapper.readValue(policyResponse, PolicyManagerResponseDto.class))
				.thenThrow(new JsonMappingException(""));
		
		policyUtil.getPolicyDetail("euin", "3456", "requestId");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpClientErrorException);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		policyUtil.getPolicyDetail("euin", "3456", "requestId");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws  Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpServerErrorException);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		policyUtil.getPolicyDetail("euin", "3456", "requestId");
	}

	@Test(expected = PolicyException.class)
	public void policyFailureTest() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(policyResponse);
		ServiceError error = new ServiceError();
		error.setErrorCode("PLC-GET-001");
		error.setMessage("policy error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		policyManagerResponseDto.setErrors(errors);
		policyUtil.getPolicyDetail("euin", "3456", "requestId");
	}

	@Test
	public void partnerExtractorSuccessTest() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(partnerextractorResponse);

		PartnerExtractorResponse policyResponseDto = policyUtil.getPartnerExtractorFormat("1234", "3456", "requestId");
		assertNotNull(policyResponseDto.getExtractors());

	}

	@Test(expected = PartnerException.class)
	public void partnerExtractorFailureTest() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(partnerextractorResponse);
		ServiceError error = new ServiceError();
		error.setErrorCode("PMS_PRT_065");
		error.setMessage("extraction error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		partnerExtractorResponseDto.setErrors(errors);
		policyUtil.getPartnerExtractorFormat("1234", "3456", "requestId");
	}

	@Test(expected = PartnerException.class)
	public void testPartnerIOException() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(partnerextractorResponse);
		Mockito.when(objectMapper.readValue(partnerextractorResponse, PartnerExtractorResponseDto.class))
				.thenThrow(new JsonMappingException(""));
		policyUtil.getPartnerExtractorFormat("1234", "3456", "requestId");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testPartnerExtractorHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpClientErrorException);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		policyUtil.getPartnerExtractorFormat("1234", "3456", "requestId");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testPartnerExtractorHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpServerErrorException);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		policyUtil.getPartnerExtractorFormat("1234", "3456", "requestId");
	}

	@Test
	public void NoPartnerExtractorTest() throws Exception {
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(partnerextractorResponse);
		ServiceError error = new ServiceError();
		error.setErrorCode("PMS_PRT_064");
		error.setMessage("extraction error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		partnerExtractorResponseDto.setErrors(errors);

		PartnerExtractorResponse policyResponseDto = policyUtil.getPartnerExtractorFormat("euin", "3456", "requestId");
		assertNull(policyResponseDto);

	}
	
}
