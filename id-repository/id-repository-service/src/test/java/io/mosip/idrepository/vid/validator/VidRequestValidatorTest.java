package io.mosip.idrepository.vid.validator;

import io.mosip.kernel.core.util.DateUtils2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.VidRequestDTO;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.test.support.TestEnvSupport;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.idrepository.core.validator.IdRepoValidationMessageHelper;
import io.mosip.idrepository.vid.provider.VidPolicyProvider;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * 
 * @author Prem Kumar
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class VidRequestValidatorTest {

	@InjectMocks
	private VidRequestValidator requestValidator;

	@Mock
	private VidValidator<String> vidValidator;
	
	@Mock
	private BaseIdRepoValidator  baseValidator;

	@Mock
	private UinValidator<String> uinValidator;

	@Mock
	private VidPolicyProvider policyProvider;

	List<String> allowedStatus;

	Map<String, String> id;

	Errors errors;

	@Before
	public void before() {
		TestEnvSupport.initEnvUtil(TestEnvSupport.loadTestEnvironment());
		EnvUtil.setVersionPattern("^v\\d+(\\.\\d+)?$");
		EnvUtil.setDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		id = new HashMap<>();
		id.put("create", "mosip.vid.create");
		id.put("update", "mosip.vid.update");
		id.put("deactivate", "mosip.vid.deactivate");
		id.put("reactivate", "mosip.vid.reactivate");
		id.put("regenerate", "mosip.vid.regenerate");
		allowedStatus = Arrays.asList("ACTIVE", "REVOKED", "EXPIRED", "USED", "INVALIDATED", "DEACTIVATED");
		errors = new BeanPropertyBindingResult(new RequestWrapper<VidRequestDTO>(), "vidRequestDto");
		ReflectionTestUtils.setField(requestValidator, "allowedStatus", allowedStatus);
		ReflectionTestUtils.setField(requestValidator, "id", id);
		ReflectionTestUtils.setField(baseValidator, "id", id);
		ReflectionTestUtils.setField(requestValidator, "vidValidator", vidValidator);
		ReflectionTestUtils.setField(requestValidator, "policyProvider", policyProvider);
		ReflectionTestUtils.setField(requestValidator, "uinValidator", uinValidator);
		ReflectionTestUtils.setField(requestValidator, "expectedApplicationVersion", "v1");
		ReflectionTestUtils.setField(requestValidator, "maxRequestTimeDeviationSeconds", 60);
	}

	@Test
	public void testSupport() {
		assertTrue(requestValidator.supports(RequestWrapper.class));
	}

	@Test
	public void testSupport_Invalid() {
		assertFalse(requestValidator.supports(IdRequestDTO.class));
	}

	@Test
	public void testValidateReqTimeNullReqTime() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime", null, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.missingRequestTime()), error.getDefaultMessage());
			assertEquals("requesttime", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateReqTimeFutureReqTime() {
		LocalDateTime futureTime = DateUtils2.parseToLocalDateTime("9999-12-31T15:28:28.610Z");
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime", futureTime, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidRequestTimeDeviation(futureTime.toString(), 60)),
					error.getDefaultMessage());
			assertEquals("requesttime", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateVerNullVer() {
		String versionPattern = EnvUtil.getVersionPattern();
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVersion", null, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.missingVersion(versionPattern, "v1")), error.getDefaultMessage());
			assertEquals("version", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateVerInvalidVer() {
		String versionPattern = EnvUtil.getVersionPattern();
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVersion", "1234.a", errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidVersion("1234.a", versionPattern, "v1")), error.getDefaultMessage());
			assertEquals("version", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateStatus_Invalid_Status() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateStatus", "ACTIVAT", errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidWithAllowed("vidStatus", "ACTIVAT", allowedStatus)),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateStatus_Null_Status() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateStatus", null, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.missingWithAllowed("vidStatus", allowedStatus)),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequest() {
		RequestWrapper<VidRequestDTO> req = new RequestWrapper<VidRequestDTO>();
		req.setId("mosip.vid.update");
		VidRequestDTO request = new VidRequestDTO();
		request.setVidStatus("ACTIVE");
		req.setVersion("v1");
		req.setRequesttime(DateUtils2.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone())).toLocalDateTime());
		req.setRequest(request);
		ReflectionTestUtils.invokeMethod(requestValidator, "validate", req, errors);
		assertFalse(errors.hasErrors());
	}

	@Test
	public void testValidateRequest_NullRequest() {
		RequestWrapper<VidRequestDTO> req = new RequestWrapper<VidRequestDTO>();
		req.setId("mosip.vid.update");
		req.setRequest(null);
		req.setVersion("v1");
		req.setRequesttime(DateUtils2.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone())).toLocalDateTime());
		ReflectionTestUtils.invokeMethod(requestValidator, "validate", req, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.missingField("request")),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateVid_Valid() {
		Mockito.when(vidValidator.validateId(Mockito.anyString())).thenReturn(true);
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVid", "2015642902372692");
	}

	@Test
	public void testValidateRequest_validateVidType_Valid() {
		RequestWrapper<VidRequestDTO> req = new RequestWrapper<VidRequestDTO>();
		req.setId("mosip.vid.create");
		VidRequestDTO request = new VidRequestDTO();
		request.setVidStatus("ACTIVE");
		request.setVidType("Perpetual");
		request.setUin("2953190571");
		req.setVersion("v1");
		req.setRequesttime(DateUtils2.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone())).toLocalDateTime());
		req.setRequest(request);
		HashSet<String> value = new HashSet<String>();
		value.add("Perpetual".toUpperCase());
		value.add("Temporary".toUpperCase());
		Mockito.when(policyProvider.getAllVidTypes()).thenReturn(value);
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);
		ReflectionTestUtils.invokeMethod(requestValidator, "validate", req, errors);
		assertFalse(errors.hasErrors());
	}

	@Test
	public void testValidateRequest_validateVidType_InValid() {
		RequestWrapper<VidRequestDTO> req = new RequestWrapper<VidRequestDTO>();
		req.setId("mosip.vid.create");
		VidRequestDTO request = new VidRequestDTO();
		request.setVidStatus("ACTIVE");
		request.setUin("2953190571");
		request.setVidType("Temp");
		req.setVersion("v1");
		req.setRequesttime(DateUtils2.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone())).toLocalDateTime());
		req.setRequest(request);
		HashSet<String> value = new HashSet<String>();
		value.add("Perpetual");
		value.add("Temporary");
		Mockito.when(policyProvider.getAllVidTypes()).thenReturn(value);
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);
		ReflectionTestUtils.invokeMethod(requestValidator, "validate", req, errors);
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidWithAllowed("vidType", "Temp", value)),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateRequest_validateVidType_Null() {
		RequestWrapper<VidRequestDTO> req = new RequestWrapper<VidRequestDTO>();
		req.setId("mosip.vid.create");
		VidRequestDTO request = new VidRequestDTO();
		request.setVidStatus("ACTIVE");
		request.setUin("2953190571");
		request.setVidType(null);
		req.setVersion("v1");
		req.setRequesttime(DateUtils2.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone())).toLocalDateTime());
		req.setRequest(request);
		HashSet<String> value = new HashSet<String>();
		value.add("Perpetual");
		value.add("Temporary");
		Mockito.when(policyProvider.getAllVidTypes()).thenReturn(value);
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);
		ReflectionTestUtils.invokeMethod(requestValidator, "validate", req, errors);
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.missingWithAllowed("vidType", value)),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}
	
	
	
	@Test
	public void testUinValid() {
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenReturn(true);
		ReflectionTestUtils.invokeMethod(requestValidator, "validateUin", "123456", errors);
	}
	
	@Test
	public void testUinInValid() {
		Mockito.when(uinValidator.validateId(Mockito.anyString())).thenThrow(new InvalidIDException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
				String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "UIN")));
		ReflectionTestUtils.invokeMethod(requestValidator, "validateUin", "123456", errors);
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidUin()),
					error.getDefaultMessage());
			assertEquals("request", ((FieldError) error).getField());
		});
	}
}
