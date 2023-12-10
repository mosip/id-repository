package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
public class HandleInfosDTO extends ResponseWrapper<List<HandleInfoDTO>> {

    private List<HandleInfoDTO> response;
}
