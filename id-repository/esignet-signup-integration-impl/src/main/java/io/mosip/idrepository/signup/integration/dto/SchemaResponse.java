package io.mosip.idrepository.signup.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaResponse {

    private double idVersion;
    private String schemaJson;

    private JsonNode parsedSchemaJson;

}
