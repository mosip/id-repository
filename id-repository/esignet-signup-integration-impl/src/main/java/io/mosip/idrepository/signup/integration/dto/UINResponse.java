package io.mosip.idrepository.signup.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UINResponse {

    @JsonProperty("uin")
    private String UIN;
}
