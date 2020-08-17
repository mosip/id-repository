package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class Policies {

	private DataSharePolicies dataSharePolicies;

	private List<ShareableAttribute> shareableAttributes;
}
