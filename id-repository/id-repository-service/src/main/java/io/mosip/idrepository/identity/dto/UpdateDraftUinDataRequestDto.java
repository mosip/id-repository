package io.mosip.idrepository.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request body of {@code PATCH /draft/uindata/{registrationId}}.
 * <p>
 * Stamps a UIN on a LOST draft (one created with {@code generateUin=false}) after
 * ABIS deduplication has matched the packet to an existing identity. The draft's
 * identity data is then back-filled from that live UIN.
 * Lombok generates accessors, {@code equals}, {@code hashCode} and {@code toString}.
 */
@Data
public class UpdateDraftUinDataRequestDto {

	/** UIN of the identity ABIS matched; must already exist in the live store. */
	@Schema(description = "UIN to be stamped on the LOST draft after ABIS resolves the match")
	private String uin;
}
