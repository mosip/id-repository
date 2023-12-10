package io.mosip.idrepository.identity.entity;

import io.mosip.idrepository.core.entity.UinInfo;

public interface HandleInfo extends UinInfo {

    public String getHandle();

    public void setHandle(String handle);

    public String getHandleHash();

    public void setHandleHash(String handleHash);
}
