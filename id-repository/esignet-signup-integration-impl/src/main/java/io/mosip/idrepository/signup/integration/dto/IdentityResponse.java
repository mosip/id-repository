package io.mosip.idrepository.signup.integration.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class IdentityResponse {

    private String status;
    private JsonNode identity;
    private List<String> documents;
    private List<String> verifiedAttributes;
}
