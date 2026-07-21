package io.mosip.idrepository.identity.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("identityAuthorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.identity")
@Getter
@Setter
public class IdentityAuthorizedRolesDto {

	/** Postidrepo (List<String>). */
	private List<String> postidrepo;
	
    /** Getidvidid (List<String>). */
    private List<String> getidvidid;

	/** Postidrepov2 (List<String>). */
	private List<String> postidrepov2;

	/** Postidvidid (List<String>). */
	private List<String> postidvidid;

	/** Patchidrepo (List<String>). */
	private List<String> patchidrepo;
	 
	/** Getauthtypesstatusindividualidtypeindividualid (List<String>). */
	private List<String> getauthtypesstatusindividualidtypeindividualid;
	
	/** Postauthtypesstatus (List<String>). */
	private List<String> postauthtypesstatus;

	/** Postdraftcreateregistration id (List<String>). */
	private List<String> postdraftcreateregistrationId;

	/** Patchdraftupdateregistration id (List<String>). */
	private List<String> patchdraftupdateregistrationId;

	/** Getdraftpublishregistration id (List<String>). */
	private List<String> getdraftpublishregistrationId;

	/** Deletedraftdiscardregistration id (List<String>). */
	private List<String> deletedraftdiscardregistrationId;

	/** Draftregistration id (List<String>). */
	private List<String> draftregistrationId;

	/** Getdraftregistration id (List<String>). */
	private List<String> getdraftregistrationId;

	/** Putdraftextractbiometricsregistration id (List<String>). */
	private List<String> putdraftextractbiometricsregistrationId;
	
	/** Get rid by individual id (List<String>). */
	private List<String> getRidByIndividualId;

	/** Post search id vid metadata (List<String>). */
	private List<String> postSearchIdVidMetadata;
	
	/** Remaining update count by individual id (List<String>). */
	private List<String> remainingUpdateCountByIndividualId;

	/** Getdraft uin (List<String>). */
	private List<String> getdraftUIN;

	/** Gethandleuin (List<String>). */
	private List<String> gethandleuin;

	/** Postidvididv2 (List<String>). */
	private List<String> postidvididv2;
}