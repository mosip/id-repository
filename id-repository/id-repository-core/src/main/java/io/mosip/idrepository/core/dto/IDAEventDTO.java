package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.mosip.idrepository.core.constant.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Manoj SP
 *
 */    
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IDAEventDTO {
	private EventType eventType;
	private String saltedIdHash;
	private String tokenId;
	private List<AuthtypeStatus> authTypeStatusList;
	private LocalDateTime expiryTimestamp;
	private Integer transactionLimit;
}
