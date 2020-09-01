package io.mosip.credentialstore.dto;

import java.util.List;

import io.mosip.idrepository.core.dto.BaseRestResponseDTO;
import io.mosip.idrepository.core.dto.ErrorDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataShareResponseDto extends BaseRestResponseDTO {

	private static final long serialVersionUID = 1L;


    private DataShare dataShare;
    

	private List<ErrorDTO> errors;
}
