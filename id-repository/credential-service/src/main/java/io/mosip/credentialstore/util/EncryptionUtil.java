package io.mosip.credentialstore.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.dto.CryptoWithPinRequestDto;
import io.mosip.credentialstore.dto.CryptoWithPinResponseDto;
import io.mosip.credentialstore.dto.CryptoZkResponseDto;
import io.mosip.credentialstore.dto.EncryptZkRequestDto;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.service.impl.CredentialStoreServiceImpl;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;

@Component
public class EncryptionUtil {

	
	private static final Logger LOGGER = IdRepoLogger.getLogger(EncryptionUtil.class);
	
	private static final String ENCRYPTDATA = "encryptData";
	
	private static final String ENCRYPTIONUTIL = "EncryptionUtil";
	
	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";
	

	/** The Constant IO_EXCEPTION. */
	private static final String IO_EXCEPTION = "Exception while reading packet inputStream";

	/** The Constant DATE_TIME_EXCEPTION. */
	private static final String DATE_TIME_EXCEPTION = "Error while parsing packet timestamp";


	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	public String encryptDataWithPin(String data, String pin) throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
				"started encrypting data");

		String encryptedData = null;
		try {
		
			CryptoWithPinRequestDto cryptoWithPinRequestDto = new CryptoWithPinRequestDto();
			RequestWrapper<CryptoWithPinRequestDto> request = new RequestWrapper<>();
			cryptoWithPinRequestDto.setData(data);
			cryptoWithPinRequestDto.setUserPin(pin);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptoWithPinRequestDto);
		
			String response= restUtil.postApi(ApiName.KEYMANAGER_ENCRYPT_PIN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptoWithPinResponseDto responseObject= mapper.readValue(response,
					CryptoWithPinResponseDto.class);

			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
			encryptedData = responseObject.getResponse().getData();
			LOGGER.info(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"Encryption done successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA, "EncryptionUtil::encryptData()::exit");
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
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
	public EncryptZkResponseDto encryptDataWithZK(String id, List<ZkDataAttribute> zkDataAttributes) throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
				"started encrypting data");

		EncryptZkResponseDto encryptedData = null;
		try {
		
			EncryptZkRequestDto encryptZkRequestDto = new EncryptZkRequestDto();
			RequestWrapper<EncryptZkRequestDto> request = new RequestWrapper<>();
			encryptZkRequestDto.setZkDataAttributes(zkDataAttributes);
			encryptZkRequestDto.setId(id);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);

			request.setRequest(encryptZkRequestDto);
		
			String response= restUtil.postApi(ApiName.KEYMANAGER_ENCRYPT_ZK, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptoZkResponseDto responseObject= mapper.readValue(response,
					CryptoZkResponseDto.class);

			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
			encryptedData = responseObject.getResponse();
			LOGGER.info(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"Encryption done successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA, "EncryptionUtil::encryptData()::exit");
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
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
	
}
