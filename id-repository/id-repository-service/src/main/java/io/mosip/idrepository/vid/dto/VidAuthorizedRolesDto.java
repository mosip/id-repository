package io.mosip.idrepository.vid.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Keycloak role lists for VID REST endpoints ({@code mosip.role.idrepo.vid.*}).
 * <p>
 * Referenced by {@code @PreAuthorize} SpEL on
 * {@code io.mosip.idrepository.vid.controller.VidController}.
 * </p>
 */
@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.vid")
@Getter
@Setter
public class VidAuthorizedRolesDto {

	/** Roles allowed to create a new VID ({@code POST /vid}). */
	private List<String> postvid;

	/** Roles allowed to retrieve VIDs ({@code GET /vid}). */
	private List<String> getvid;

	/** Roles allowed to resolve UIN from VID ({@code GET /vid/uin}). */
	private List<String> getviduin;

	/** Roles allowed to update VID status ({@code PATCH /vid}). */
	private List<String> patchvid;

	/** Roles allowed to regenerate a VID ({@code POST /vid/regenerate}). */
	private List<String> postvidregenerate;

	/** Roles allowed to deactivate a VID ({@code POST /vid/deactivate}). */
	private List<String> postviddeactivate;

	/** Roles allowed to reactivate a VID ({@code POST /vid/reactivate}). */
	private List<String> postvidreactivate;

	/** Roles allowed to create a draft VID ({@code POST /draft/vid}). */
	private List<String> postdraftvid;
}
