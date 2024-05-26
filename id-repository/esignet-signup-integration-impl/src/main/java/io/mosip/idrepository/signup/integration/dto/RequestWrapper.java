package io.mosip.idrepository.signup.integration.dto;

import lombok.Data;

@Data
public class RequestWrapper<T> {

    private String id;
    private String version;
    private String requesttime;
    private T request;
}
