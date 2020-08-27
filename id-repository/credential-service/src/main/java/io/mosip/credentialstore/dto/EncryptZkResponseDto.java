package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class EncryptZkResponseDto {
	
	private List<ZkDataAttribute> zkDataAttributes;
	private String encryptedRandomKey;
	private String rankomKeyIndex;
}
