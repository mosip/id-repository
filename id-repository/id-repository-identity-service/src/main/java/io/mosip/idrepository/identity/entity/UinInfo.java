package io.mosip.idrepository.identity.entity;

public interface UinInfo {

	public String getUin();

	public void setUin(String uin);

	public byte[] getUinData();

	public void setUinData(byte[] uinData);

	public String getUinHash();

	public void setUinHash(String hash);

}
