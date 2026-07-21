package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import io.mosip.idrepository.core.dto.BaseRestResponseDTO;
import io.mosip.idrepository.core.dto.ErrorDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Share service upload response deserialized by {@link io.mosip.idrepository.credential.store.util.DataShareUtil}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataShareResponseDto extends BaseRestResponseDTO {

	private static final long serialVersionUID = -6473829105847362019L;

	/** Successful upload metadata including partner URL and signature. */
	private DataShare dataShare;

	/** Kernel error list when upload fails. */
	private List<ErrorDTO> errors;
}
