package io.mosip.credentialstore.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.dto.CryptoWithPinRequestDto;
import io.mosip.credentialstore.dto.CryptoWithPinResponseDto;
import io.mosip.credentialstore.dto.CryptoZkResponseDto;
import io.mosip.credentialstore.dto.CryptomanagerRequestDto;
import io.mosip.credentialstore.dto.CryptomanagerResponseDto;
import io.mosip.credentialstore.dto.EncryptZkRequestDto;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.KeyManagerGetCertificateResponseDto;
import io.mosip.credentialstore.dto.KeyManagerUploadCertificateResponseDto;
import io.mosip.credentialstore.dto.PartnerGetCertificateResponseDto;
import io.mosip.credentialstore.dto.UploadCertificateRequestDto;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
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

	/** The application id. */
	@Value("${credential.service.application.id:CREDENTIAL_SERVICE}")
	private String applicationId;

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
		}  catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
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


	public String encryptData(String dataToBeEncrypted, String partnerId)
			throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
				"started encrypting data");
	

		String encryptedPacket = null;
		try {
			checkCertificateAndUpload(partnerId);

			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(dataToBeEncrypted);
			cryptomanagerRequestDto.setReferenceId(partnerId);
			cryptomanagerRequestDto
					.setPrependThumbprint(
							env.getProperty("mosip.credential.service.share.prependThumbprint", Boolean.class));
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptomanagerRequestDto);
			cryptomanagerRequestDto.setTimeStamp(localdatetime);
			String response = restUtil.postApi(ApiName.CRYPTOMANAGER_ENCRYPT, null, "", "", MediaType.APPLICATION_JSON,
					request, String.class);

			CryptomanagerResponseDto responseObject = mapper.readValue(response, CryptomanagerResponseDto.class);

			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
			encryptedPacket = responseObject.getResponse().getData();
			LOGGER.info(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"Encryption done successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"EncryptionUtil::encryptData()::exit");
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

	private void checkCertificateAndUpload(String partnerId) throws Exception {

		String getCertificateQueryParameterName = "applicationId,referenceId";
		String getCertificateQueryParameterValue = applicationId + "," + partnerId;

		String certificateResponse = restUtil.getApi(ApiName.KEYMANAGER_GET_CERTIFICATE, null,
				getCertificateQueryParameterName, getCertificateQueryParameterValue, String.class);

		KeyManagerGetCertificateResponseDto certificateResponseobj = mapper.readValue(certificateResponse,
				KeyManagerGetCertificateResponseDto.class);

		if (certificateResponseobj != null && certificateResponseobj.getResponse() != null
				&& certificateResponseobj.getResponse().getCertificate() != null
				&& !certificateResponseobj.getResponse().getCertificate().isEmpty()) {
			LOGGER.info(IdRepoSecurityManager.getUser(), ENCRYPTIONUTIL, ENCRYPTDATA,
					"partner Certificate is available in key manager");

		} else if (certificateResponseobj != null && certificateResponseobj.getErrors() != null
				&& !certificateResponseobj.getErrors().isEmpty()) {

			int count = 0;
			for (ServiceError error : certificateResponseobj.getErrors()) {
				if (error.getErrorCode().equals("KER-KMS-002") || error.getErrorCode().equals("KER-KMS-012")
						|| error.getErrorCode().equals("KER-KMS-016") || error.getErrorCode().equals("KER-KMS-018")) {
					count++;
					getPartnerCertificateAndUpload(partnerId);

				}
			}
			if (count == 0) {
				ServiceError error = certificateResponseobj.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
		}

	}
	private void getPartnerCertificateAndUpload(String partnerId)
			throws Exception, IOException, JsonParseException, JsonMappingException, DataEncryptionFailureException {
		PartnerGetCertificateResponseDto partnerCertificateResponseObj = getPartnerCertificate(partnerId);

		if (partnerCertificateResponseObj != null && partnerCertificateResponseObj.getResponse() != null
				&& partnerCertificateResponseObj.getResponse().getCertificateData() != null
				&& !partnerCertificateResponseObj.getResponse().getCertificateData().isEmpty()) {

			KeyManagerUploadCertificateResponseDto uploadCertificateResponseobj = uploadCertificate(
					partnerId, partnerCertificateResponseObj);

			if (uploadCertificateResponseobj != null && uploadCertificateResponseobj.getErrors() != null
					&& !uploadCertificateResponseobj.getErrors().isEmpty()) {
				ServiceError error1 = uploadCertificateResponseobj.getErrors().get(0);
				throw new DataEncryptionFailureException(error1.getMessage());
			}

		} else if (partnerCertificateResponseObj != null
				&& partnerCertificateResponseObj.getErrors() != null) {
			ServiceError error2 = partnerCertificateResponseObj.getErrors();
			throw new DataEncryptionFailureException(error2.getMessage());
		}
	}
	private KeyManagerUploadCertificateResponseDto uploadCertificate(String partnerId,
			PartnerGetCertificateResponseDto partnerCertificateResponseObj)
			throws Exception, IOException, JsonParseException, JsonMappingException {
		UploadCertificateRequestDto uploadCertificateRequestDto = new UploadCertificateRequestDto();
		uploadCertificateRequestDto.setApplicationId(applicationId);
		uploadCertificateRequestDto
				.setCertificateData(partnerCertificateResponseObj.getResponse().getCertificateData());
		uploadCertificateRequestDto.setReferenceId(partnerId);
		RequestWrapper<UploadCertificateRequestDto> uploadrequest = new RequestWrapper<UploadCertificateRequestDto>();
		uploadrequest.setRequest(uploadCertificateRequestDto);

		String uploadCertificateResponse = restUtil.postApi(
				ApiName.KEYMANAGER_UPLOAD_OTHER_DOMAIN_CERTIFICATE, null, "", "",
				MediaType.APPLICATION_JSON, uploadrequest, String.class);

		KeyManagerUploadCertificateResponseDto uploadCertificateResponseobj = mapper
				.readValue(uploadCertificateResponse, KeyManagerUploadCertificateResponseDto.class);
		return uploadCertificateResponseobj;
	}
	private PartnerGetCertificateResponseDto getPartnerCertificate(String partnerId)
			throws Exception, IOException, JsonParseException, JsonMappingException {
		Map<String, String> pathsegments = new HashMap<>();
		pathsegments.put("partnerId", partnerId);

		String partnerCertificateResponse = restUtil.getApi(ApiName.GET_PARTNER_CERTIFICATE, pathsegments,
				String.class);

		PartnerGetCertificateResponseDto partnerCertificateResponseObj = mapper
				.readValue(partnerCertificateResponse, PartnerGetCertificateResponseDto.class);
		return partnerCertificateResponseObj;
	}

}
