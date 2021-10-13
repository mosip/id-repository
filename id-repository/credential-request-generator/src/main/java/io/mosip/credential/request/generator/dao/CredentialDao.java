package io.mosip.credential.request.generator.dao;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CredentialDao {

    @Value("${credential.batch.status:NEW}")
    private List<String> status;

    @Value("${credential.batch.page.size:100}")
    private int pageSize;

    private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialDao.class);

    /** The crdential repo. */
    @Autowired
    private CredentialRepositary<CredentialEntity, String> crdentialRepo;


    public void update(List<CredentialEntity> credentialEntities) {
        crdentialRepo.saveAll(credentialEntities);
        LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "Record updated successfully",
                "Total records : " + credentialEntities.size());
    }


    public List<CredentialEntity> getCredentials() {
        LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "getCredentials()",
                "Inside getCredentials() method");

        Map<String, Object> params = new HashMap<>();

        String queryStr = "select c from CredentialEntity c where c.statusCode=:statusCode order by createDateTime";

        params.put("statusCode", status);

        List<CredentialEntity> credentialEntities = crdentialRepo.createQuerySelect(queryStr, params, pageSize);

        LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "getCredentials()",
                "Total records picked from credential_transaction table for processing is " + credentialEntities.size());

        return credentialEntities;
    }
}
