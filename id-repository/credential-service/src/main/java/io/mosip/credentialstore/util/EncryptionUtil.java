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
import io.mosip.kernel.core.util.DateUtils;

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

	public String encryptDataWithPin(String attributeName, String data, String pin, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started encrypting data using pin");

		String encryptedData = null;
		try {
		
			CryptoWithPinRequestDto cryptoWithPinRequestDto = new CryptoWithPinRequestDto();
			RequestWrapper<CryptoWithPinRequestDto> request = new RequestWrapper<>();
			cryptoWithPinRequestDto.setData(data);
			cryptoWithPinRequestDto.setUserPin(pin);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptoWithPinRequestDto);
		
			String response= restUtil.postApi(ApiName.KEYMANAGER_ENCRYPT_PIN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptoWithPinResponseDto responseObject= mapper.readValue(response,
					CryptoWithPinResponseDto.class);

			if (responseObject != null) {
				if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
					ServiceError error = responseObject.getErrors().get(0);
					LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"encrypted failed for attribute  " + attributeName);
					throw new DataEncryptionFailureException(error.getMessage());
				} else if (responseObject.getResponse() != null) {
					encryptedData = responseObject.getResponse().getData();
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"Pin Based Encryption done successfully");
				}
			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended encrypting data using pin");
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"encrypted failed for attribute  " + attributeName + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"encrypted failed for attribute  " + attributeName + ExceptionUtils.getStackTrace(e));
			if (e instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e;
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e;
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataEncryptionFailureException(e);
			}

		}
		return encryptedData;
    	
    }

	public EncryptZkResponseDto encryptDataWithZK(String id, List<ZkDataAttribute> zkDataAttributes, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started encrypting data using ZK encryption");

		EncryptZkResponseDto encryptedData = null;
		try {
		
			EncryptZkRequestDto encryptZkRequestDto = new EncryptZkRequestDto();
			RequestWrapper<EncryptZkRequestDto> request = new RequestWrapper<>();
			encryptZkRequestDto.setZkDataAttributes(zkDataAttributes);
			encryptZkRequestDto.setId(id);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			request.setRequesttime(localdatetime);

			request.setRequest(encryptZkRequestDto);
		
			String response= restUtil.postApi(ApiName.KEYMANAGER_ENCRYPT_ZK, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptoZkResponseDto responseObject= mapper.readValue(response,
					CryptoZkResponseDto.class);

			if (responseObject != null) {
				if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
					LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"ZK encryption failed");
					ServiceError error = responseObject.getErrors().get(0);
					throw new DataEncryptionFailureException(error.getMessage());
				} else if (responseObject.getResponse() != null) {
					encryptedData = responseObject.getResponse();
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"ZK Encryption done successfully");
				}
			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended encrypting data using ZK encryption");
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ZK encryption error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		}  catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ZK encryption error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataEncryptionFailureException(e);
			}

		}
		return encryptedData;
		
	}

	@Retryable(value = { DataEncryptionFailureException.class,
			ApiNotAccessibleException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public String encryptData(String dataToBeEncrypted, String partnerId, String requestId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started encrypting data using partner certificate");
	

		String encryptedPacket = null;
		try {


			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(dataToBeEncrypted);
			cryptomanagerRequestDto.setReferenceId(partnerId);
			cryptomanagerRequestDto
					.setPrependThumbprint(EnvUtil.getPrependThumbprintStatus());
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptomanagerRequestDto);
			cryptomanagerRequestDto.setTimeStamp(localdatetime);
			String response = restUtil.postApi(ApiName.CRYPTOMANAGER_ENCRYPT, null, "", "", MediaType.APPLICATION_JSON,
					request, String.class);

			CryptomanagerResponseDto responseObject = mapper.readValue(response, CryptomanagerResponseDto.class);

			if (responseObject != null) {
				if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
					LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"credential encryption failed");
					ServiceError error = responseObject.getErrors().get(0);
					throw new DataEncryptionFailureException(error.getMessage());
				} else if (responseObject.getResponse() != null) {
					encryptedPacket = responseObject.getResponse().getData();
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"Credential Data Encryption done successfully");
				}
			}
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended encrypting data using partner certificate");
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Credential Data Encryption error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Credential Data Encryption error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Credential Data Encryption error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataEncryptionFailureException(e.getMessage());
			}

		}
		return encryptedPacket;

	}



}
