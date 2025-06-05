package io.mosip.credential.request.generator.service.impl;

import io.mosip.credential.request.generator.api.annotation.SkipDecryption;
import io.mosip.credential.request.generator.dao.CredentialDao;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.service.CredentialServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CredentialServiceFacadeImpl implements CredentialServiceFacade {

    @Autowired
    private CredentialDao credentialDao;

    @Override
    @SkipDecryption
    public List<CredentialEntity> getCredentialsWithNoDecryption(String batchId) {
        return credentialDao.getCredentials(batchId);
    }
}
