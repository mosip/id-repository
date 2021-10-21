package io.mosip.credential.request.generator.test.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.dto.Event;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialrRequestGeneratorException;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.impl.CredentialRequestServiceImpl;
import io.mosip.credential.request.generator.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.dto.CredentialRequestIdsDto;
import io.mosip.idrepository.core.dto.PageDto;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.http.ResponseWrapper;

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
	public void testCancelCredentialIssuanceSuccess() throws IOException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.cancelCredentialRequest("1234");
				assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}

	@Test
	public void testCancelCredentialIssuanceFailure() throws JsonProcessingException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("ISSUED");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.cancelCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}
	@Test
	public void testDataAccessLayerExceptionForCancelCredentialIssuance() throws JsonProcessingException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenThrow(new DataAccessLayerException("", "", new Throwable()));
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
	
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.cancelCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}
	@Test
	public void testEntityNullForCancelCredentialIssuance() throws JsonProcessingException {

		Mockito.when(credentialRepositary.update(Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(null);

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.cancelCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testCancelCredentialIssuanceIOException() throws IOException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new IOException());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.cancelCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testGetCredentialRequestStatusSuccess() throws IOException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto=credentialRequestServiceImpl.getCredentialRequestStatus("1234");
				assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}
	
	@Test
	public void testDataAccessLayerExceptionForGetCredentialRequestStatus() throws JsonProcessingException {
		Mockito.when(credentialRepositary.findById(Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testEntityNullForGetCredentialRequestStatus() throws JsonProcessingException {
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(null);
	    ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto=credentialRequestServiceImpl.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testGetCredentialRequestStatusIOException() throws IOException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new IOException());
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testUpdateCredentialStatusSuccess()
			throws JsonProcessingException, CredentialrRequestGeneratorException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		Event event = new Event();
		CredentialStatusEvent credentialStatusEvent = new CredentialStatusEvent();
		event.setRequestId("1234");
		event.setUrl("sampleUrl");
		credentialStatusEvent.setEvent(event);
		credentialRequestServiceImpl
				.updateCredentialStatus(credentialStatusEvent);

	}

	@Test(expected = CredentialrRequestGeneratorException.class)
	public void testDataAccessLayerExceptionForUpdateCredentialStatus()
			throws JsonProcessingException, CredentialrRequestGeneratorException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		Event event = new Event();
		CredentialStatusEvent credentialStatusEvent = new CredentialStatusEvent();
		event.setRequestId("1234");
		event.setUrl("sampleUrl");
		credentialStatusEvent.setEvent(event);
		credentialRequestServiceImpl.updateCredentialStatus(credentialStatusEvent);
	}

	@Test(expected = CredentialrRequestGeneratorException.class)
	public void testEntityNullForUpdateCredentialStatus()
			throws JsonProcessingException, CredentialrRequestGeneratorException {
	
		Mockito.when(credentialRepositary.update(Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(null);
		Event event = new Event();
		CredentialStatusEvent credentialStatusEvent = new CredentialStatusEvent();
		event.setRequestId("1234");
		event.setUrl("sampleUrl");
		credentialStatusEvent.setEvent(event);
		credentialRequestServiceImpl.updateCredentialStatus(credentialStatusEvent);

	}

	@Test
	public void testGetRequestIdsWithStatusCodeSuccess() throws IOException {
		List<CredentialEntity> credentialList=new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);

		Mockito.when(credentialRepositary.fingByStatusCode(Mockito.any(), Mockito.any())).thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", null, 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getResponse());
	}

	@Test
	public void testGetRequestIdsWithEffectiveDtimeSuccess() throws IOException {
		List<CredentialEntity> credentialList = new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);

		Mockito.when(
				credentialRepositary.fingByStatusCodeWithEffectiveDtimes(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", "2018-12-10T06:12:52.994Z", 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getResponse());
	}

	@Test
	public void testGetRequestIdsWithDataNotFound() throws IOException {
		List<CredentialEntity> credentialList = new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);

		Mockito.when(credentialRepositary.fingByStatusCode(Mockito.any(), Mockito.any())).thenReturn(null);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", null, 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getErrors().get(0));
	}

	@Test
	public void testGetRequestIdsIoException() throws IOException {
		List<CredentialEntity> credentialList = new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);

		Mockito.when(credentialRepositary.fingByStatusCode(Mockito.any(), Mockito.any())).thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new IOException());
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", null, 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getErrors().get(0));
	}

	@Test
	public void testGetRequestIdsWithDataAccessLayerException() throws IOException
	{
		List<CredentialEntity> credentialList = new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);

		Mockito.when(credentialRepositary.fingByStatusCode(Mockito.any(), Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", null, 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getErrors().get(0));
	}

	@Test
	public void testGetRequestIdsWithException() throws IOException {
		List<CredentialEntity> credentialList = new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");

		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);
		Mockito.when(credentialRepositary.fingByStatusCode(Mockito.any(), Mockito.any())).thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", null, 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getErrors().get(0));
	}

	@Test
	public void testGetRequestIdsWithDateParseException() throws IOException {
		List<CredentialEntity> credentialList = new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);
		Mockito.when(
				credentialRepositary.fingByStatusCodeWithEffectiveDtimes(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> credentialgetRequestIdsresponseDto = credentialRequestServiceImpl
				.getRequestIds("FAILED", "2018-12-10:52.94Z", 0, 1, "updateDateTime", "ASC");
		assertNotNull(credentialgetRequestIdsresponseDto.getErrors().get(0));
	}

	@Test
	public void testRetriggerCredentialRequestSuccess() throws IOException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("FAILED");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.retriggerCredentialRequest("1234");
		assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}

	@Test
	public void testRetriggerCredentialRequestFailure() throws JsonProcessingException {
		Optional<CredentialEntity> entity = null;
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.retriggerCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testDataAccessLayerExceptionForRetriggerCredentialRequest() throws JsonProcessingException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.retriggerCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testRetriggerCredentialRequestIOException() throws IOException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		Mockito.when(credentialRepositary.update(Mockito.any())).thenReturn(credentialEntity);
		Mockito.when(credentialRepositary.findById(Mockito.any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		Mockito.when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new IOException());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.retriggerCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}


}
