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
	
}