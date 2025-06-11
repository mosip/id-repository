package io.mosip.credential.request.generator.util;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.dto.CryptomanagerRequestDto;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorUncheckedException;
import io.mosip.credential.request.generator.interceptor.CredentialTransactionInterceptor;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class CryptoUtil {

    @Autowired
    private RestUtil restUtil;

    public String encryptData(String request) {
        try {
            RequestWrapper<CryptomanagerRequestDto> requestWrapper = new RequestWrapper<>();
            CryptomanagerRequestDto cryptoRequest = new CryptomanagerRequestDto();
            cryptoRequest.setApplicationId(EnvUtil.getAppId());
            cryptoRequest.setData(request);
            cryptoRequest.setReferenceId(EnvUtil.getCredCryptoRefId());
            requestWrapper.setRequest(cryptoRequest);
            cryptoRequest.setTimeStamp(DateUtils.getUTCCurrentDateTime());
            requestWrapper.setRequest(cryptoRequest);
            ResponseWrapper<Map<String, String>> restResponse = restUtil.postApi(ApiName.DECRYPTION, null, null, null,
                    MediaType.APPLICATION_JSON_UTF8, requestWrapper, ResponseWrapper.class);
            if (Objects.isNull(restResponse.getErrors()) || restResponse.getErrors().isEmpty()) {
                return restResponse.getResponse().get("data");
            } else {
                IdRepoLogger.getLogger(CredentialTransactionInterceptor.class)
                        .error("KEYMANAGER ERROR RESPONSE -> " + restResponse);
                throw new CredentialRequestGeneratorUncheckedException(
                        CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED);
            }
        } catch (Exception e) {
            IdRepoLogger.getLogger(CredentialTransactionInterceptor.class).error(ExceptionUtils.getStackTrace(e));
            throw new CredentialRequestGeneratorUncheckedException(
                    CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED, e);
        }
    }

    public String decryptData(String request) {
        try {
            RequestWrapper<CryptomanagerRequestDto> requestWrapper = new RequestWrapper<>();
            CryptomanagerRequestDto cryptoRequest = new CryptomanagerRequestDto();
            cryptoRequest.setApplicationId(EnvUtil.getAppId());
            cryptoRequest.setData(request);
            cryptoRequest.setReferenceId(EnvUtil.getCredCryptoRefId());
            requestWrapper.setRequest(cryptoRequest);
            cryptoRequest.setTimeStamp(DateUtils.getUTCCurrentDateTime());
            requestWrapper.setRequest(cryptoRequest);
            ResponseWrapper<Map<String, String>> restResponse = restUtil.postApi(ApiName.ENCRYPTION, null, null, null,
                    MediaType.APPLICATION_JSON_UTF8, requestWrapper, ResponseWrapper.class);
            if (Objects.isNull(restResponse.getErrors()) || restResponse.getErrors().isEmpty()) {
                return restResponse.getResponse().get("data");
            } else {
                IdRepoLogger.getLogger(CredentialTransactionInterceptor.class)
                        .error("KEYMANAGER ERROR RESPONSE -> " + restResponse);
                throw new CredentialRequestGeneratorUncheckedException(
                        CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED);
            }
        } catch (Exception e) {
            IdRepoLogger.getLogger(CredentialTransactionInterceptor.class).error(ExceptionUtils.getStackTrace(e));
            throw new CredentialRequestGeneratorUncheckedException(
                    CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED, e);
        }
    }
}
