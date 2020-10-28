package io.mosip.credentialstore.dto;

import lombok.Data;

@Data
public class AllowedAuthType {
 private String authType;
 private String authSubType;
 private boolean mandatory;
}
