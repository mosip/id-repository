package io.mosip.idrepository.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request body of {@code POST /draft/v2/create/{registrationId}}.
 * <p>
 * Selects how a V2 draft gets its UIN:
 * <ul>
 * <li>{@code uin} set: the draft is created for that existing identity (UPDATE,
 * ACTIVATED or DEACTIVATED packets) and its live files are copied into the draft path.</li>
 * <li>{@code uin} empty and {@code generateUin=true}: a new UIN is allocated (NEW packets).</li>
 * <li>{@code uin} empty and {@code generateUin=false}: the draft is created without a
 * UIN (LOST packets). The UIN is stamped later through
 * {@link UpdateDraftUinDataRequestDto} once ABIS resolves the match.</li>
 * </ul>
 * Lombok generates accessors, {@code equals}, {@code hashCode} and {@code toString}.
 */
@Data
public class CreateDraftV2RequestDto {

	/** UIN of the existing identity; omit for NEW and LOST packets. */
	@Schema(description = "UIN of the resident (required for UPDATE packets; omit for NEW packets so UIN is auto-generated)")
	private String uin;

	/**
	 * Whether to allocate a new UIN when {@link #uin} is empty. Defaults to {@code true};
	 * {@code false} creates a LOST draft with no UIN.
	 */
	@Schema(
			description = "When true (default) a UIN is allocated during draft creation. Set to false for LOST packets where the UIN is resolved later after ABIS deduplication.",
			defaultValue = "true")
	private boolean generateUin = true;
}
