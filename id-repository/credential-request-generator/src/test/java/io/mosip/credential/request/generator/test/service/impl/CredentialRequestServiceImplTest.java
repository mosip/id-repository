package io.mosip.credential.request.generator.test.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.impl.CredentialRequestServiceImpl;
import io.mosip.credential.request.generator.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueResponseDto;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.http.ResponseWrapper;

import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class CredentialRequestServiceImplTest {

	
	@Mock
	CredentialRepositary<CredentialEntity, String> credentialRepositary;
	
	@InjectMocks
	private CredentialRequestServiceImpl credentialRequestServiceImpl;
	
	@Mock
	private Environment env;
	
	@Mock
	CredentialEntity credentialEntity;
	
	
	@Mock
	Utilities utilities;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@Mock
	private AuditHelper auditHelper;

	
	@Before
	public void setUp() {
		
		Mockito.when(utilities.generateId()).thenReturn("123456");
		Mockito.when(env.getProperty("mosip.credential.request.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		
	}
	

	@Test
	public void testCreateCredentialIssuanceSuccess() throws JsonProcessingException {
		Mockito.when(credentialRepositary.save(Mockito.any())).thenReturn(credentialEntity);
		CredentialIssueRequestDto credentialIssueRequestDto=new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.createCredentialIssuance(credentialIssueRequestDto);
		assertEquals("123456", credentialIssueResponseDto.getResponse().getRequestId());
	}
	
	@Test
	public void testDataAccessLayerExceptionForCreateCredential() throws JsonProcessingException {
		Mockito.when(credentialRepositary.save(Mockito.any())).thenThrow(new DataAccessLayerException("", "", new Throwable()));
		CredentialIssueRequestDto credentialIssueRequestDto=new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.createCredentialIssuance(credentialIssueRequestDto);
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}
	@Test
	public void testCancelCredentialIssuanceSuccess() throws JsonProcessingException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
	
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.cancelCredentialRequest("1234");
				assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}
	@Test
	public void testDataAccessLayerExceptionForCancelCredentialIssuance() throws JsonProcessingException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenThrow(new DataAccessLayerException("", "", new Throwable()));
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
	
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.cancelCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}
	@Test
	public void testGetCredentialRequestStatusSuccess() throws JsonProcessingException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
	
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto=credentialRequestServiceImpl.getCredentialRequestStatus("1234");
				assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}
	
	@Test
	public void testDataAccessLayerExceptionFoGetCredentialRequestStatus() throws JsonProcessingException {
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenThrow(new DataAccessLayerException("", "", new Throwable()));
	    ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto=credentialRequestServiceImpl.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}
}
