package io.mosip.credential.request.generator.dao;

import io.mosip.credential.request.generator.aspect.SkipDecryption;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class is designed for keeping the data access methods where we want control the
 * cryptographic loading CredentialEntity. Example: Avoid decryption while loading the
 * CredentialEntity.
 *
 * @author tarique-azeez
 */

@Component
public class CryptoCredentialDao {

    @Autowired
    private CredentialRepositary<CredentialEntity, String> credentialRepo;

    @SkipDecryption
    public List<CredentialEntity> findCredentialByStatusCode(String statusCode, int pageSize) {
        return credentialRepo.findCredentialByStatusCode(statusCode, pageSize);
    }
}
