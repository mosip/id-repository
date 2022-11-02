package io.mosip.credential.request.generator.test.batch.config;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.mosip.credential.request.generator.batch.config.CredentialItemReprocessTasklet;
import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.dao.CredentialDao;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.util.EnvUtil;

@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class CredentialItemReprocessTaskletTest {

	@InjectMocks
	private CredentialItemReprocessTasklet credentialItemReprocessTasklet;


	@Mock
	CredentialEntity credentialEntity;


	@Mock
	private ObjectMapper mapper;

	@Mock
	private RestUtil restUtil;
	
	CredentialIssueRequestDto credentialIssueRequestDto;

	String responseString;

	CredentialServiceResponseDto credentialServiceResponseDto;

	CredentialEntity credential;
	
	@Mock
	private CredentialDao credentialDao;
	
	List<CredentialEntity> credentialEntities;


	@Before
	public void setUp() {
		ReflectionTestUtils.setField(credentialItemReprocessTasklet, "retryMaxCount", 3);
		credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		responseString = "response";
		credentialServiceResponseDto = new CredentialServiceResponseDto();
		credential = new CredentialEntity();
		credential.setRequestId("test123");
		credential.setRetryCount(1);
		credential.setRequest("request");
		credential.setStatusCode("FAILED");
		ReflectionTestUtils.setField(credentialItemReprocessTasklet, "threadCount",
				1);
		credentialItemReprocessTasklet.init();
		 credentialEntities=new ArrayList();
			credentialEntities.add(credential);
			Mockito.when(credentialDao.getCredentialsForReprocess(Mockito.any()))
			.thenReturn(credentialEntities);
	}

	@Test
	public void testProcessSuccess() throws Exception {

		Mockito.when(mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		Mockito.when(mapper.readValue(responseString, CredentialServiceResponseDto.class))
				.thenReturn(credentialServiceResponseDto);
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(responseString);
		CredentialServiceResponse credentialServiceResponse = new CredentialServiceResponse();
		credentialServiceResponse.setCredentialId("testcredentialid");
		credentialServiceResponse.setStatus("ISSUED");;
		credentialServiceResponseDto.setResponse(credentialServiceResponse);
		RepeatStatus repeatStatus = credentialItemReprocessTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	public void testProcessFailure() throws Exception {

		Mockito.when(mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		Mockito.when(mapper.readValue(responseString, CredentialServiceResponseDto.class))
				.thenReturn(credentialServiceResponseDto);
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseString);
		List<ErrorDTO> errors = new ArrayList<>();
		ErrorDTO error = new ErrorDTO();
		error.setErrorCode("IDR-CRS-008");
		error.setMessage("Failed to get policy details");
		errors.add(error);
		credentialServiceResponseDto.setErrors(errors);
		RepeatStatus repeatStatus = credentialItemReprocessTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	public void testProcessIOException() throws Exception {

		Mockito.when(mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class))
				.
				thenThrow(new JsonMappingException("mapping exception"));

		RepeatStatus repeatStatus = credentialItemReprocessTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	public void testProcessHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpServerErrorException);

		Mockito.when(mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		Mockito.when(mapper.readValue(responseString, CredentialServiceResponseDto.class))
				.thenReturn(credentialServiceResponseDto);
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);

		RepeatStatus repeatStatus = credentialItemReprocessTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	public void testProcessHttpClientException() throws Exception
	{
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e = new Exception(httpClientErrorException);

		Mockito.when(mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		Mockito.when(mapper.readValue(responseString, CredentialServiceResponseDto.class))
				.thenReturn(credentialServiceResponseDto);
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		RepeatStatus repeatStatus = credentialItemReprocessTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}
}
