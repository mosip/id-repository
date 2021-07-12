package io.mosip.idrepo.core.common.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo")
@Getter
@Setter
public class AuthorizedRolesDTO {

  //Idrepo controller
	
	private List<String> postidrepo;
	
    private List<String> getidvidid;	
	
	private List<String> patchidrepo; 
	 
	private List<String> getauthtypesstatusindividualidtypeindividualid;
	
	private List<String> postauthtypesstatus;
	
  //Vid controller
	
	private List<String> postvid;
	
	private List<String> getvid;
	
	private List<String> getviduin;
	
	private List<String> patchvid;
	
	private List<String> postvidregenerate;
	
	private List<String> postviddeactivate;
	
	private List<String> postvidreactivate;
	
	
}