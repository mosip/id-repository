package io.mosip.idrepository.core.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

	/**
	 * Object-form verification metadata copied from a v2 request.
	 * Not part of the v1 JSON body; v1 clients still send {@link BaseRequestResponseDTO#getVerifiedAttributes()}.
	 */
	@JsonIgnore
	private List<VerificationMetadata> verifiedAttributesMetadata;

}
