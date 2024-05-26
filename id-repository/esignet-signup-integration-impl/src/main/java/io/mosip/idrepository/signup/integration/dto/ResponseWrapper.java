package io.mosip.idrepository.signup.integration.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResponseWrapper<T> {

    private String id;
    private String version;
    private String responsetime;
    private String metadata;
    private T response;
    private List<Error> errors;
}
