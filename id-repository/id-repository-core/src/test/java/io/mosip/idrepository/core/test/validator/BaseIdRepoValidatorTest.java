package io.mosip.idrepository.core.test.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import io.mosip.kernel.core.http.RequestWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.kernel.core.util.DateUtils;
import java.time.LocalDateTime;

/**
 * 
 * @author Prem Kumar
 *
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.identity")
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
		EnvUtil.setVersionPattern("^v\\\\d+(\\\\.\\\\d+)?$");
		ReflectionTestUtils.setField(requestValidator, "id", id);
		ReflectionTestUtils.setField(requestValidator, "maxRequestTimeDeviationSeconds", 60);
		RequestWrapper<IdRequestDTO<Object>> requestWrapper = new RequestWrapper<>();
		errors = new BeanPropertyBindingResult(requestWrapper, "idRequestDto");
	}

	@Test
	public void testValidateReqTimeNullReqTime() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime", null, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "requesttime"),
					error.getDefaultMessage());
			assertEquals("requesttime", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateReqTimeFutureReqTime() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime",
				DateUtils.parseToLocalDateTime("9999-12-31T15:28:28.610Z"), errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "requesttime - the timestamp value can be at most 60 seconds before and after the current time."),
					error.getDefaultMessage());
			assertEquals("requesttime", ((FieldError) error).getField());
		});
	}

	@Test
	public void testvalidateReqtimeneg() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateReqTime", LocalDateTime.now().minusSeconds(90),
				errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(),
					"requesttime - the timestamp value can be at most 60 seconds before and after the current time."),
					error.getDefaultMessage());
			assertEquals("requesttime", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateVerNullVer() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVersion", null, errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "version"),
					error.getDefaultMessage());
			assertEquals("version", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateVerInvalidVer() {
		ReflectionTestUtils.invokeMethod(requestValidator, "validateVersion", "1234.a", errors);
		assertTrue(errors.hasErrors());
		errors.getAllErrors().forEach(error -> {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), error.getCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "version"),
					error.getDefaultMessage());
			assertEquals("version", ((FieldError) error).getField());
		});
	}

	@Test
	public void testValidateIdInvalidId() {
		try {
			ReflectionTestUtils.invokeMethod(requestValidator, "validateId", "abc", "read");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(cause.getErrorCode(), IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode());
			assertEquals(cause.getErrorText(),
					String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id"));
		}
	}

	@Test
	public void testValidate_NullId() throws Throwable {
		try {
			ReflectionTestUtils.invokeMethod(requestValidator, "validateId", null, "read");
		} catch (UndeclaredThrowableException e) {
			IdRepoAppException cause = (IdRepoAppException) e.getCause();
			assertEquals(cause.getErrorCode(), IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode());
			assertEquals(cause.getErrorText(),
					String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "id"));
		}
	}
}
