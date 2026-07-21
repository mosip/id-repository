package io.mosip.idrepository.credential.request.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Translates between credential-queue persistence and credential-service DTOs.
 * <p>
 * Used by batch tasklets to map a decrypted {@link CredentialEntity#getRequest()}
 * into a {@link CredentialIssueRequestDto}, and to build the
 * {@link CredentialServiceRequestDto} passed to credential-service (in-process or HTTP).
 * </p>
 */
@Component
public class CredentialIssueRequestHelper {

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialIssueRequestHelper.class);

	/**
	 * Cryptomanager client for decrypting queue payloads when bypassing the Hibernate interceptor.
	 */
	@Autowired
	private io.mosip.idrepository.credential.request.util.CryptoUtil cryptoUtil;

	/**
	 * Jackson mapper for JSON deserialization of decrypted request payloads.
	 */
	@Autowired
	@Lazy
	private ObjectMapper objectMapper;

	/**
	 * Builds a {@link CredentialServiceRequestDto} from a queue DTO for credential-service invocation.
	 * <p>
	 * Copies identity, credential type, encryption flags, sharable attributes, and additional data.
	 * The recipient is set to the issuer, matching legacy credential-request-generator behaviour.
	 * </p>
	 *
	 * @param credentialIssueRequestDto decrypted issue request from the queue
	 * @param requestId               queue row primary key ({@code credential_transaction.id})
	 * @return DTO ready for {@code /v1/credentialservice/issue}
	 * @throws JsonProcessingException if JSON mapping is required and fails
	 */
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

	/**
	 * Deserializes the decrypted {@link CredentialEntity#getRequest()} JSON into a
	 * {@link CredentialIssueRequestDto}.
	 * <p>
	 * Decrypts URL-safe Base64-encoded ciphertext via {@link io.mosip.idrepository.credential.request.util.CryptoUtil}
	 * before Jackson binding. Used when batch code reads queue rows outside the Hibernate load path.
	 * </p>
	 *
	 * @param credentialEntity queue row with encrypted {@code request} column
	 * @return parsed credential issue request
	 * @throws JsonProcessingException if decrypted content is not valid JSON for the target type
	 */
	public CredentialIssueRequestDto getCredentialIssueRequestDto(CredentialEntity credentialEntity) throws JsonProcessingException {
		String request = credentialEntity.getRequest();
		return objectMapper.readValue(resolveRequestPayload(request), CredentialIssueRequestDto.class);
	}

	/**
	 * Returns plaintext JSON for the queue {@code request} column.
	 * <p>
	 * {@link io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor}
	 * decrypts on Hibernate load; batch paths may leave ciphertext when
	 * {@link io.mosip.idrepository.credential.request.context.CryptoContext#isSkipDecryption()} is set.
	 * </p>
	 */
	private String resolveRequestPayload(String request) {
		if (request == null || request.isBlank()) {
			return request;
		}
		String trimmed = request.stripLeading();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			return request;
		}
		return new String(CryptoUtil.decodeURLSafeBase64(cryptoUtil.decryptData(request)));
	}
}
