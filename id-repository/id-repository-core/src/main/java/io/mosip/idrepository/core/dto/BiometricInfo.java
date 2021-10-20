package io.mosip.idrepository.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author Manoj SP
 *
 */
@Data
@Builder(toBuilder = true)
public class BiometricInfo {
	private String type;
	private String subType;
	private Long qualityScore;
	private String attempts;
	private String digitalId;
}