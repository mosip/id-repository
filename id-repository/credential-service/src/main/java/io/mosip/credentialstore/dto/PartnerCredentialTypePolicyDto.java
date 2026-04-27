package io.mosip.credentialstore.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;


@Data
public class PartnerCredentialTypePolicyDto implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String partnerId;

	private String credentialType;

	private String policyId;

	private String policyName;

	private String policyDesc;

	private String policyType;

	private LocalDateTime publishDate;

	private LocalDateTime validTill;

	private String status;

	private String version;

	private String schema;

	private Boolean is_Active;

	private String cr_by;

	private LocalDateTime cr_dtimes;

	private String up_by;

	private LocalDateTime upd_dtimes;
	
	private PolicyAttributesDto policies;
}
