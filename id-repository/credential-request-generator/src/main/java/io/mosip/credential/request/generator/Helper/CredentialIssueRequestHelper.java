package io.mosip.credential.request.generator.Helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class CredentialIssueRequestHelper {

    @Autowired
    private io.mosip.credential.request.generator.util.CryptoUtil cryptoUtil;

    private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialIssueRequestHelper.class);

    @Autowired
    private ObjectMapper objectMapper;

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

    public CredentialIssueRequestDto getCredentialIssueRequestDto(CredentialEntity credentialEntity) throws JsonProcessingException {
        String request = credentialEntity.getRequest();
        LOGGER.info("ENCRYPTED REQUEST"+request);
        String decryptedData = new String(CryptoUtil
                .decodeURLSafeBase64(cryptoUtil.encryptDecryptData(ApiName.DECRYPTION,request)));
        LOGGER.info("DECRYPTED DATA "+decryptedData);
        CredentialIssueRequestDto credentialIssueRequestDto = objectMapper.readValue(decryptedData, CredentialIssueRequestDto.class);
        return credentialIssueRequestDto;
    }

}
