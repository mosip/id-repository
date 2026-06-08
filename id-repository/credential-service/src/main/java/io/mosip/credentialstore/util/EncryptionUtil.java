package io.mosip.credentialstore.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.CryptoWithPinRequestDto;
import io.mosip.credentialstore.dto.CryptoWithPinResponseDto;
import io.mosip.credentialstore.dto.CryptoZkResponseDto;
import io.mosip.credentialstore.dto.CryptomanagerRequestDto;
import io.mosip.credentialstore.dto.CryptomanagerResponseDto;
import io.mosip.credentialstore.dto.EncryptZkRequestDto;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;

@Component
public class EncryptionUtil {
	
	private static final Logger LOGGER = IdRepoLogger.getLogger(EncryptionUtil.class); 

	/** The Constant IO_EXCEPTION. */
	private static final String IO_EXCEPTION = "Exception while reading packet inputStream";

	/** The Constant DATE_TIME_EXCEPTION. */
	private static final String DATE_TIME_EXCEPTION = "Error while parsing packet timestamp";


	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	/** The application id. */
	@Value("${credential.service.application.id:PARTNER}")
	private String applicationId;

	/**
	 * Reusable method to build RequestWrapper (reduces object creation boilerplate)
	 */
	private <T> RequestWrapper<T> createRequestWrapper(T requestBody) {
		RequestWrapper<T> request = new RequestWrapper<>();
		String pattern = EnvUtil.getDateTimePattern();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		LocalDateTime now = LocalDateTime.parse(
				DateUtils2.getUTCCurrentDateTimeString(pattern),
				formatter);

		request.setRequesttime(now);
		request.setRequest(requestBody);
		return request;
	}

	public String encryptDataWithPin(String attributeName, String data, String pin, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started encrypting data using pin");

		try {
			CryptoWithPinRequestDto cryptoReq = new CryptoWithPinRequestDto();
			cryptoReq.setData(data);
			cryptoReq.setUserPin(pin);

			RequestWrapper<CryptoWithPinRequestDto> request = createRequestWrapper(cryptoReq);

			String responseStr = restUtil.postApi(ApiName.KEYMANAGER_ENCRYPT_PIN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptoWithPinResponseDto response = mapper.readValue(responseStr, CryptoWithPinResponseDto.class);

			if (hasErrors(response.getErrors())) {
				ServiceError error = response.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}

			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Pin-based encryption successful");
			return response.getResponse().getData();
		} catch (Exception e) {
			handleEncryptionException(e, "pin", attributeName, requestId);
			throw new DataEncryptionFailureException(e); // fallback
		}
	}

	public EncryptZkResponseDto encryptDataWithZK(String id, List<ZkDataAttribute> zkDataAttributes, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started encrypting data using ZK encryption");

		try {

			EncryptZkRequestDto zkReq = new EncryptZkRequestDto();
			zkReq.setId(id);
			zkReq.setZkDataAttributes(zkDataAttributes);

			RequestWrapper<EncryptZkRequestDto> request = createRequestWrapper(zkReq);

			String responseStr = restUtil.postApi(ApiName.KEYMANAGER_ENCRYPT_ZK, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptoZkResponseDto response = mapper.readValue(responseStr, CryptoZkResponseDto.class);

			if (hasErrors(response.getErrors())) {
				throw new DataEncryptionFailureException(response.getErrors().get(0).getMessage());
			}

			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ZK encryption successful");
			return response.getResponse();

		} catch (Exception e) {
			handleEncryptionException(e, "ZK", null, requestId);
			throw new DataEncryptionFailureException(e);
		}
	}

	@Retryable(value = { DataEncryptionFailureException.class,
			ApiNotAccessibleException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public String encryptData(String dataToBeEncrypted, String partnerId, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started encrypting data using partner certificate");
	

		try {

			CryptomanagerRequestDto cryptoReq = new CryptomanagerRequestDto();
			cryptoReq.setApplicationId(applicationId);
			cryptoReq.setData(dataToBeEncrypted);
			cryptoReq.setReferenceId(partnerId);
			cryptoReq.setPrependThumbprint(EnvUtil.getPrependThumbprintStatus());

			LocalDateTime now = LocalDateTime.parse(
					DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()),
					DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern()));
			cryptoReq.setTimeStamp(now);

			RequestWrapper<CryptomanagerRequestDto> request = createRequestWrapper(cryptoReq);

			String responseStr = restUtil.postApi(ApiName.CRYPTOMANAGER_ENCRYPT, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptomanagerResponseDto response = mapper.readValue(responseStr, CryptomanagerResponseDto.class);

			if (hasErrors(response.getErrors())) {
				throw new DataEncryptionFailureException(response.getErrors().get(0).getMessage());
			}

			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Partner encryption successful");
			return response.getResponse().getData();

		} catch (Exception e) {
			handleEncryptionException(e, "partner", null, requestId);
			return null;
		}
	}

	private boolean hasErrors(List<ServiceError> errors) {
		return errors != null && !errors.isEmpty();
	}

	private void handleEncryptionException(Exception e, String encryptionType,
										   String attributeName, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {

		String logMessage = encryptionType + " encryption failed";

		if (attributeName != null) {
			logMessage += " for attribute " + attributeName;
		}

		LOGGER.error(IdRepoSecurityManager.getUser(),
				LoggerFileConstant.REQUEST_ID.toString(),
				requestId,
				logMessage + " -> " + ExceptionUtils.getStackTrace(e));

		// Specific handling as per your original code
		if (e instanceof IOException) {
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		}

		if (e instanceof DateTimeParseException) {
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION, e);  // added cause
		}

		// HTTP related exceptions
		Throwable rootCause = e.getCause() != null ? e.getCause() : e;

		if (rootCause instanceof HttpClientErrorException) {
			HttpClientErrorException hce = (HttpClientErrorException) rootCause;
			throw new ApiNotAccessibleException(hce.getResponseBodyAsString());
		}

		if (rootCause instanceof HttpServerErrorException) {
			HttpServerErrorException hse = (HttpServerErrorException) rootCause;
			throw new ApiNotAccessibleException(hse.getResponseBodyAsString());
		}

		// Fallback
		throw new DataEncryptionFailureException(e.getMessage(), e);
	}

	private String getHttpErrorBody(Exception e) {
		if (e instanceof HttpClientErrorException) return ((HttpClientErrorException) e).getResponseBodyAsString();
		if (e.getCause() instanceof HttpClientErrorException)
			return ((HttpClientErrorException) e.getCause()).getResponseBodyAsString();
		// similar for server exception...
		return e.getMessage();
	}
}
