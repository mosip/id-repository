package io.mosip.idrepository.core.dto;

import lombok.Data;

import java.util.List;

/**
 * Single identity update draft summary in draft-list responses.
 *
 * <p>
 * Exposes registration id, creation time, and the set of attributes modified
 * in the draft. Aggregated under {@link DraftResponseDto#getDrafts()}.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Element type for identity draft-list APIs. Does not include the full draft
 * identity JSON — only summary metadata for listing UIs.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link DraftResponseDto}</li>
 *   <li>Identity draft service implementations</li>
 * </ul>
 *
 * @author Kamesh Shekhar Prasad
 * @see DraftResponseDto
 */
@Data
public class DraftUinResponseDto {

	/** Registration identifier (RID) associated with the draft. */
	private String rid;

	/** Creation timestamp of the draft record. */
	private String createdDTimes;

	/** Identity attribute names included in this draft update. */
	private List<String> attributes;
}
