package io.mosip.credential.request.generator.dao;

import io.mosip.credential.request.generator.aspect.SkipDecryption;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CryptoCredentialDao {

    @Autowired
    private CredentialRepositary<CredentialEntity,String> credentialRepo;

    @SkipDecryption
    public List<CredentialEntity> findCredentialByStatusCode(String statusCode, int pageSize) {
        return credentialRepo.findCredentialByStatusCode(statusCode, pageSize);
    }
}
