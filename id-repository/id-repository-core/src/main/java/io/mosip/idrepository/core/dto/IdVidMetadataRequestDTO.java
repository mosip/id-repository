package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for ID/VID metadata search by individual identifier.
 *
 * <p>
 * Carries the individual id and id type validated at the service layer before
 * metadata lookup. Does not request full identity or biometric payloads.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Nested in {@link IdVidMetadataRequestWrapper} for the metadata search API
 * exposed via {@code IdRepoService#getIdVidMetadata}. Returns
 * {@link IdVidMetadataResponseDTO} with RID and audit timestamps.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code IdRepoService#getIdVidMetadata}</li>
 *   <li>Clients that need registration metadata without full identity</li>
 * </ul>
 *
 * @see IdVidMetadataRequestWrapper
 * @see IdVidMetadataResponseDTO
 * @see io.mosip.idrepository.core.spi.IdRepoService#getIdVidMetadata
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdVidMetadataRequestDTO {

    /** Individual identifier (UIN, VID, or handle value). */
    private String individualId;

    /** Type of {@link #individualId} (for example, UIN, VID). */
    private String idType;
}
