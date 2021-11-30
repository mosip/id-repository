package io.mosip.idrepository.core.dto;

import java.time.LocalDate;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import lombok.Data;

@Data
public class IdentityIssuanceProfile {
	private String processName;
	private LocalDate date;
	AnonymousProfile oldProfile;
	AnonymousProfile newProfile;
	
	public static IdentityIssuanceProfileBuilder builder() {
		return new IdentityIssuanceProfileBuilder();
	}
}