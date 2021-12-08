package io.mosip.credential.request.generator.util;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.entity.CredentialFailedEntity;
import io.mosip.credential.request.generator.entity.CredentialIssuedEntity;
import io.mosip.kernel.core.util.DateUtils;
import org.springframework.beans.BeanUtils;

public class CredentialUtilityConverter {

    public static CredentialFailedEntity convertFailed(CredentialEntity failed) {
        CredentialFailedEntity credentialFailed = new CredentialFailedEntity();
        BeanUtils.copyProperties(failed, credentialFailed);
        credentialFailed.setCreateDateTime(DateUtils.getUTCCurrentDateTime());
        credentialFailed.setUpdateDateTime(DateUtils.getUTCCurrentDateTime());
        return credentialFailed;
    }

    public static CredentialIssuedEntity convertIssued(CredentialEntity issued) {
        CredentialIssuedEntity credentialIssued = new CredentialIssuedEntity();
        BeanUtils.copyProperties(issued, credentialIssued);
        credentialIssued.setCreateDateTime(DateUtils.getUTCCurrentDateTime());
        credentialIssued.setUpdateDateTime(DateUtils.getUTCCurrentDateTime());
        return credentialIssued;
    }

    public static CredentialIssuedEntity convertFailedToIssued(CredentialFailedEntity issued) {
        CredentialIssuedEntity credentialIssued = new CredentialIssuedEntity();
        BeanUtils.copyProperties(issued, credentialIssued);
        credentialIssued.setCreateDateTime(DateUtils.getUTCCurrentDateTime());
        credentialIssued.setUpdateDateTime(DateUtils.getUTCCurrentDateTime());
        return credentialIssued;
    }
}
