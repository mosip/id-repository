package io.mosip.credentialstore.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.mosip.kernel.core.exception.ErrorResponse;
import io.mosip.kernel.core.exception.ServiceError;
import lombok.Data;

@Data
public class PartnerResponseWrapper<T> {
	
		private String id;
		private String version;
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		private LocalDateTime responsetime = LocalDateTime.now(ZoneId.of("UTC"));
		private Object metadata;
		@NotNull
		@Valid
		private T response;

		private ServiceError  errors;

	
}
