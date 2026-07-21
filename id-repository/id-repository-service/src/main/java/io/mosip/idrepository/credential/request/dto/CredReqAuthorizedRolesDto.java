package io.mosip.idrepository.credential.request.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Keycloak role names authorized for credential-request REST endpoints.
 * <p>
 * Bound from {@code mosip.role.idrepo.credentialrequest.*} and referenced in
 * {@code @PreAuthorize} on {@link io.mosip.idrepository.credential.request.controller.CredentialRequestGeneratorController}.
 * </p>
 */
@Component("credReqAuthorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.credentialrequest")
@Getter
@Setter
public class CredReqAuthorizedRolesDto {

	/** Roles for {@code POST /v1/credentialrequest/requestgenerator}. */
	private List<String> postrequestgenerator;

	/** Roles for {@code POST /v1/credentialrequest/v2/requestgenerator/{rid}}. */
	private List<String> postv2requestgeneratorrid;

	/** Roles for {@code GET /v1/credentialrequest/cancel/{requestId}}. */
	private List<String> getcancelrequestid;

	/** Roles for {@code GET /v1/credentialrequest/get/{requestId}}. */
	private List<String> getgetrequestid;

	/** Roles for {@code GET /v1/credentialrequest/getRequestIds}. */
	private List<String> getgetrequestids;

	/** Roles for {@code PUT /v1/credentialrequest/retrigger/{requestId}}. */
	private List<String> putretriggerrequestid;
}
