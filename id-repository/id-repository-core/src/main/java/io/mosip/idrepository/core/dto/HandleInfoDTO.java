package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandleInfoDTO {

    private String handle;
    private LocalDateTime expiryTimestamp;
    private LocalDateTime genratedOnTimestamp;
    private Integer transactionLimit;
    private Map<String, String> additionalData;
}
