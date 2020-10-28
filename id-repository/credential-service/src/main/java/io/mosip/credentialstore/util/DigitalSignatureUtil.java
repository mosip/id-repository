package io.mosip.credentialstore.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.dto.SignRequestDto;
import io.mosip.credentialstore.dto.SignResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.service.impl.CredentialStoreServiceImpl;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;


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

	private static final String SIGN = "sign";
	
	private static final String DIGITALSIGNATURE = "DigitalSignatureUtil";


	/**
	 * Sign.
	 *
	 * @param packet the packet
	 * @return the byte[]
	 * @throws ApiNotAccessibleException 
	 * @throws SignatureException 
	 */
	public String sign(byte[] packet) throws ApiNotAccessibleException, SignatureException {
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), DIGITALSIGNATURE, SIGN,
					"entry");
			String packetData = new String(packet, StandardCharsets.UTF_8);
			SignRequestDto dto = new SignRequestDto();
			dto.setData(packetData);
			RequestWrapper<SignRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(environment.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_SIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			}
			String signedData = responseObject.getResponse().getSignature();
			LOGGER.info(IdRepoSecurityManager.getUser(), DIGITALSIGNATURE, SIGN,
					"Signed data successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), DIGITALSIGNATURE, SIGN,
					"exit");
			return signedData;
		} catch (IOException e) {
			LOGGER.debug(IdRepoSecurityManager.getUser(), DIGITALSIGNATURE, SIGN,
					ExceptionUtils.getStackTrace(e));
			throw new SignatureException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), DIGITALSIGNATURE, SIGN,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new io.mosip.credentialstore.exception.ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new SignatureException(e);
			}

		}

	}

}
