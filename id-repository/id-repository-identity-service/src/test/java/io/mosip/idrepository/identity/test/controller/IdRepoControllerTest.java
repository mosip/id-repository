package io.mosip.idrepository.identity.test.controller;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.idrepository.core.dto.*;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.spi.AuthtypeStatusService;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.controller.IdRepoController;
import io.mosip.idrepository.identity.dto.AttributeListDto;
import io.mosip.idrepository.identity.dto.RidDto;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;

/**
 * The Class IdRepoControllerTest.
 *
 * @author Manoj SP
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class IdRepoControllerTest {

	private static final String TYPE = "type";

	private static final String REGISTRATION_ID = "registrationId";

	private static final String UIN = "UIN";

	@Mock
	private IdRepoService<IdRequestDTO, IdResponseDTO> idRepoService;

	@Mock
	private IdRequestValidator validator;

	@Mock
	private AuditHelper auditHelper;

	@Mock
	private AuthtypeStatusService authTypeStatusService;

	@InjectMocks
	IdRepoController controller;

	@Autowired
	ObjectMapper mapper;

	@Before
	public void before() {
		Map<String, String> id = Maps.newHashMap("read", "mosip.id.read");
		id.put("create", "mosip.id.create");
		id.put("update", "mosip.id.update");
		ReflectionTestUtils.setField(controller, "id", id);
		ReflectionTestUtils.setField(controller, "mapper", mapper);
		ReflectionTestUtils.setField(controller, "validator", validator);
		ReflectionTestUtils.setField(validator, "id", id);
		ReflectionTestUtils.setField(validator, "allowedTypes", Lists.newArrayList("bio", "demo", "all"));
	}

	@Test
	public void testAddIdentity() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdResponseDTO response = new IdResponseDTO();
		RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
		request.setId("mosip.id.create");
		IdRequestDTO<List<String>> requestDTO = new IdRequestDTO<>();
		Object identity = mapper.readValue(
				"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		requestDTO.setIdentity(identity);
		request.setRequest(requestDTO);
		when(validator.validateUin(any())).thenReturn(true);
		when(idRepoService.addIdentity(any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<List<String>>> responseEntity = controller.addIdentity(request,
				new BeanPropertyBindingResult(request, "IdRequestDTO"));
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test(expected = IdRepoAppException.class)
	public void testAddIdentityFailed()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdResponseDTO response = new IdResponseDTO();
		RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
		request.setId("mosip.id.creat");
		IdRequestDTO<List<String>> requestDTO = new IdRequestDTO<>();
		Object identity = mapper.readValue(
				"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		requestDTO.setIdentity(identity);
		request.setRequest(requestDTO);
		when(idRepoService.addIdentity(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		ResponseEntity<IdResponseDTO<List<String>>> responseEntity = controller.addIdentity(request,
				new BeanPropertyBindingResult(request, "IdRequestDTO"));
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	/**
	 * Test add identity exception.
	 *
	 * @throws IdRepoAppException   the id repo app exception
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testAddIdentityException() throws Throwable {
		try {
			RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
			request.setId("mosip.id.create");
			IdRequestDTO<List<String>> requestDTO = new IdRequestDTO<>();
			Object identity = mapper.readValue(
					"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);
			requestDTO.setIdentity(identity);
			request.setRequest(requestDTO);
			when(validator.validateUin(Mockito.anyString())).thenThrow(new InvalidIDException(null, null));
			BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "IdRequestDTO");
			errors.reject("errorCode");
			controller.addIdentity(request, errors);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATA_VALIDATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATA_VALIDATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityExceptionNullRequest() throws Throwable {
		try {
			RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
			request.setId("mosip.id.create");
			IdRequestDTO requestDTO = new IdRequestDTO();
			Object identity = mapper.readValue(
					"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);
			requestDTO.setIdentity(identity);
			request.setRequest(null);
			when(validator.validateUin(Mockito.anyString())).thenThrow(new InvalidIDException(null, null));
			BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "IdRequestDTO");
			errors.reject("errorCode");
			controller.addIdentity(request, errors);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), "request"), e.getErrorText());
		}
	}

	/**
	 * Test retrieve identity.
	 *
	 * @throws IdRepoAppException the id repo app exception
	 */
	@Test
	public void testRetrieveIdentity() throws IdRepoAppException {
		IdResponseDTO<List<String>> response = new IdResponseDTO<>();
		when(validator.validateUin(anyString())).thenReturn(true);
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentity("1234", "demo", null, null, null,
				null);
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityAll() throws IdRepoAppException {
		IdResponseDTO<List<String>> response = new IdResponseDTO<>();
		when(validator.validateUin(anyString())).thenReturn(true);
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentity("1234", "demo,all", null, null, null,
				null);
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityAllWithExtractionFormatsForVID() throws IdRepoAppException {
		when(validator.validateUin(any())).thenReturn(false);
		when(validator.validateVid(any())).thenReturn(true);
		IdResponseDTO<List<String>> response = new IdResponseDTO<>();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentity("1234", "demo,all", null,
				"fingerFormat", "irisFormat", "faceFormat");
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	/**
	 * Test retrieve identity.
	 *
	 * @throws IdRepoAppException the id repo app exception
	 */
	@Test
	public void testRetrieveIdentityInvalidUin() {
		try {
			when(idRepoService.retrieveIdentity(any(), any(), any(), any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id")));
			controller.retrieveIdentity("1234", "demo", null, null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id"),
					e.getErrorText());
		}
	}

	@Test(expected = IdRepoAppException.class)
	public void testRetrieveIdentityRequestParameterMap() throws IdRepoAppException {
		when(validator.validateType(any())).thenThrow(new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
				String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "type")));
		controller.retrieveIdentity("1234", "dem, abc", "UIN", null, null, null);
	}

	/**
	 * Test retrieve identity.
	 *
	 * @throws IdRepoAppException the id repo app exception
	 */
	@Test
	public void testRetrieveIdentityById() throws Throwable {
		RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
		IdResponseDTO response = new IdResponseDTO();
		when(validator.validateUin(anyString())).thenReturn(true);
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		String idRequest = "{\"id\":\"1234\",\"type\":\"demo\",\"idType\":null,\"fingerExtractionFormat\":\"fingerFormat\",\"irisExtractionFormat\":\"irisFormat\",\"faceExtractionFormat\":\"faceFormat\"}";
		IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
		idDTORequestWrapper.setRequest(request);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));

		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByIdAll() throws Throwable {
		IdResponseDTO response = new IdResponseDTO();
		when(validator.validateUin(anyString())).thenReturn(true);
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);

		String idRequest = "{\"id\":\"1234\",\"type\":\"demo,all\",\"idType\":null,\"fingerExtractionFormat\":\"fingerFormat\",\"irisExtractionFormat\":\"irisFormat\",\"faceExtractionFormat\":\"faceFormat\"}";
		IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
		RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
		idDTORequestWrapper.setRequest(request);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));

		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByIdAllWithExtractionFormatsForVID() throws Throwable {
		when(validator.validateUin(any())).thenReturn(false);
		when(validator.validateVid(any())).thenReturn(true);
		IdResponseDTO response = new IdResponseDTO();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		String idRequest = "{\"id\":\"1234\",\"type\":\"demo,all\",\"idType\":null,\"fingerExtractionFormat\":\"fingerFormat\",\"irisExtractionFormat\":\"irisFormat\",\"faceExtractionFormat\":\"faceFormat\"}";
		IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
		RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
		idDTORequestWrapper.setRequest(request);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	/**
	 * Test retrieve identity.
	 *
	 * @throws IdRepoAppException the id repo app exception
	 */
	@Test
	public void testRetrieveIdentityByIdInvalidUin() throws Throwable {
		try {
			when(idRepoService.retrieveIdentity(any(), any(), any(), any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id")));
			String idRequest = "{\"id\":\"1234\",\"type\":\"demo\",\"idType\":null,\"fingerExtractionFormat\":null,\"irisExtractionFormat\":null,\"faceExtractionFormat\":null}";
			IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
			RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
			idDTORequestWrapper.setRequest(request);
			controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id"),
					e.getErrorText());
		}
	}

	@Test(expected = IdRepoAppException.class)
	public void testRetrieveIdentityByIdRequestParameterMap() throws Throwable {
		when(validator.validateType(any())).thenThrow(new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
				String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "type")));
		String idRequest = "{\"id\":\"1234\",\"type\":\"dem, abc\",\"idType\":\"UIN\",\"fingerExtractionFormat\":null,\"irisExtractionFormat\":null,\"faceExtractionFormat\":null}";
		IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
		RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
		idDTORequestWrapper.setRequest(request);
		controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));
	}

	@Test
	public void testRetrieveIdentityByHandleMultipleValidType() throws Throwable {
		IdResponseDTO response = new IdResponseDTO();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		String idRequest = "{\"id\":\"123456789@phone\",\"type\":\"demo, all,bio\",\"idType\":\"handle\",\"fingerExtractionFormat\":null,\"irisExtractionFormat\":null,\"faceExtractionFormat\":null}";
		IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
		RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
		idDTORequestWrapper.setRequest(request);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByHandleWithSlash() throws Throwable {
		IdResponseDTO response = new IdResponseDTO();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		String idRequest = "{\"id\":\"111111/01/1@nrcid\",\"type\":\"all\",\"idType\":\"handle\",\"fingerExtractionFormat\":null,\"irisExtractionFormat\":null,\"faceExtractionFormat\":null}";
		IdRequestByIdDTO request = mapper.readValue(idRequest, IdRequestByIdDTO.class);
		RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
		idDTORequestWrapper.setRequest(request);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentityById(idDTORequestWrapper, new BeanPropertyBindingResult(request, "IdRequestByIdDTO"));
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}
	/**
	 * Test init binder.
	 */
	@Test
	public void testInitBinder() {
		ReflectionTestUtils.setField(controller, "validator", new IdRequestValidator());
		WebDataBinder binder = new WebDataBinder(new IdRequestDTO());
		controller.initBinder(binder);
	}

	@Test
	public void updateIdentity() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdResponseDTO response = new IdResponseDTO();
		when(validator.validateUin(anyString())).thenReturn(true);
		when(idRepoService.updateIdentity(any(), any())).thenReturn(response);
		RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
		request.setId("mosip.id.update");
		IdRequestDTO requestDTO = new IdRequestDTO();
		Object identity = mapper.readValue(
				"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		requestDTO.setIdentity(identity);
		request.setRequest(requestDTO);
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "IdRequestDTO");
		ResponseEntity<IdResponseDTO<List<String>>> updateIdentity = controller.updateIdentity(request, errors);
		assertEquals(response, updateIdentity.getBody());
		assertEquals(HttpStatus.OK, updateIdentity.getStatusCode());
	}

	@Test
	public void updateIdentityInvalidId() throws Throwable {
		try {
			when(idRepoService.updateIdentity(any(), any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), UIN)));
			RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
			request.setId("mosip.id.update");
			IdRequestDTO requestDTO = new IdRequestDTO();
			Object identity = mapper.readValue(
					"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);
			requestDTO.setIdentity(identity);
			request.setRequest(requestDTO);
			BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "IdRequestDTO");
			controller.updateIdentity(request, errors);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), UIN),
					e.getErrorText());
		}

	}

	@Test
	public void updateIdentityIdRepoDataValidationException() throws Throwable {
		try {
			IdResponseDTO response = new IdResponseDTO();
			when(validator.validateUin(anyString())).thenReturn(true);
			when(idRepoService.updateIdentity(any(), any())).thenReturn(response);
			RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
			request.setId("mosip.id.update");
			IdRequestDTO requestDTO = new IdRequestDTO();
			Object identity = mapper.readValue(
					"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);
			requestDTO.setIdentity(identity);
			request.setRequest(requestDTO);
			BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "IdRequestDTO");
			errors.reject("");
			controller.updateIdentity(request, errors);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATA_VALIDATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATA_VALIDATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test(expected = IdRepoAppException.class)
	public void testUpdateIdentityFailed()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdResponseDTO response = new IdResponseDTO();
		RequestWrapper<IdRequestDTO<List<String>>> request = new RequestWrapper<>();
		request.setId("mosip.id.update");
		IdRequestDTO requestDTO = new IdRequestDTO();
		Object identity = mapper.readValue(
				"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		requestDTO.setIdentity(identity);
		request.setRequest(requestDTO);
		when(idRepoService.updateIdentity(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		ResponseEntity<IdResponseDTO<List<String>>> responseEntity = controller.updateIdentity(request,
				new BeanPropertyBindingResult(request, "IdRequestDTO"));
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByRid() throws IdRepoAppException {
		IdResponseDTO response = new IdResponseDTO();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentity("1234", "demo", "", "RegistrationId",
				null, null);
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByRidAll() throws IdRepoAppException {
		IdResponseDTO response = new IdResponseDTO();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentity("1234", "demo,all", "RegistrationId",
				null, null, null);
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByRidInvalidUin() throws Throwable {
		when(validator.validateUin(null)).thenThrow(new InvalidIDException(null, null));
		try {
			when(idRepoService.retrieveIdentity(any(), any(), any(), any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), UIN)));
			controller.retrieveIdentity("1234", "demo", "RegistrationId", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), UIN),
					e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityByRidMultipleInvalidType() throws Throwable {
		try {
			when(idRepoService.retrieveIdentity(any(), any(), any(), any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE)));
			controller.retrieveIdentity("1234", "dem, abc", "RegistrationId", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE),
					e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityByRidInvalidType() throws Throwable {
		try {
			when(idRepoService.retrieveIdentity(any(), any(), any(), any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE)));
			controller.retrieveIdentity("1234", "dem", "RegistrationId", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE),
					e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityByRidMultipleValidType() throws IdRepoAppException {
		IdResponseDTO response = new IdResponseDTO();
		when(idRepoService.retrieveIdentity(any(), any(), any(), any())).thenReturn(response);
		ResponseEntity<IdResponseDTO<?>> responseEntity = controller.retrieveIdentity("1234", "demo,all,bio",
				"RegistrationId", null, null, null);
		assertEquals(response, responseEntity.getBody());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void testRetrieveIdentityByRidNullId() throws Throwable {
		try {
			controller.retrieveIdentity(null, null, "RegistrationId", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), REGISTRATION_ID),
					e.getErrorText());
		}
	}

	@Test
	public void testGetUin_valid() throws JsonParseException, JsonMappingException, IOException {
		String uin = "6743571690";
		RequestDTO requestDTO = new RequestDTO();
		Object identity = mapper.readValue(
				"{\"UIN\":6743571690,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		requestDTO.setIdentity(identity);
		String uinOutPut = ReflectionTestUtils.invokeMethod(controller, "getUin", requestDTO);
		assertEquals(uin, uinOutPut);
	}

	@Test
	public void testGetUin_missingInputUin() throws Throwable {
		RequestDTO requestDTO = new RequestDTO();
		Object identity;
		try {
			identity = mapper.readValue(
					"{\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"ARA\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);
			requestDTO.setIdentity(identity);
			ReflectionTestUtils.invokeMethod(controller, "getUin", requestDTO);
		} catch (UndeclaredThrowableException e) {
			assertEquals("IDR-IDC-001 --> Missing Input Parameter - identity/UIN", e.getCause().getMessage());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testGetUin_JsonProcessingException() throws Throwable {
		try {
			ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
			when(mockMapper.writeValueAsString(Mockito.any()))
					.thenThrow(new JsonProcessingException(IdRepoErrorConstants.INVALID_REQUEST.getErrorMessage()) {
					});
			ReflectionTestUtils.setField(controller, "mapper", mockMapper);
			ReflectionTestUtils.invokeMethod(controller, "getUin", "");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(cause.getErrorCode(), IdRepoErrorConstants.INVALID_REQUEST.getErrorCode());
			assertEquals(cause.getErrorText(), IdRepoErrorConstants.INVALID_REQUEST.getErrorMessage());
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testGetAuthtypeStatus() throws IdRepoAppException {
		when(validator.validateIdType(anyString())).thenReturn(IdType.UIN);
		when(authTypeStatusService.fetchAuthTypeStatus(any(), any())).thenReturn(List.of(new AuthtypeStatus()));
		ResponseEntity<AuthtypeResponseDto> authTypeStatusResponse = controller.getAuthTypeStatus("", "UIN");
		assertEquals(HttpStatus.OK, authTypeStatusResponse.getStatusCode());
		assertEquals(List.of(new AuthtypeStatus()), authTypeStatusResponse.getBody().getResponse().get("authTypes"));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testGetAuthtypeStatusInvalidIdType() throws IdRepoAppException {
		when(validator.validateIdType(anyString()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER));
		when(authTypeStatusService.fetchAuthTypeStatus(any(), any())).thenReturn(List.of(new AuthtypeStatus()));
		try {
			controller.getAuthTypeStatus("", "UIN");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateAuthtypeStatus() throws IdRepoAppException {
		when(validator.validateIdType(anyString())).thenReturn(IdType.UIN);
		IdResponseDTO response = new IdResponseDTO();
		when(authTypeStatusService.updateAuthTypeStatus(any(), any(), any())).thenReturn(response);
		AuthTypeStatusRequestDto authTypeStatusRequest = new AuthTypeStatusRequestDto();
		authTypeStatusRequest.setIndividualId("");
		authTypeStatusRequest.setIndividualIdType("UIN");
		authTypeStatusRequest.setRequest(List.of(new AuthtypeStatus()));
		ResponseEntity<IdResponseDTO> authTypeStatusResponse = controller.updateAuthtypeStatus(authTypeStatusRequest);
		assertEquals(HttpStatus.OK, authTypeStatusResponse.getStatusCode());
		assertEquals(response, authTypeStatusResponse.getBody());
	}

	@Test
	public void testUpdateAuthtypeStatusInvalidIdType() throws IdRepoAppException {
		when(validator.validateIdType(anyString()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER));
		IdResponseDTO response = new IdResponseDTO();
		when(authTypeStatusService.updateAuthTypeStatus(any(), any(), any())).thenReturn(response);
		AuthTypeStatusRequestDto authTypeStatusRequest = new AuthTypeStatusRequestDto();
		authTypeStatusRequest.setIndividualId("");
		authTypeStatusRequest.setIndividualIdType("UIN");
		authTypeStatusRequest.setRequest(List.of(new AuthtypeStatus()));
		try {
			controller.updateAuthtypeStatus(authTypeStatusRequest);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), e.getErrorText());
		}
	}
	
	@Test
	public void testGetRidByUin() throws IdRepoAppException {
		when(validator.validateIdType(anyString())).thenReturn(IdType.UIN);
		when(idRepoService.getRidByIndividualId(any(), any())).thenReturn("1234");
		ResponseEntity<ResponseWrapper<RidDto>> ridResponse = controller.getRidByIndividualId("", null);
		assertEquals("1234", ridResponse.getBody().getResponse().getRid());
	}
	
	@Test
	public void testGetRidByUinWithIdType() throws IdRepoAppException {
		when(validator.validateIdType(anyString())).thenReturn(IdType.UIN);
		when(idRepoService.getRidByIndividualId(any(), any())).thenReturn("1234");
		ResponseEntity<ResponseWrapper<RidDto>> ridResponse = controller.getRidByIndividualId("", "");
		assertEquals("1234", ridResponse.getBody().getResponse().getRid());
	}
	
	@Test
	public void testGetRemainingUpdateCountByIndividualId() throws IdRepoAppException {
		when(validator.validateIdType(anyString())).thenReturn(IdType.UIN);
		when(idRepoService.getRemainingUpdateCountByIndividualId(any(), any(), any())).thenReturn(Map.of("1234", 1));
		ResponseEntity<ResponseWrapper<AttributeListDto>> response = controller.getRemainingUpdateCountByIndividualId("1234", null, null);
		response.getBody().getResponse();
	}
	
	@Test
	public void testGetRemainingUpdateCountByIndividualIdWithIdType() throws IdRepoAppException {
		when(validator.validateIdType(anyString())).thenReturn(IdType.UIN);
		when(idRepoService.getRemainingUpdateCountByIndividualId(any(), any(), any())).thenReturn(Map.of("1234", 1));
		ResponseEntity<ResponseWrapper<AttributeListDto>> response = controller.getRemainingUpdateCountByIndividualId("1234", "UIN", null);
		response.getBody().getResponse();
	}
}
