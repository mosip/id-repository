package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * The Class IdRequestDTO - Request DTO for Id Repository Identity service.
 *
 * @author Manoj SP
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdRequestDTO<T> extends RequestDTO {

    private T verifiedAttributes;
}
