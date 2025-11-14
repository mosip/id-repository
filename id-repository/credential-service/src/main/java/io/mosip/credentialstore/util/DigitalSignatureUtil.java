package io.mosip.credentialstore.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.JWTSignatureRequestDto;
import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.dto.VerCredSignatureRequestDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;


/**
 * The Class DigitalSignatureUtil.
 */
@Component
public class DigitalSignatureUtil {

	/** The rest template. */
	@Autowired
	RestUtil restUtil;


	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	private static final Logger LOGGER = IdRepoLogger.getLogger(DigitalSignatureUtil.class);



	/**
	 * Sign.
	 *
	 * @param packet the packet
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
			dto.setIncludeCertHash(EnvUtil.getCredServiceIncludeCertificateHash());
			dto.setIncludeCertificate(EnvUtil.getCredServiceIncludeCertificate());
			dto.setIncludePayload(EnvUtil.getCredServiceIncludePayload());

			RequestWrapper<JWTSignatureRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_JWTSIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			} else if (Objects.isNull(responseObject) || Objects.isNull(responseObject.getResponse())) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"KEYMANAGER_JWTSIGN response is null");
				throw new SignatureException(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
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
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_VERCRED_SIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			} else if (Objects.isNull(responseObject) || Objects.isNull(responseObject.getResponse())) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"KEYMANAGER_VERCRED_SIGN response is null");
				throw new SignatureException(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
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
