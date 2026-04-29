package io.mosip.idrepository.core.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VerificationMetadata {

    private String trustFramework;
    private String verificationProcess;
    private List<String> claims;
    private Map<String, Object> metadata;
}
