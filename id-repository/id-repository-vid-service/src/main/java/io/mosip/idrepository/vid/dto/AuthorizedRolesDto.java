package io.mosip.idrepository.vid.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.vid")
@Getter
@Setter
public class AuthorizedRolesDto {

  //Vid controller
	
	private List<String> postvid;
	
	private List<String> getvid;
	
	private List<String> getviduin;
	
	private List<String> patchvid;
	
	private List<String> postvidregenerate;
	
	private List<String> postviddeactivate;

	private List<String> postvidreactivate;

	private List<String> postdraftvid;
}