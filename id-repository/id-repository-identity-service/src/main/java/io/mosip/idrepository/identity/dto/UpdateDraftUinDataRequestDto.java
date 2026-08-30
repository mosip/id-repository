package io.mosip.idrepository.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateDraftUinDataRequestDto {

	@Schema(description = "UIN to be stamped on the LOST draft after ABIS resolves the match")
	private String uin;
}
