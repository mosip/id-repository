package io.mosip.idrepository.core.builder;

import java.time.LocalDate;

import io.mosip.idrepository.core.dto.AnonymousProfile;
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