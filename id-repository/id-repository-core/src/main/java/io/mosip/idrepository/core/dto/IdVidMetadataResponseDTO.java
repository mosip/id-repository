package io.mosip.idrepository.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration and update timestamps for an individual's ID/VID metadata record.
 *
 * <p>
 * Lightweight response exposing RID and audit timestamps without the full
 * identity payload. Returned by metadata search for clients that only need
 * registration correlation and last-update information.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Produced by {@code IdRepoService#getIdVidMetadata} in response to
 * {@link IdVidMetadataRequestDTO} / {@link IdVidMetadataRequestWrapper}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity metadata search controllers</li>
 *   <li>Downstream systems correlating RID to identity lifecycle</li>
 * </ul>
 *
 * @see IdVidMetadataRequestDTO
 * @see io.mosip.idrepository.core.spi.IdRepoService#getIdVidMetadata
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdVidMetadataResponseDTO {

    /** Registration identifier (RID) linked to the identity record. */
    private String rid;

    /** ISO-8601 timestamp when the identity was first created. */
    private String createdOn;

    /** ISO-8601 timestamp of the most recent identity update. */
    private String updatedOn;
}
