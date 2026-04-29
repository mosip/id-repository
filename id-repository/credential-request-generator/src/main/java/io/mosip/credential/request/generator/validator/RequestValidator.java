package io.mosip.credential.request.generator.validator;

import io.mosip.idrepository.core.dto.CredentialIssueRequest;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.util.StringUtils;
import org.springframework.stereotype.Component;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;

/**
 * @author Kamesh Shekhar Prasad
 * Class to check request of credential request generator API.
 */
@Component
public class RequestValidator {

    private static final String ISSUER = "issuer";
    private static final String CREDENTIAL_TYPE = "credentialType";
    private static final Object CREDENTIAL_ISSUE_REQUEST_DTO = "credentialIssueRequestDto";

    public void validateRequestGeneratorRequest(RequestWrapper<CredentialIssueRequest> requestWrapper) throws IdRepoAppException {
        validateAPIRequestToCheckNull(requestWrapper);
        validateDataToCheckNullOrEmpty(requestWrapper.getRequest().getCredentialType(),
                CREDENTIAL_TYPE);
        validateDataToCheckNullOrEmpty(requestWrapper.getRequest().getIssuer(), ISSUER);
       
    }

    private void validateAPIRequestToCheckNull(RequestWrapper<CredentialIssueRequest> requestWrapper) throws IdRepoAppException {
        if (requestWrapper.getRequest() == null) {
            throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
                    String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), CREDENTIAL_ISSUE_REQUEST_DTO));
        }
    }

    private void validateDataToCheckNullOrEmpty(String variableValue, String variableName) throws IdRepoAppException {
        if (StringUtils.isBlank(variableValue)) {
            throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
                    String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), variableName));
        }
    }
}
