package io.mosip.idrepository.credential.request.validator;

import io.mosip.idrepository.core.dto.CredentialIssueRequest;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.util.StringUtils;
import org.springframework.stereotype.Component;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;

/**
 * Validates inbound credential-request-generator API payloads before queue insert.
 * <p>
 * Ensures the {@link RequestWrapper} envelope and mandatory fields on
 * {@link CredentialIssueRequest} — {@code credentialType} and {@code issuer} —
 * are present and non-blank. Throws {@link IdRepoAppException} with
 * {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#INVALID_INPUT_PARAMETER}
 * when validation fails.
 * </p>
 *
 * @author Kamesh Shekhar Prasad
 */
@Component
public class RequestValidator {

	private static final String ISSUER = "issuer";
	private static final String CREDENTIAL_TYPE = "credentialType";
	private static final Object CREDENTIAL_ISSUE_REQUEST_DTO = "credentialIssueRequestDto";

	/**
	 * Validates a credential-request-generator {@link RequestWrapper}.
	 * <p>
	 * Checks that {@code request} is non-null and that {@code credentialType} and
	 * {@code issuer} are non-blank strings.
	 * </p>
	 *
	 * @param requestWrapper MOSIP request envelope containing {@link CredentialIssueRequest}
	 * @throws IdRepoAppException when the wrapper or any mandatory field is missing or empty
	 */
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
