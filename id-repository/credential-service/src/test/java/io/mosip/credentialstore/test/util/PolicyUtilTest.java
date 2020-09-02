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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.dto.PolicyManagerResponseDto;
import io.mosip.credentialstore.dto.PolicyResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.PolicyUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
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
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws IOException, ApiNotAccessibleException {
		
		policyManagerResponseDto = new PolicyManagerResponseDto();
		PolicyResponseDto responseData = new PolicyResponseDto();
		responseData.setPolicyId("1234");
		policyManagerResponseDto.setResponse(responseData);

		ResponseEntity<String> response = new ResponseEntity<String>(policyResponse, HttpStatus.OK);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(policyResponse);

		Mockito.when(objectMapper.readValue(policyResponse, PolicyManagerResponseDto.class))
				.thenReturn(policyManagerResponseDto);
	}

	@Test
	public void policySuccessTest() throws IOException, PolicyException, ApiNotAccessibleException {


		PolicyResponseDto policyResponseDto = policyUtil.getPolicyDetail("1234", "3456");
		assertEquals(policyResponseDto.getPolicyId(), "1234");

	}

	@Test(expected = PolicyException.class)
	public void testIOException() throws IOException, PolicyException, ApiNotAccessibleException {

		Mockito.when(objectMapper.readValue(policyResponse, PolicyManagerResponseDto.class))
				.thenThrow(new IOException());
		policyUtil.getPolicyDetail("1234", "3456");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws IOException, ApiNotAccessibleException, PolicyException {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");

		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpClientErrorException);
		policyUtil.getPolicyDetail("1234", "3456");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws  IOException, ApiNotAccessibleException, PolicyException {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpServerErrorException);
		policyUtil.getPolicyDetail("1234", "3456");
	}

	@Test(expected = PolicyException.class)
	public void policyFailureTest() throws  IOException, PolicyException, ApiNotAccessibleException {
		ServiceError error = new ServiceError();
		error.setErrorCode("PLC-GET-001");
		error.setMessage("policy error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		policyManagerResponseDto.setErrors(errors);
		policyUtil.getPolicyDetail("1234", "3456");
	}
}
