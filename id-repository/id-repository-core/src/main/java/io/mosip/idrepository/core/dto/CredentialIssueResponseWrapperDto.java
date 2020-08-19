package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author Loganathan Sekaran
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialIssueResponseWrapperDto extends ResponseWrapper<CredentialIssueRequestDto> {
}
