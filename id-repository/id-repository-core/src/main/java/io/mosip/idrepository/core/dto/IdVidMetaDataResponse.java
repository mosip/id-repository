package io.mosip.idrepository.core.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdVidMetaDataResponse {

    private String rid;
    private String createdOn;
    private String updatedOn;
}
