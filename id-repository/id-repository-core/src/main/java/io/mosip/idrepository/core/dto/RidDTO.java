package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RidDTO {

        private String rid;
        private LocalDateTime updatedDate;

    }

