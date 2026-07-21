package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP {@link RequestWrapper} for ID/VID metadata search requests.
 *
 * <p>
 * Wraps {@link IdVidMetadataRequestDTO} in the standard MOSIP request envelope.
 * Overrides {@link #getRequest()} with {@link Valid} so Bean Validation runs on
 * the nested request body when the controller validates the wrapper.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Inbound envelope for the ID/VID metadata search endpoint. Pair with
 * {@link IdVidMetadataResponseDTO} (typically inside a kernel response wrapper).
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity metadata search controllers</li>
 *   <li>{@code IdRepoService#getIdVidMetadata}</li>
 * </ul>
 *
 * @see IdVidMetadataRequestDTO
 * @see IdVidMetadataResponseDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdVidMetadataRequestWrapper extends RequestWrapper<IdVidMetadataRequestDTO> {

	/**
	 * Returns the nested metadata request, annotated for Bean Validation.
	 *
	 * @return the {@link IdVidMetadataRequestDTO} body
	 */
	@Override
	@Valid
	public IdVidMetadataRequestDTO getRequest() {
		return super.getRequest();
	}
}
