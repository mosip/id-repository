package io.mosip.idrepository.core.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RidInfoDTO {

    private String rid;
    private LocalDateTime creationDate;
    private LocalDateTime updationDate;
}
