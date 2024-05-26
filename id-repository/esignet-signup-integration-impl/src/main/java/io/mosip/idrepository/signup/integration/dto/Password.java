package io.mosip.idrepository.signup.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Password {

    private String hash;
    private String salt;

    @Data
    @AllArgsConstructor
    public static class PasswordPlaintext{
        private String inputData;
    }

    @Data
    public static class PasswordHash {
        private String hashValue;
        private String salt;
    }
}
