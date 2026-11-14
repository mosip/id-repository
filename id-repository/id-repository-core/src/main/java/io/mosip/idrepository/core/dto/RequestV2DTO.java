package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * Request body for {@code POST} and {@code PATCH /idrepository/v1/identity/v2}.
 *
 * <p>
 * Same identity, document, and registration fields as {@link RequestDTO}, with
 * {@link #verifiedAttributes} typed as {@link VerificationMetadata} objects.
 * The v1 {@link RequestDTO} is unchanged and still carries a list of claim names.
 * </p>
 *
 * @see IdRequestV2DTO
 * @see RequestDTO
 */
@Data
public class RequestV2DTO {

	/** Lifecycle status of the identity record, for example {@code ACTIVATED}. */
	private String status;

	/** Identity JSON object keyed by schema field names. */
	private Object identity;

	/** Supporting documents attached to the record. */
	private List<DocumentsDTO> documents;

	/** Registration identifier (RID) from the registration module. */
	private String registrationId;

	/** Unique Identification Number for the individual. */
	private String uin;

	/**
	 * Legacy biometric reference identifier.
	 *
	 * @deprecated since 1.1.4; no longer used in current ID schema flows.
	 */
	@Deprecated(since = "1.1.4")
	private String biometricReferenceId;

	/** Verification records for the claims in {@link #identity}. */
	private List<VerificationMetadata> verifiedAttributes;
}
