package io.mosip.credential.request.generator.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.kernel.core.http.RequestWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.dao.CredentialDao;
import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.dto.Event;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorException;
import io.mosip.credential.request.generator.util.Utilities;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.http.ResponseWrapper;

import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class CredentialRequestServiceImplTest {

	@Mock
	private CredentialDao credentialDao;

	@InjectMocks
	private CredentialRequestServiceImpl credentialRequestServiceImpl;

	@Mock
	private EnvUtil env;

	@Mock
	CredentialEntity credentialEntity;


	@Mock
	Utilities utilities;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private AuditHelper auditHelper;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private AuditRequestBuilder auditBuilder;

	@Mock
	private RestUtil restUtil;


	@Before
	public void setUp() {
		when(utilities.generateId()).thenReturn("123456");
		EnvUtil.setDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	}


	@Test
	public void testCreateCredentialIssuanceSuccess() throws JsonProcessingException {
		CredentialIssueRequest credentialIssueRequestDto=new CredentialIssueRequest();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		when(objectMapper.writeValueAsString(any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.createCredentialIssuance(credentialIssueRequestDto);
		assertEquals("123456", credentialIssueResponseDto.getResponse().getRequestId());
	}

	@Test
	public void testCreateCredentialIssuanceByRidSuccess() throws JsonProcessingException {
		CredentialIssueRequest credentialIssueRequestDto=new CredentialIssueRequest();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		when(objectMapper.writeValueAsString(any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.createCredentialIssuanceByRid(credentialIssueRequestDto,"123456");
		assertEquals("123456", credentialIssueResponseDto.getResponse().getRequestId());
	}

	@Test
	public void testDataAccessLayerExceptionForCreateCredentialByRid() throws JsonProcessingException {
		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		CredentialIssueRequest credentialIssueRequestDto=new CredentialIssueRequest();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		when(objectMapper.writeValueAsString(any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.createCredentialIssuanceByRid(credentialIssueRequestDto,"123456");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void createCredentialIssuanceByRid_withException() throws JsonProcessingException {
		doThrow(new RuntimeException("Unknown Error")).when(credentialDao).save(any());
		CredentialIssueRequest credentialIssueRequestDto=new CredentialIssueRequest();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		when(objectMapper.writeValueAsString(any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl.createCredentialIssuanceByRid(credentialIssueRequestDto,"123456");
		assertEquals("unknown exception", credentialIssueResponseDto.getErrors().get(0).getMessage());
	}

	@Test
	public void testDataAccessLayerExceptionForCreateCredential() throws JsonProcessingException {
		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		CredentialIssueRequest credentialIssueRequestDto=new CredentialIssueRequest();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		when(objectMapper.writeValueAsString(any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.createCredentialIssuance(credentialIssueRequestDto);
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void createCredentialIssuance_withException() throws Exception {
		doThrow(new RuntimeException("Unknown error")).when(credentialDao).save(any());
		CredentialIssueRequest credentialIssueRequestDto=new CredentialIssueRequest();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setEncrypt(true);
		when(objectMapper.writeValueAsString(any())).thenReturn(credentialIssueRequestDto.toString());
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl.createCredentialIssuance(credentialIssueRequestDto);
		assertEquals("IDR-CRG-005", credentialIssueResponseDto.getErrors().get(0).getErrorCode());
	}

	@Test
	public void testCancelCredentialIssuanceSuccess() throws IOException {
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		when(credentialDao.findById(any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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
		when(credentialDao.findById(any())).thenReturn(entity);

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
		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		when(credentialDao.findById(any())).thenReturn(entity);

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto=credentialRequestServiceImpl.cancelCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}
	@Test
	public void testEntityNullForCancelCredentialIssuance() throws JsonProcessingException {
		String requestId = "1234";
		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		when(credentialDao.findById(requestId)).thenReturn(Optional.ofNullable(null));
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.cancelCredentialRequest(requestId);
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testCancelCredentialIssuanceIOException() throws IOException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		when(credentialDao.findById(any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new JsonMappingException(""));
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
		when(credentialDao.findById(any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto=credentialRequestServiceImpl.getCredentialRequestStatus("1234");
		assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}

	@Test
	public void testDataAccessLayerExceptionForGetCredentialRequestStatus() throws JsonProcessingException {
		when(credentialDao.findById(any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testEntityEmptyForGetCredentialRequestStatus() throws JsonProcessingException {
		when(credentialDao.findById(any())).thenReturn(Optional.empty());
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto=credentialRequestServiceImpl.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testGetCredentialRequestStatusIOException() throws IOException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		when(credentialDao.findById(any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new JsonMappingException(""));
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.getCredentialRequestStatus("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void testUpdateCredentialStatusSuccess()
			throws JsonProcessingException, CredentialRequestGeneratorException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		when(credentialDao.findById(any())).thenReturn(entity);
		Event event = new Event();
		CredentialStatusEvent credentialStatusEvent = new CredentialStatusEvent();
		event.setRequestId("1234");
		event.setUrl("sampleUrl");
		credentialStatusEvent.setEvent(event);
		credentialRequestServiceImpl
				.updateCredentialStatus(credentialStatusEvent);

	}

	@Test(expected = CredentialRequestGeneratorException.class)
	public void testDataAccessLayerExceptionForUpdateCredentialStatus()
			throws JsonProcessingException, CredentialRequestGeneratorException {
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		Optional<CredentialEntity> entity = Optional.of(credentialEntity);
		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		when(credentialDao.findById(any())).thenReturn(entity);
		Event event = new Event();
		CredentialStatusEvent credentialStatusEvent = new CredentialStatusEvent();
		event.setRequestId("1234");
		event.setUrl("sampleUrl");
		credentialStatusEvent.setEvent(event);
		credentialRequestServiceImpl.updateCredentialStatus(credentialStatusEvent);
	}

	@Test(expected = CredentialRequestGeneratorException.class)
	public void testEntityNullForUpdateCredentialStatus()
			throws JsonProcessingException, CredentialRequestGeneratorException {

		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		when(credentialDao.findById(any())).thenReturn(Optional.empty());
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

		when(credentialDao.findByStatusCode(any(), any())).thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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

		when(
				credentialDao.findByStatusCodeWithEffectiveDtimes(any(), any(), any()))
				.thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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
		new PageImpl<>(credentialList);

		when(credentialDao.findByStatusCode(any(), any())).thenReturn(null);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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

		when(credentialDao.findByStatusCode(any(), any())).thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new JsonMappingException(""));
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
		new PageImpl<>(credentialList);

		when(credentialDao.findByStatusCode(any(), any()))
				.thenThrow(new DataAccessLayerException("", "", new Throwable()));
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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
		when(credentialDao.findByStatusCode(any(), any())).thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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
		when(
				credentialDao.findByStatusCodeWithEffectiveDtimes(any(), any(), any()))
				.thenReturn(page);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		credentialIssueRequestDto.setIssuer("test");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
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
		when(credentialDao.findById(any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenReturn(credentialIssueRequestDto);
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.retriggerCredentialRequest("1234");
		assertEquals("1234", credentialIssueResponseDto.getResponse().getRequestId());
	}

	@Test
	public void testRetriggerCredentialRequestFailure() throws JsonProcessingException {
		when(credentialDao.findById(any())).thenReturn(Optional.empty());

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
		doThrow(new DataAccessLayerException("", "", new Throwable())).when(credentialDao).save(any());
		when(credentialDao.findById(any())).thenReturn(entity);

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
		when(credentialDao.findById(any())).thenReturn(entity);
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setCredentialType("MOSIP");
		credentialIssueRequestDto.setId("123");
		when(objectMapper.readValue(credentialEntity.getRequest(), CredentialIssueRequestDto.class))
				.thenThrow(new JsonMappingException(""));
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseDto = credentialRequestServiceImpl
				.retriggerCredentialRequest("1234");
		assertNotNull(credentialIssueResponseDto.getErrors().get(0));
	}

	@Test
	public void audit(){
		credentialRequestServiceImpl.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR,
				AuditEvents.RETRY_CREDENTIAL_REQUEST, "id", IdType.ID, "des");
	}

	@Test
	public void testAudit_IdRepoDataValidationException() throws Exception {

		AuditModules module = AuditModules.ID_REPO_CREDENTIAL_SERVICE;
		AuditEvents event = AuditEvents.RETRY_CREDENTIAL_REQUEST;
		String id = "12345";
		IdType idType = IdType.ID;
		String desc = "Test description";

		AuditRequestDTO auditRequestDTO = new AuditRequestDTO(); // Fill as needed
		RequestWrapper<AuditRequestDTO> requestWrapper = new RequestWrapper<>();

		when(securityManager.hash(id.getBytes())).thenReturn(Arrays.toString("hashedId".getBytes()));
		when(auditBuilder.buildRequest(eq(module), eq(event), any(), eq(idType), eq(desc)))
				.thenReturn(requestWrapper);

		doThrow(new IdRepoDataValidationException(IdRepoErrorConstants.BIO_EXTRACTION_ERROR))
				.when(restUtil).postApi(
						eq(ApiName.KERNELAUDITMANAGER), isNull(), eq(id), eq(desc),
						eq(MediaType.APPLICATION_JSON), any(HttpEntity.class), eq(AuditResponseDTO.class));

		credentialRequestServiceImpl.audit(module, event, id, idType, desc);
	}

	@Test
	public void testAudit_Exception() throws Exception {

		AuditModules module = AuditModules.ID_REPO_CREDENTIAL_SERVICE;
		AuditEvents event = AuditEvents.RETRY_CREDENTIAL_REQUEST;
		String id = "12345";
		IdType idType = IdType.ID;
		String desc = "Test description";

		AuditRequestDTO auditRequestDTO = new AuditRequestDTO(); // Fill as needed
		RequestWrapper<AuditRequestDTO> requestWrapper = new RequestWrapper<>();

		when(securityManager.hash(id.getBytes())).thenReturn(Arrays.toString("hashedId".getBytes()));
		when(auditBuilder.buildRequest(eq(module), eq(event), any(), eq(idType), eq(desc)))
				.thenReturn(requestWrapper);

		doThrow(new Exception())
				.when(restUtil).postApi(
						eq(ApiName.KERNELAUDITMANAGER), isNull(), eq(id), eq(desc),
						eq(MediaType.APPLICATION_JSON), any(HttpEntity.class), eq(AuditResponseDTO.class));

		credentialRequestServiceImpl.audit(module, event, id, idType, desc);
	}



}
