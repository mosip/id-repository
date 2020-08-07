package io.mosip.credentialstore.dto;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PolicyDetailResponseDto {
     private String policyName;
	
	/** The valid for in minutes. */
	private int validForInMinutes;
	
	/** The transactions allowed. */
	private int transactionsAllowed;
	
	/** The extension allowed. */
	private boolean extensionAllowed;
	
	private boolean isEncryptionNeeded;
	
	private String shareDomain;
	
	private String sha256;
	
	private Date policyPublishDate;
	
	List<ShareableAttributeDto>  shareableAttributes;
}
