package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.mosip.kernel.core.exception.ServiceError;
import lombok.Data;

/**
 * PMS partner-management service response envelope used when credential-service calls partner APIs.
 * <p>
 * Mirrors the kernel {@link io.mosip.kernel.core.http.ResponseWrapper} shape but carries a single
 * {@link ServiceError} in {@link #errors} instead of a list. Deserialized for partner certificate
 * download and similar PMS endpoints.
 * </p>
 *
 * @param <T> typed partner payload (e.g. {@link PartnerCertDownloadResponeDto})
 * @see PartnerGetCertificateResponseDto
 */
@Data
public class PartnerResponseWrapper<T> {

	/** MOSIP API id echoed from the partner service request. */
	private String id;

	/** API version echoed from the partner service request. */
	private String version;

	/** UTC timestamp when the partner service produced this response. */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	private LocalDateTime responsetime;

	/** Optional opaque metadata returned by the partner service. */
	private Object metadata;

	/** Successful partner payload; required when {@link #errors} is absent. */
	@NotNull
	@Valid
	private T response;

	/** Partner service error when the call failed; mutually exclusive with a populated {@link #response}. */
	private ServiceError errors;

}
