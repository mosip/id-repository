package io.mosip.credentialstore.dto;

import lombok.Data;

@Data
public class Proof {
	
	
private String type;
private String created;
private String proofPurpose;
private String verificationMethod;
private String jws;


}
