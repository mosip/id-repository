package io.mosip.credential.request.generator.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.dto.CryptomanagerRequestDto;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorUncheckedException;
import io.mosip.credential.request.generator.interceptor.CredentialTransactionInterceptor;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
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
import io.mosip.kernel.core.util.CryptoUtil;

@Component
public class CredentialIssueRequestHelper {

    @Autowired
    private RestUtil restUtil;

    @Autowired
    private ObjectMapper mapper;

    public CredentialServiceRequestDto getCredentialServiceRequestDto(CredentialIssueRequestDto credentialIssueRequestDto, String requestId) throws JsonProcessingException {
        CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
        credentialServiceRequestDto.setId(credentialIssueRequestDto.getId());
        credentialServiceRequestDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
        credentialServiceRequestDto.setRequestId(requestId);
        credentialServiceRequestDto.setIssuer(credentialIssueRequestDto.getIssuer());
        credentialServiceRequestDto.setRecepiant(credentialIssueRequestDto.getIssuer());
        credentialServiceRequestDto.setUser(credentialIssueRequestDto.getUser());
        credentialServiceRequestDto.setEncrypt(credentialIssueRequestDto.isEncrypt());
        credentialServiceRequestDto.setEncryptionKey(credentialIssueRequestDto.getEncryptionKey());
        credentialServiceRequestDto.setSharableAttributes(credentialIssueRequestDto.getSharableAttributes());
        credentialServiceRequestDto.setAdditionalData(credentialIssueRequestDto.getAdditionalData());
        return credentialServiceRequestDto;
    }

    public String getCredentialServiceRequest(CredentialIssueRequestDto credentialIssueRequestDto, String requestId) throws JsonProcessingException {
        CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
        credentialServiceRequestDto.setId(credentialIssueRequestDto.getId());
        credentialServiceRequestDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
        credentialServiceRequestDto.setRequestId(requestId);
        credentialServiceRequestDto.setIssuer(credentialIssueRequestDto.getIssuer());
        credentialServiceRequestDto.setRecepiant(credentialIssueRequestDto.getIssuer());
        credentialServiceRequestDto.setUser(credentialIssueRequestDto.getUser());
        credentialServiceRequestDto.setEncrypt(credentialIssueRequestDto.isEncrypt());
        credentialServiceRequestDto.setEncryptionKey(credentialIssueRequestDto.getEncryptionKey());
        credentialServiceRequestDto.setSharableAttributes(credentialIssueRequestDto.getSharableAttributes());
        credentialServiceRequestDto.setAdditionalData(credentialIssueRequestDto.getAdditionalData());
        return mapper.writeValueAsString(credentialIssueRequestDto);
    }

    public CredentialIssueRequestDto getCredentialIssueRequestDto(CredentialEntity credentialEntity) throws JsonProcessingException {
        String request = credentialEntity.getRequest();
        String decryptedData = new String(CryptoUtil
                .decodeURLSafeBase64(encryptDecryptData(ApiName.DECRYPTION, request)));
        CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(decryptedData, CredentialIssueRequestDto.class);
        return credentialIssueRequestDto;
    }

    private String encryptDecryptData(ApiName api, String request) {
        try {
            RequestWrapper<CryptomanagerRequestDto> requestWrapper = new RequestWrapper<>();
            CryptomanagerRequestDto cryptoRequest = new CryptomanagerRequestDto();
            cryptoRequest.setApplicationId(EnvUtil.getAppId());
            cryptoRequest.setData(request);
            cryptoRequest.setReferenceId(EnvUtil.getCredCryptoRefId());
            requestWrapper.setRequest(cryptoRequest);
            cryptoRequest.setTimeStamp(DateUtils.getUTCCurrentDateTime());
            requestWrapper.setRequest(cryptoRequest);
            ResponseWrapper<Map<String, String>> restResponse = restUtil.postApi(api, null, null, null,
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
