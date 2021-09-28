package io.mosip.idrepository.identity.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.identity")
@Getter
@Setter
public class AuthorizedRolesDto {

  //Idrepo controller
	
	private List<String> postidrepo;
	
    private List<String> getidvidid;	
	
	private List<String> patchidrepo; 
	 
	private List<String> getauthtypesstatusindividualidtypeindividualid;
	
	private List<String> postauthtypesstatus;

	private List<String> postdraftcreateregistrationId;

	private List<String> patchdraftupdateregistrationId;

	private List<String> getdraftpublishregistrationId;

	private List<String> deletedraftdiscardregistrationId;

	private List<String> draftregistrationId;

	private List<String> getdraftregistrationId;

	private List<String> putdraftextractbiometricsregistrationId;

}