package io.mosip.idrepository.core.test.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.idrepository.core.validator.IdRepoValidationMessageHelper;
import io.mosip.kernel.core.util.DateUtils2;
import java.time.LocalDateTime;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * 
 * @author Prem Kumar
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseIdRepoValidatorTest {

	BaseIdRepoValidator requestValidator = new BaseIdRepoValidator() {

	};

	/** The id. */
	private Map<String, String> id;

	public Map<String, String> getId() {
		return id;
	}

	public void setId(Map<String, String> id) {
		this.id = id;
	}

	Errors errors;

	@Before
	public void before() {
		EnvUtil.setVersionPattern("^v\\d+(\\.\\d+)?$");
		EnvUtil.setDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		id = new HashMap<>();
		id.put("read", "mosip.identity.read");
		id.put("deactivate", "mosip.vid.deactivate");
		id.put("reactivate", "mosip.vid.reactivate");
		ReflectionTestUtils.setField(requestValidator, "id", id);
		ReflectionTestUtils.setField(requestValidator, "expectedApplicationVersion", "v1");
		ReflectionTestUtils.setField(requestValidator, "maxRequestTimeDeviationSeconds", 60);
		errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
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
	public void testvalidateReqtimeneg() {
		LocalDateTime pastTime = LocalDateTime.now().minusSeconds(90);
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime", pastTime, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidRequestTimeDeviation(pastTime.toString(), 60)),
					error.getDefaultMessage());
			assertEquals("requesttime", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateReqTimeWithinRange() {
		LocalDateTime now = DateUtils2.getUTCCurrentDateTime().plusSeconds(EnvUtil.getDateTimeAdjustment());
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime", now, errors);
		assertTrue(errors.getAllErrors().isEmpty());
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
	public void testValidateVerExpectedVersionMismatch() {
		String versionPattern = EnvUtil.getVersionPattern();
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVersion", "v2", errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					IdRepoValidationMessageHelper.invalidVersion("v2", versionPattern, "v1")), error.getDefaultMessage());
		});
	}

	@Test
	public void testValidateIdInvalidId() {
		try {
			ReflectionTestUtils.invokeMethod(requestValidator, "validateId", "abc", "deactivate");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(cause.getErrorCode(), IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode());
			assertEquals(cause.getErrorText(),
					String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.invalidId("abc", "deactivate", id)));
		}
	}

	@Test
	public void testValidate_NullId() throws Throwable {
		try {
			ReflectionTestUtils.invokeMethod(requestValidator, "validateId", null, "deactivate");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(cause.getErrorCode(), IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode());
			assertEquals(cause.getErrorText(),
					String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.missingId("deactivate", id)));
		}
	}

	@Test
	public void testValidateVersionValidVersion() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVersion", "v1", errors);
		assertTrue(errors.getAllErrors().isEmpty());
	}

	@Test
	public void testValidateIdValidId() throws Throwable {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateId", "mosip.vid.deactivate", "deactivate");
	}

	@Test
	public void testValidateIdUnconfiguredOperation() {
		try {
			ReflectionTestUtils.invokeMethod(requestValidator, "validateId", "mosip.vid.deactivate", "unknown");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), cause.getErrorCode());
		}
	}

	@Test
	public void testSettersUpdateConfiguration() {
		Map<String, String> newIds = new HashMap<>();
		newIds.put("read", "mosip.identity.read");
		ReflectionTestUtils.invokeMethod(requestValidator, "setOperationIds", newIds);
		ReflectionTestUtils.invokeMethod(requestValidator, "setExpectedApplicationVersion", "v2");
		assertEquals("v2", ReflectionTestUtils.getField(requestValidator, "expectedApplicationVersion"));
	}

	@Test
	public void testValidateIdWhenOperationMapNull() {
		ReflectionTestUtils.setField(requestValidator, "id", null);
		try {
			ReflectionTestUtils.invokeMethod(requestValidator, "validateId", "mosip.vid.deactivate", "deactivate");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), cause.getErrorCode());
		}
	}
}
