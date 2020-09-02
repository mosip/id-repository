package io.mosip.credentialstore.dto;

import java.time.LocalDateTime;
import java.util.List;


import lombok.Data;


@Data
public class PolicyResponseDto {
	
    private String policyGroupId;
	
	private String policyGroupName;
	
	private String policyGroupDesc;
	
	private Boolean policyGroupStatus;
	
	private String policyGroup_cr_by;
	
	private LocalDateTime policyGroup_cr_dtimes;
	
	private String policyGroup_up_by;
	
	private LocalDateTime policyGroup_upd_dtimes;	
	
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
