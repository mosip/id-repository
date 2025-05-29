package io.mosip.idrepository.identity.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * The Get RID dto.
 * 
 * @author Ritik Jain
 */
@Data
public class RidDto {

	private String rid;
	private LocalDateTime updatedDate;
}
