package io.mosip.bioextractor.exception;

import static io.mosip.bioextractor.constant.BiometricExtractionErrorConstants.INVALID_CBEFF;
import static io.mosip.bioextractor.constant.BiometricExtractionErrorConstants.INVALID_INPUT_PARAMETER;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequest;

import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class BiometricExtractionExceptionHandlerTest {

	@InjectMocks
	BiometricExtractionExceptionHandler biometricExtractionExceptionHandler;

	@Test
	public void testGetErrors() throws Exception {
		Exception ex = null;
		List<ServiceError> result;

		// default test 1
		result = BiometricExtractionExceptionHandler.getErrors(ex);
		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testHandleAllExceptions() throws Exception {
		BiometricExtractionExceptionHandler testSubject;
		Exception ex = new Exception("test");
		WebRequest request = null;
		ResponseEntity<Object> result;

		// default test 1
		result = biometricExtractionExceptionHandler.handleAllExceptions(ex, request);
	}

	@Test
	public void testHandleBiometricExtractionExceptions() throws Exception {
		BiometricExtractionExceptionHandler testSubject;
		BiometricExtractionException ex = new BiometricExtractionException(INVALID_INPUT_PARAMETER);
		WebRequest request = null;
		ResponseEntity<Object> result;

		// default test 1
		result = biometricExtractionExceptionHandler.handleBiometricExtractionExceptions(ex,request);
	}

	@Test
	public void testHandleAllExceptions_1() throws Exception {
		BiometricExtractionExceptionHandler testSubject;
		Exception ex = new Exception("test");
		WebRequest request = null;
		ResponseEntity<Object> result;

		// default test 1
		result = biometricExtractionExceptionHandler.handleAllExceptions(ex, request);
	}

	@Test
	public void testHandleBiometricExtractionExceptions_1() throws Exception {
		BiometricExtractionExceptionHandler testSubject;
		BiometricExtractionException ex = new BiometricExtractionException(INVALID_INPUT_PARAMETER);
		WebRequest request = null;
		ResponseEntity<Object> result;

		// default test 1
		result = biometricExtractionExceptionHandler.handleBiometricExtractionExceptions(ex,request);
	}

	@Test
	public void testGetErrors_1() throws Exception {
		Exception ex = new DataValidationException(INVALID_INPUT_PARAMETER, new String[] {"Test"});
		List<ServiceError> result;

		// default test 1
		result = BiometricExtractionExceptionHandler.getErrors(ex);
		Assert.assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), result.get(0).getErrorCode());
	}
	
	@Test
	public void testGetErrors_2_nullArgs() throws Exception {
		Exception ex = new DataValidationException(INVALID_CBEFF);
		List<ServiceError> result;

		// default test 1
		result = BiometricExtractionExceptionHandler.getErrors(ex);
		Assert.assertEquals(INVALID_CBEFF.getErrorCode(), result.get(0).getErrorCode());
	}
	
	@Test
	public void testGetErrors_2_EmptyArgs() throws Exception {
		Exception ex = new DataValidationException(INVALID_CBEFF, new String[0]);
		List<ServiceError> result;

		// default test 1
		result = BiometricExtractionExceptionHandler.getErrors(ex);
		Assert.assertEquals(INVALID_CBEFF.getErrorCode(), result.get(0).getErrorCode());
	}
}