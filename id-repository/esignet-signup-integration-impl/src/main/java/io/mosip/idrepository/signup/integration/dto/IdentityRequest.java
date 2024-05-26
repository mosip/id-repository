package io.mosip.idrepository.signup.integration.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class IdentityRequest {

    private String registrationId;
    private JsonNode identity;
}
