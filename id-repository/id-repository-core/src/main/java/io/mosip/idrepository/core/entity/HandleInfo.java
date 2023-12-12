package io.mosip.idrepository.core.entity;

public interface HandleInfo extends UinInfo {

    public String getHandle();

    public void setHandle(String handle);

    public String getHandleHash();

    public void setHandleHash(String handleHash);
}
