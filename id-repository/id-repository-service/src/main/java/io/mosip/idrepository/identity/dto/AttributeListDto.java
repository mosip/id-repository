package io.mosip.idrepository.identity.dto;

import java.util.List;

import lombok.Data;

/**
 * Wrapper listing sharable identity attribute names for retrieve requests.
 */
@Data
public class AttributeListDto {

	/** Attributes (List<UpdateCountDto>). */
	private List<UpdateCountDto> attributes;

}
