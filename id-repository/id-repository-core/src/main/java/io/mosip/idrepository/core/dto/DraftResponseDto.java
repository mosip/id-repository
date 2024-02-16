package io.mosip.idrepository.core.dto;

import lombok.Data;

import java.util.List;

/**
 * The DraftResponseDto.
 * 
 * @author Kamesh Shekhar Prasad
 */
@Data
public class DraftResponseDto {
	private List<DraftUinResponseDto> drafts;
}
