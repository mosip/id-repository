package io.mosip.idrepository.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HandleDto {

    private String handle;
    private String handleHash;
}
