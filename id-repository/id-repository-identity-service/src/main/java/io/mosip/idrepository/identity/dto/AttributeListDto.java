package io.mosip.idrepository.identity.dto;

import java.util.List;

import lombok.Data;

/**
 * The attribute list dto.
 * 
 * @author Ritik Jain
 */
@Data
public class AttributeListDto {

	private List<UpdateCountDto> attributes;

}
