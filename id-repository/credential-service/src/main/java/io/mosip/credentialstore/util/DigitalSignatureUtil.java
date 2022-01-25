package io.mosip.credentialstore.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.JWTSignatureRequestDto;
import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.dto.VerCredSignatureRequestDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

import static io.mosip.idrepository.core.constant.IdRepoConstants.DATETIME_PATTERN;


/**
 * The Class DigitalSignatureUtil.
 */
@Component
public class DigitalSignatureUtil {

	/** The environment. */
	@Autowired
	private Environment environment;

	/** The rest template. */
	@Autowired
	RestUtil restUtil;


	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";


	private static final Logger LOGGER = IdRepoLogger.getLogger(DigitalSignatureUtil.class);



	/**
	 * Sign.
	 *
	 * @param data the data
	 * @return the byte[]
	 * @throws ApiNotAccessibleException 
	 * @throws SignatureException 
	 */
	@Retryable(value = { SignatureException.class,
			ApiNotAccessibleException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public String sign(String data, String requestId) throws ApiNotAccessibleException, SignatureException {
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Digital signature entry");

			JWTSignatureRequestDto dto = new JWTSignatureRequestDto();
			dto.setDataToSign(data);
			dto.setIncludeCertHash(
					environment.getProperty("mosip.credential.service.includeCertificateHash", Boolean.class));
			dto.setIncludeCertificate(
					environment.getProperty("mosip.credential.service.includeCertificate", Boolean.class));
			dto.setIncludePayload(environment.getProperty("mosip.credential.service.includePayload", Boolean.class));

			RequestWrapper<JWTSignatureRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(environment.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_JWTSIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			}
			String signedData = responseObject.getResponse().getJwtSignedData();
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Signed data successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Digital signature exit");
			return signedData;
		} catch (IOException e) {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new SignatureException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new SignatureException(e);
			}

		}

	}

	@Retryable(value = { SignatureException.class,
		ApiNotAccessibleException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public String signVerCred(String data, String requestId) throws ApiNotAccessibleException, SignatureException {
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Digital signature entry");

			VerCredSignatureRequestDto verCredDto = new VerCredSignatureRequestDto();
			verCredDto.setDataToSign(data);
			verCredDto.setB64JWSHeaderParam(false);
			verCredDto.setIncludePayload(false);
			verCredDto.setValidateJson(false);
			verCredDto.setSignAlgorithm(JsonConstants.VC_SIGN_ALGO);

			RequestWrapper<VerCredSignatureRequestDto> request = new RequestWrapper<>();
			request.setRequest(verCredDto);
			request.setMetadata(null);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(environment.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_VERCRED_SIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			}
			String signedData = responseObject.getResponse().getJwtSignedData();
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"JWS Signed data successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"JWS Digital signature exit");
			return signedData;
		} catch (IOException e) {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new SignatureException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new SignatureException(e);
			}

		}
	}
}
