package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;

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
	private LocalDateTime expiryTimestamp;
	private Integer transactionLimit;
}
