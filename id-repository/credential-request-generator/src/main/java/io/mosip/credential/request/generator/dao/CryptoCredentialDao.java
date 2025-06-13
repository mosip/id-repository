package io.mosip.credential.request.generator.dao;

import io.mosip.credential.request.generator.aspect.SkipDecryption;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CryptoCredentialDao class for retrieving credential data with cryptographic context awareness.
 * <p>
 * This class provides access to {@link CredentialEntity} objects from the database
 * while selectively bypassing decryption logic using the {@link SkipDecryption} annotation.
 * </p>
 * The {@code findCredentialByStatusCode} method fetches credentials by status code,
 * and decryption is skipped to improve performance for internal processing like batch jobs.
 *
 * @author tarique-azeez
 */

@Component
public class CryptoCredentialDao {

    @Autowired
    private CredentialRepositary<CredentialEntity,String> credentialRepo;

    @SkipDecryption
    public List<CredentialEntity> findCredentialByStatusCode(String statusCode, int pageSize) {
        return credentialRepo.findCredentialByStatusCode(statusCode, pageSize);
    }
}
