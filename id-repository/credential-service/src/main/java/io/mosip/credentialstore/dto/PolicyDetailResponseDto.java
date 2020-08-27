package io.mosip.credentialstore.dto;



import lombok.Data;

@Data
public class PolicyDetailResponseDto {
	
	private String policyGroupId;
	
	private String policyGroupName;
	
	private String policyGroupDesc;
	
	private String policyGroupStatus;
	
	private String policyGroup_cr_by;
	
	private String policyGroup_cr_dtimes;

	
	private String policyGroup_up_by;
	
	private String policyGroup_upd_dtimes;
	
	private String policyId;
	
	private String policyName;
	
	private String policyDesc;
	
	private String publishDate;
	
	private String validTill;
	
	private String version;
	
	private String status;
	
	private String schema;
	
	private boolean is_Active;
	
	private String cr_by;
	
	private String cr_dtimes;
	
	private String up_by;
	
	private String upd_dtimes;
	
	private String authTokenType;


	private Policies policies;
}
