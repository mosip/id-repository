package io.mosip.idrepository.pipeline;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.CredentialIssueRequest;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.credential.request.service.CredentialRequestService;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.StringUtils;

/**
 * SDK adapter for credential-request queueing within the consolidated JVM.
 * <p>
 * Delegates to {@link CredentialRequestService} so rows are written to
 * {@code credential_transaction} without internal HTTP.
 * </p>
 */
@Component
public class InProcessCredentialRequestClient {

	@Autowired
	@Lazy
	private CredentialRequestService credentialRequestService;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Queues a credential issuance request in {@code credential_transaction}.
	 *
	 * @param requestDto partner credential issue payload
	 * @return MOSIP response map ({@code response}, {@code errors}, etc.)
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> queueRequest(CredentialIssueRequestDto requestDto) {
		CredentialIssueRequest request = toCredentialIssueRequest(requestDto);
		ResponseWrapper<CredentialIssueResponse> wrapper;
		if (StringUtils.isNotEmpty(requestDto.getRequestId())) {
			wrapper = credentialRequestService.createCredentialIssuanceByRid(request, requestDto.getRequestId());
		} else {
			wrapper = credentialRequestService.createCredentialIssuance(request);
		}
		return objectMapper.convertValue(wrapper, Map.class);
	}

	private CredentialIssueRequest toCredentialIssueRequest(CredentialIssueRequestDto dto) {
		CredentialIssueRequest request = new CredentialIssueRequest();
		request.setId(dto.getId());
		request.setCredentialType(dto.getCredentialType());
		request.setIssuer(dto.getIssuer());
		request.setRecepiant(dto.getRecepiant());
		request.setUser(dto.getUser());
		request.setEncrypt(dto.isEncrypt());
		request.setEncryptionKey(dto.getEncryptionKey());
		request.setSharableAttributes(dto.getSharableAttributes());
		request.setAdditionalData(dto.getAdditionalData() != null ? new HashMap<>(dto.getAdditionalData()) : null);
		return request;
	}
}
