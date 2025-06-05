package io.mosip.credential.request.generator.service;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CredentialServiceFacade {

    List<CredentialEntity> getCredentialsWithNoDecryption(String batchId);
}
