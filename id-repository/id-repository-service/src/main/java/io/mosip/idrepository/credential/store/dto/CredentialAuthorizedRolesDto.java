package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Keycloak role names authorized for credential store REST endpoints.
 * <p>
 * Bound from {@code mosip.role.idrepo.credentialservice.*} and referenced in
 * {@code @PreAuthorize} SpEL on {@link io.mosip.idrepository.credential.store.controller.CredentialStoreController}.
 * </p>
 */
@Component("credentialAuthorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.credentialservice")
@Getter
@Setter
public class CredentialAuthorizedRolesDto {

	/** Roles allowed to call {@code POST /v1/credentialservice/issue}. */
	private List<String> postissue;
}
