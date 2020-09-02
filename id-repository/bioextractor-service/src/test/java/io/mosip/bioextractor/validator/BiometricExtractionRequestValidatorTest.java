package io.mosip.bioextractor.validator;

import static io.mosip.bioextractor.constant.BiometricExtractionErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.bioextractor.constant.BiometricExtractionErrorConstants.MISSING_INPUT_PARAMETER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.util.CryptoUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class BiometricExtractionRequestValidatorTest {
	
	@Mock
	private CbeffUtil cbeffUtil;
	
	@InjectMocks
	private BiometricExtractionRequestValidator validator;

	@Test
	public void testValidate_nullTarget() throws Exception {
		Object target = null;
		Errors errors = null;

		// default test
		validator.validate(target, errors);
	}
	
	@Test
	public void testValidate() throws Exception {
		BioExtractRequestDTO bioExtractRequestDTO = new BioExtractRequestDTO();
		String biometrics = CryptoUtil.encodeBase64String("test".getBytes());
		bioExtractRequestDTO.setBiometrics(biometrics);
		
		RequestWrapper<BioExtractRequestDTO> target = new RequestWrapper<>();
		target.setRequest(bioExtractRequestDTO);
		
		Errors errors = new BindException(target, "bioExtractRequestDTO");
		
		Mockito.when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);

		// default test
		validator.validate(target, errors);
		
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void testValidate_EmptyBiometrics() throws Exception {
		BioExtractRequestDTO bioExtractRequestDTO = new BioExtractRequestDTO();
		String biometrics = "";
		bioExtractRequestDTO.setBiometrics(biometrics);
		
		RequestWrapper<BioExtractRequestDTO> target = new RequestWrapper<>();
		target.setRequest(bioExtractRequestDTO);
		
		Errors errors = new BindException(target, "bioExtractRequestDTO");

		// default test
		validator.validate(target, errors);
		
		assertEquals(1, errors.getErrorCount());
		
		assertEquals(MISSING_INPUT_PARAMETER.getErrorCode(), errors.getAllErrors().get(0).getCode());
	}
	
	@Test
	public void testValidate_InvalidCbeff() throws Exception {
		BioExtractRequestDTO bioExtractRequestDTO = new BioExtractRequestDTO();
		String biometrics = CryptoUtil.encodeBase64String("test".getBytes());;
		bioExtractRequestDTO.setBiometrics(biometrics);
		
		RequestWrapper<BioExtractRequestDTO> target = new RequestWrapper<>();
		target.setRequest(bioExtractRequestDTO);
		
		Errors errors = new BindException(target, "bioExtractRequestDTO");
		
		Mockito.when(cbeffUtil.validateXML(Mockito.any())).thenReturn(false);
		
		// default test
		validator.validate(target, errors);
		
		assertEquals(1, errors.getErrorCount());
		
		assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), errors.getAllErrors().get(0).getCode());
	}
	
	@Test
	public void testValidate_InvalidCbeff_withException() throws Exception {
		BioExtractRequestDTO bioExtractRequestDTO = new BioExtractRequestDTO();
		String biometrics = CryptoUtil.encodeBase64String("test".getBytes());;
		bioExtractRequestDTO.setBiometrics(biometrics);
		
		RequestWrapper<BioExtractRequestDTO> target = new RequestWrapper<>();
		target.setRequest(bioExtractRequestDTO);
		
		Errors errors = new BindException(target, "bioExtractRequestDTO");
		
		Mockito.when(cbeffUtil.validateXML(Mockito.any())).thenThrow(new Exception("Invalid Cbeff"));
		
		// default test
		validator.validate(target, errors);
		
		assertEquals(1, errors.getErrorCount());
		
		assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), errors.getAllErrors().get(0).getCode());
	}


	@Test
	public void testSupports() throws Exception {
		Class<?> clazz = null;
		boolean result;

		// default test
		result = validator.supports(clazz);
	}
}