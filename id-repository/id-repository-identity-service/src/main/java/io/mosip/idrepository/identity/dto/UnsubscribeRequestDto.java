package io.mosip.idrepository.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnsubscribeRequestDto {

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Reason is mandatory")
    private String reason;

    private String comments;

    @NotBlank(message = "Consent is mandatory")
    private String consent; // must be "true"
}