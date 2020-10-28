package io.mosip.bioextractor.validator;

import static io.mosip.bioextractor.constant.BioExtractorConstants.REQUEST;
import static io.mosip.bioextractor.constant.BiometricExtractionErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.bioextractor.constant.BiometricExtractionErrorConstants.MISSING_INPUT_PARAMETER;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import io.mosip.bioextractor.exception.BiometricExtractionExceptionHandler;
import io.mosip.bioextractor.logger.BioExtractorLogger;
import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

@Component
public class BiometricExtractionRequestValidator implements Validator {
	
	private static final Logger LOGGER = BioExtractorLogger.getLogger(BiometricExtractionExceptionHandler.class);

	private static final String REQUEST_BIOMETRICS = REQUEST + "/biometrics";
	
	@Autowired
	private CbeffUtil cbeffUtil;

	@Override
	public boolean supports(Class<?> clazz) {
		return RequestWrapper.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (Objects.nonNull(target)) {
			RequestWrapper<BioExtractRequestDTO> requestWrapperDto = (RequestWrapper<BioExtractRequestDTO>) target;
			BioExtractRequestDTO request = requestWrapperDto.getRequest();
			validateBiometrics(errors, request);
		}
		
	}

	private void validateBiometrics(Errors errors, BioExtractRequestDTO request) {
		if(StringUtils.isEmpty(request.getBiometrics())) {
			errors.rejectValue(REQUEST,
					MISSING_INPUT_PARAMETER.getErrorCode(),
					new String[] {REQUEST_BIOMETRICS},
					MISSING_INPUT_PARAMETER.getErrorMessage());
		} else if(!validateCbeff(request.getBiometrics())) {
			errors.rejectValue("request",
					INVALID_INPUT_PARAMETER.getErrorCode(),
					new String[] {REQUEST_BIOMETRICS + " : " + request.getBiometrics()},
					INVALID_INPUT_PARAMETER.getErrorMessage()); 
		}
	}

	private boolean validateCbeff(String biometrics) {
		try {
			return cbeffUtil.validateXML(CryptoUtil.decodeBase64(biometrics));
		} catch (Exception e) {
			LOGGER.error("", this.getClass().getSimpleName(), "validateCbeff",
					e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
			return false;
		}
	}

}