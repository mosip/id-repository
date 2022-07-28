package io.mosip.idrepository.credentialsfeeder.entity;

import java.time.LocalDateTime;

public interface UinInfo {

	public String getUin();

	public void setUin(String uin);

	public byte[] getUinData();

	public void setUinData(byte[] uinData);

	public String getUinHash();

	public void setUinHash(String hash);
	
	public void setUpdatedBy(String updatedBy);
	
	public void setUpdatedDateTime(LocalDateTime updatedDTimes);

}
