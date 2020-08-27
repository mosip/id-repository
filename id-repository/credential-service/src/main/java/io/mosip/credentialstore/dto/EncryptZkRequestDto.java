package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class EncryptZkRequestDto  {

    private String id;
    private List<ZkDataAttribute> zkDataAttributes;
}
