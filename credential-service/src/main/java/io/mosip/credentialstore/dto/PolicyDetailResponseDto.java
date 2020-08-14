package io.mosip.credentialstore.dto;



import lombok.Data;

@Data
public class PolicyDetailResponseDto {
	private String id;
	
	private String name;
	
	
	private String desc;
	
	private String publishDate;
	
	private String status;
	
	private String version;
	
	private String schema;
	
	private int is_Active;
	
	private String cr_by;
	
	private String cr_dtimes;
	
	private String up_by;
	
	private String upd_dtimes;
	

	private Policies policies;
}
