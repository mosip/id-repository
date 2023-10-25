package io.mosip.idrepository.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class RequestDTO - response DTO containing additional fields for request
 * field in {@code IdRequestDTO}.
 *
 * @author Manoj SP
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestDTO extends BaseRequestResponseDTO {

	private String registrationId;
	
	private String uin;
	
	@Deprecated(since = "1.1.4")
	private String biometricReferenceId;

	@Override
	public String toString() {
		return "RequestDTO [registrationId=" + registrationId + ", uin=" + uin + ", biometricReferenceId="
				+ biometricReferenceId + ", getRegistrationId()=" + getRegistrationId() + ", getUin()=" + getUin()
				+ ", getBiometricReferenceId()=" + getBiometricReferenceId() + ", hashCode()=" + hashCode()
				+ ", getStatus()=" + getStatus() + ", getIdentity()=" + getIdentity() + ", getDocuments()="
				+ getDocuments() + ", getVerifiedAttributes()=" + getVerifiedAttributes() + ", toString()="
				+ super.toString() + ", getClass()=" + getClass() + "]";
	}
}
