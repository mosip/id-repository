package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP request envelope for identity v2 create and update, and draft v2 update.
 *
 * <p>
 * Used by {@code POST} and {@code PATCH /idrepository/v1/identity/v2} and by
 * {@code PATCH /idrepository/v1/identity/draft/v2/update/{registrationId}}.
 * The v1 envelope remains {@link IdRequestDTO}.
 * </p>
 *
 * @see RequestV2DTO
 * @see IdRequestDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdRequestV2DTO extends RequestWrapper<RequestV2DTO> {

	/**
	 * Copies this envelope onto the v1 {@link IdRequestDTO} used by the services.
	 * Verification objects are carried on {@link RequestDTO#getVerifiedAttributesMetadata()},
	 * so the v1 claim-name list is left empty.
	 *
	 * @return v1 envelope with the same id, version, request time, metadata and request body
	 */
	public IdRequestDTO toLegacyRequest() {
		IdRequestDTO target = new IdRequestDTO();
		target.setId(getId());
		target.setVersion(getVersion());
		target.setRequesttime(getRequesttime());
		target.setMetadata(getMetadata());
		RequestV2DTO incoming = getRequest();
		if (incoming == null) {
			return target;
		}
		RequestDTO request = new RequestDTO();
		request.setStatus(incoming.getStatus());
		request.setIdentity(incoming.getIdentity());
		request.setDocuments(incoming.getDocuments());
		request.setRegistrationId(incoming.getRegistrationId());
		request.setUin(incoming.getUin());
		request.setBiometricReferenceId(incoming.getBiometricReferenceId());
		request.setVerifiedAttributesMetadata(incoming.getVerifiedAttributes());
		target.setRequest(request);
		return target;
	}
}
