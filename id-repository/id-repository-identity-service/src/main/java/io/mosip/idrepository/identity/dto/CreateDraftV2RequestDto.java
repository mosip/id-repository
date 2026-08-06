package io.mosip.idrepository.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateDraftV2RequestDto {

	@Schema(description = "UIN of the resident (required for UPDATE packets; omit for NEW packets so UIN is auto-generated)")
	private String uin;

	@Schema(description = "When true (default) a UIN is allocated during draft creation. Set to false for LOST packets where the UIN is resolved later after ABIS deduplication.")
	private boolean generateUin = true;
}
