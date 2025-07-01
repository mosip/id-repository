package io.mosip.credential.request.generator.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialIssueRequestHelper.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private io.mosip.credential.request.generator.util.CryptoUtil cryptoUtil;

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

    //Convert the Encrypted data to Decrypted data
    public CredentialIssueRequestDto getCredentialIssueRequestDto(CredentialEntity credentialEntity) throws JsonProcessingException {
        String request = credentialEntity.getRequest();
        String decryptedData = new String(CryptoUtil
                .decodeURLSafeBase64(cryptoUtil.decryptData(request)));
        CredentialIssueRequestDto credentialIssueRequestDto = objectMapper.readValue(decryptedData, CredentialIssueRequestDto.class);
        return credentialIssueRequestDto;
    }
}
