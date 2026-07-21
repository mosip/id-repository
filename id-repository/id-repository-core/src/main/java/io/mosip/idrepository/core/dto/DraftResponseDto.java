package io.mosip.idrepository.core.dto;

import lombok.Data;

import java.util.List;

/**
 * Response listing all identity update drafts for an individual.
 *
 * <p>
 * Wraps a collection of {@link DraftUinResponseDto} entries returned by
 * draft-listing identity APIs before publish or discard.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Used by identity draft-list endpoints under {@code /idrepository/v1/identity}.
 * Each draft summarizes RID, creation time, and modified attributes.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity draft controllers and services</li>
 *   <li>Registration / portal clients listing pending drafts</li>
 * </ul>
 *
 * @author Kamesh Shekhar Prasad
 * @see DraftUinResponseDto
 */
@Data
public class DraftResponseDto {

	/** Draft records pending publish or discard. */
	private List<DraftUinResponseDto> drafts;
}
