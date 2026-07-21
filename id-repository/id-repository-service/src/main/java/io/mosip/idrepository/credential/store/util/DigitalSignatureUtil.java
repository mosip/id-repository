package io.mosip.idrepository.credential.store.util;

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

import io.mosip.idrepository.credential.store.constant.ApiName;
import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.idrepository.credential.store.constant.JsonConstants;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.JWTSignatureRequestDto;
import io.mosip.idrepository.credential.store.dto.SignResponseDto;
import io.mosip.idrepository.credential.store.dto.VerCredSignatureRequestDto;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.SignatureException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;


/**
 * JWT and verifiable-credential signing via MOSIP keymanager.
 * <p>
 * Wraps {@link ApiName#KEYMANAGER_JWTSIGN} and {@link ApiName#KEYMANAGER_VERCRED_SIGN} calls
 * during credential issuance.
 * </p>
 */
@Component
public class DigitalSignatureUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(DigitalSignatureUtil.class);

	/** Outbound REST client for keymanager signature APIs. */
	@Autowired
	private CredentialStoreRestUtil restUtil;

	/** Deserializes keymanager JWT sign responses. */
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Signs credential data with kernel JWT signature service ({@link ApiName#KEYMANAGER_JWTSIGN}).
	 * <p>
	 * Retries on transient failures per {@code mosip.credential.service.retry.*} properties.
	 * </p>
	 *
	 * @param data      base64-encoded payload to sign
	 * @param requestId correlation id for logging
	 * @return compact JWS from keymanager
	 * @throws ApiNotAccessibleException if HTTP call fails
	 * @throws SignatureException        if keymanager returns errors
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

	/**
	 * Signs a verifiable credential document ({@link ApiName#KEYMANAGER_VERCRED_SIGN}).
	 *
	 * @param data      VC JSON-LD document to sign
	 * @param requestId correlation id for logging
	 * @return JWS attached to VC proof block
	 * @throws ApiNotAccessibleException if HTTP call fails
	 * @throws SignatureException        if keymanager returns errors
	 */
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
