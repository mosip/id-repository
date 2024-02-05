package io.mosip.idrepository.identity.test.validator;

import static io.mosip.idrepository.core.constant.IdRepoConstants.FACE_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FINGER_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IRIS_EXTRACTION_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorErrorConstant;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.exception.InvalidIdSchemaException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.idvalidator.uin.impl.UinValidatorImpl;

/**
 * @author Manoj SP
 *
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.identity")
public class IdRequestValidatorTest {

	private static final String UIN = "uin";

	@InjectMocks
	IdRequestValidator validator;
	
	@Mock
	private HttpServletRequest servletRequest;

	private Map<String, String> id;

	List<String> uinStatus;

	List<String> allowedTypes;

	public List<String> getAllowedTypes() {
		return allowedTypes;
	}

	public void setAllowedTypes(List<String> allowedTypes) {
		this.allowedTypes = allowedTypes;
	}

	@Autowired
	ObjectMapper mapper;

	@Mock
	private UinValidatorImpl uinValidator;
	
	@Mock
	private VidValidator<String> vidValidator;

	@Mock
	private IdObjectValidator idObjectValidator;

	@Mock
	private RestRequestBuilder restBuilder;

	@Mock
	private RestHelper restHelper;

	@InjectMocks
	private IdRepoServiceHelper idRepoServiceHelper;

	public Map<String, String> getId() {
		return id;
	}

	public void setId(Map<String, String> id) {
		this.id = id;
	}

	public void setUinStatus(List<String> uinStatus) {
		this.uinStatus = uinStatus;
	}

	Errors errors;

	@Before
	public void setup() throws IdRepoAppException {
		when(servletRequest.getRequestURI()).thenReturn("");
		uinStatus.add(EnvUtil.getUinActiveStatus());
		allowedTypes.add("bio,demo,all");
		ReflectionTestUtils.setField(validator, "id", id);
		ReflectionTestUtils.setField(validator, "uinStatus", uinStatus);
		ReflectionTestUtils.setField(validator, "allowedTypes", allowedTypes);
		ReflectionTestUtils.setField(validator, "idRepoServiceHelper", idRepoServiceHelper);
		ReflectionTestUtils.setField(idRepoServiceHelper, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceHelper, "restBuilder", restBuilder);
		ReflectionTestUtils.setField(idRepoServiceHelper, "restHelper", restHelper);

		IdentityMapping identityMapping = new IdentityMapping();
		identityMapping.setIdentity(new IdentityMapping.Identity());
		IdentityMapping.IDSchemaVersion idSchemaVersion = new IdentityMapping.IDSchemaVersion();
		idSchemaVersion.setValue("IDSchemaVersion");
		IdentityMapping.SelectedHandles selectedHandles = new IdentityMapping.SelectedHandles();
		selectedHandles.setValue("selectedHandles");
		identityMapping.getIdentity().setIDSchemaVersion(idSchemaVersion);
		identityMapping.getIdentity().setSelectedHandles(selectedHandles);
		ReflectionTestUtils.setField(idRepoServiceHelper, "identityMapping", identityMapping);

		errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<Map<String, String>> response = new ResponseWrapper<>();
		Map<String, String> responseMap = new HashMap<String, String>();
		responseMap.put("schemaJson", "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"description\":\"MOISP Test Identity\",\"additionalProperties\":false,\"title\":\"MOISP identity\",\"type\":\"object\",\"definitions\":{\"simpleType\":{\"uniqueItems\":true,\"additionalItems\":false,\"type\":\"array\",\"items\":{\"additionalProperties\":false,\"type\":\"object\",\"required\":[\"language\",\"value\"],\"properties\":{\"language\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}}},\"documentType\":{\"additionalProperties\":false,\"type\":\"object\",\"properties\":{\"format\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}},\"biometricsType\":{\"additionalProperties\":false,\"type\":\"object\",\"properties\":{\"format\":{\"type\":\"string\"},\"version\":{\"type\":\"number\",\"minimum\":0},\"value\":{\"type\":\"string\"}}}},\"properties\":{\"identity\":{\"additionalProperties\":false,\"type\":\"object\",\"required\":[\"IDSchemaVersion\",\"fullName\"],\"properties\":{\"UIN\":{\"bioAttributes\":[],\"fieldCategory\":\"none\",\"format\":\"none\",\"type\":\"string\",\"fieldType\":\"default\"},\"IDSchemaVersion\":{\"bioAttributes\":[],\"fieldCategory\":\"none\",\"format\":\"none\",\"type\":\"number\",\"fieldType\":\"default\",\"minimum\":0},\"fullName\":{\"bioAttributes\":[],\"validators\":[{\"validator\":\"^(?=.{3,50}$).\",\"arguments\":[],\"type\":\"regex\"}],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"$ref\":\"#/definitions/simpleType\"},\"phone\":{\"bioAttributes\":[],\"validators\":[{\"validator\":\"^[+]855([0-9]{8,9})$\",\"arguments\":[],\"type\":\"regex\"}],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"type\":\"string\",\"fieldType\":\"default\",\"requiredOn\":\"\",\"handle\":true}}}}}");
		response.setResponse(responseMap);
		when(restHelper.requestSync(Mockito.any())).thenReturn(response);
	}

	@Test
	public void testSupport() {
		assertTrue(validator.supports(IdRequestDTO.class));
	}

	@Test
	public void testValidateRequestJsonAttributes() throws JsonParseException, JsonMappingException, IOException,
			IdObjectValidationFailedException, IdObjectIOException, InvalidIdSchemaException, IdRepoAppException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new IdObjectValidationFailedException("", "Invalid - Request"));
		Object request = mapper.readValue(
				"{\"identity\":{\"IDSchemaVersion\":0,\"dateOfBirth\":\"12345\",\"fullName\":[{\"language\":\"\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testValidateStatusInvalidStatus() {
		ReflectionTestUtils.invokeMethod(validator, "validateStatus", "1234", errors, "update");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "status"),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRegIdNullRegId() {
		ReflectionTestUtils.invokeMethod(validator, "validateRegId", null, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(
					String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "registrationId"),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequestInvalidSchema() throws JsonParseException, JsonMappingException, IOException,
			IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException, IdRepoAppException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new IdObjectIOException("errorCode", "errorMessage"));
		Object request = mapper.readValue(
				"{\"identity\":{\"IDSchemaVersion\":0,\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED.getErrorCode(), error.getCode());
			assertEquals(IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED.getErrorMessage(), error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequestWithDocuments() throws JsonParseException, JsonMappingException, IOException,
			IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException, IdRepoAppException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		Object request = mapper.readValue(
				"{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":795429385028},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					"documents/0/category"), error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequestWithDocumentsEmptyDocValue() throws JsonParseException, JsonMappingException,
			IOException, IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException, IdRepoAppException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		Object request = mapper.readValue(
				"{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":795429385028,\"individualBiometrics\": {\"format\": \"cbeff\",\"version\": 1,\"value\": \"individualBiometrics_bio_CBEFF\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"\"}]}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					"documents/0/value"), error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequestWithDocumentsInvalidIdentityJsonValidator()
			throws JsonParseException, JsonMappingException, IOException, IdObjectValidationFailedException,
			IdObjectIOException, InvalidIdSchemaException, IdRepoAppException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		Object request = mapper.readValue(
				"{\"identity\":{},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testValidateRequestWithEmptyIdentity() throws JsonParseException, JsonMappingException, IOException, IdRepoAppException {
		Object request = mapper.readValue("{\"identity\":{}}".getBytes(), Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "update");
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testValidateRequestWithNullRequest() throws JsonParseException, JsonMappingException, IOException {
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", null, errors, "create");
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testValidateRequestWithDocumentsInvalidIdentity()
			throws JsonParseException, JsonMappingException, IOException, IdRepoAppException {
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		when(mockMapper.writeValueAsBytes(Mockito.any()))
				.thenThrow(new UnrecognizedPropertyException(null, "", null, null, "", null));
		ReflectionTestUtils.setField(idRepoServiceHelper, "mapper", mockMapper);
		Object request = mapper.readValue(
				"{\"identity\":{},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}"
						.getBytes(),
				Map.class);
		ReflectionTestUtils.invokeMethod(validator, "validateDocuments", request, errors);
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testValidateRequestWithDocumentsDuplicateDoc()
			throws JsonParseException, JsonMappingException, IOException {
		ReflectionTestUtils.invokeMethod(validator, "validateDocuments", mapper.readValue(
				"{\"identity\":{\"individualBiometrics\": {\"format\": \"cbeff\", \"version\": 1.0,\"fileReference\": \"le monde est grand et petit\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}, {\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}"
						.getBytes(),
				Map.class), errors);
		assertTrue(errors.hasErrors());
	}

	@Test(expected = IdRepoAppException.class)
	public void testconvertToMap() throws Throwable {
		try {
			ReflectionTestUtils.invokeMethod(idRepoServiceHelper, "convertToMap", "1234");
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testValidateRequestConfigServerConnectionException() throws JsonParseException, JsonMappingException,
			IOException, IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new IdObjectIOException("errorCode", "errorMessage"));
		Object request = mapper.readValue(
				"{\"identity\":{\"IDSchemaVersion\":0,\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED.getErrorCode(), error.getCode());
			assertEquals(IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED.getErrorMessage(), error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequestUnidentifiedJsonException() throws JsonParseException, JsonMappingException,
			IOException, IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new IdObjectValidationFailedException(
						IdObjectValidatorErrorConstant.INVALID_INPUT_PARAMETER.getErrorCode(), "error - Message"));
		Object request = mapper.readValue(
				"{\"identity\":{\"IDSchemaVersion\":0,\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "Message"),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequestWithoutIdentity() throws JsonParseException, JsonMappingException, IOException,
			IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException {
		when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new IdObjectIOException("errorCode", "errorMessage"));
		Object request = mapper.readValue(
				"{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}".getBytes(),
				Object.class);
		ReflectionTestUtils.invokeMethod(validator, "validateRequest", request, errors, "create");
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "identity"),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateCreate() throws JsonParseException, JsonMappingException, JsonProcessingException,
			IOException, IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException, IdRepoAppException {
		ReflectionTestUtils.setField(validator, "maxRequestTimeDeviationSeconds", 60);
		Mockito.when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);

		IdRequestDTO request = new IdRequestDTO();
		request.setId("mosip.id.create");
		request.setVersion("v1");
		Object obj = mapper.readValue(
				"{\"IDSchemaVersion\":0,\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}"
						.getBytes(),
				Object.class);
		RequestDTO req = new RequestDTO();
		req.setRegistrationId("1234");
		req.setStatus("ACTIVATED");
		req.setIdentity(obj);
		request.setRequest(req);
		request.setRequesttime(DateUtils.getUTCCurrentDateTime());
		validator.validate(request, errors);
		assertFalse(errors.hasErrors());
	}

	@Test
	public void testValidateUpdate() throws JsonParseException, JsonMappingException, JsonProcessingException,
			IOException, IdObjectIOException, IdObjectValidationFailedException, InvalidIdSchemaException {
		ReflectionTestUtils.setField(validator, "maxRequestTimeDeviationSeconds", 60);
		Mockito.when(idObjectValidator.validateIdObject(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);
		IdRequestDTO request = new IdRequestDTO();
		request.setId("mosip.id.update");
		request.setVersion("v1");
		Object obj = mapper.readValue(
				"{\"IDSchemaVersion\":0,\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}"
						.getBytes(),
				Object.class);

		RequestDTO req = new RequestDTO();
		req.setRegistrationId("1234");
		req.setStatus("BLOCKED");
		req.setIdentity(obj);
		request.setRequest(req);
		request.setRequesttime(DateUtils.getUTCCurrentDateTime());
		validator.validate(request, errors);
		assertFalse(errors.hasErrors());
	}

	@Test
	public void testInvalidUin() throws IdRepoAppException {
		try {
			when(uinValidator.validateId(anyString()))
					.thenThrow(new InvalidIDException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), UIN)));
			validator.validateUin("1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), UIN),
					e.getErrorText());
		}
	}

	/**
	 * Test retrieve identity null id.
	 *
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	@Test
	public void testValidateNullId() throws IdRepoAppException {
		try {
			when(uinValidator.validateId(null)).thenThrow(new InvalidIDException(null, null));
			validator.validateUin(null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(
					String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
							EnvUtil.getUinJsonPath()).replace(".", "/"),
					e.getErrorText());
		}
	}

	@Test
	public void testValidateVid() {
		when(vidValidator.validateId(Mockito.anyString())).thenReturn(true);
		boolean flag = validator.validateVid("cvb");
		assertTrue(flag);
	}
	
	@Test
	public void testValidateVidwithException() {
		when(vidValidator.validateId(Mockito.anyString())).thenThrow(InvalidIDException.class);
		boolean flag = validator.validateVid("cvb");
		assertFalse(flag);		
	}
	
	@Test
	public void testValidateUin() throws IdRepoAppException {
		when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);
		boolean flag = validator.validateUin("vcbhg");
		assertTrue(flag);
	}
	
	@Test(expected = IdRepoAppUncheckedException.class)
	public void testGetSchemawithIdRepoAppUncheckedException() {
		String response = ReflectionTestUtils.invokeMethod(idRepoServiceHelper, "getSchema", "null");
		assertNotNull(response);
	}
	
	@Test
	public void testGetSchema() throws IdRepoDataValidationException {
		try{
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(IdRepoDataValidationException.class);
			//String response = ReflectionTestUtils.invokeMethod(validator, "getSchema", "cvhgfvbn");
			}
		catch(IdRepoAppUncheckedException e) {
			assertEquals(IdRepoErrorConstants.SCHEMA_RETRIEVE_ERROR.getErrorCode(), e.getErrorCode());	
		}
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testValidateIdvId() throws IdRepoAppException {
		IdType id = IdType.VID;
		when(vidValidator.validateId(Mockito.anyString())).thenThrow(InvalidIDException.class);
		validator.validateIdvId("edcvbj", id);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testValidateTypeAndExtractionFormatswithTypeNull() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		validator.validateTypeAndExtractionFormats(null,extractionFormats);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testValidateTypeAndExtractionFormats() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		validator.validateTypeAndExtractionFormats("cvbhgfc",extractionFormats);
	}
	
	@Test
	public void testValidateIdTypewithIllegalArgumentException() throws IdRepoAppException {
		try{
			IdType id = validator.validateIdType("jhgcvb");
		}
		catch(IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
		}
	}
	
	@Test
	public void testValidateIdTypeWithTypeNull() throws IdRepoAppException {
		IdType id = validator.validateIdType(null);
		assertNull(id);
	}
	
	@Test
	public void testValidateIdType() throws IdRepoAppException {
		IdType id = validator.validateIdType("VID");
		assertNotNull(id);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testValidateTypeWithIdRepoAppException() throws IdRepoAppException {
		allowedTypes = List.of("bio", "demo", "all");	
		ReflectionTestUtils.setField(validator, "allowedTypes", allowedTypes);
		String response = validator.validateType("metadata");
		assertNotNull(response);
	}
	
	@Test
	public void testValidateTypeWithTypeAll() throws IdRepoAppException {
		allowedTypes = List.of("bio", "demo", "metadata", "all");	
		ReflectionTestUtils.setField(validator, "allowedTypes", allowedTypes);
		String response = validator.validateType("bio,demo,metadata,all");
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testValidateType() throws IdRepoAppException {
		allowedTypes = List.of("bio", "demo", "all");		
		ReflectionTestUtils.setField(validator, "allowedTypes", allowedTypes);
		String response = validator.validateType("metadata,bio");
		assertNotNull(response);
	}
	
}
