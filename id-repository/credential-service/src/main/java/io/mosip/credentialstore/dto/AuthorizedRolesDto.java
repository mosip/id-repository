package io.mosip.credentialstore.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.credentialservice")
@Getter
@Setter
public class AuthorizedRolesDto {

   //credential store controller
	
	private List<String> postissue;
	
	//private List<String> getissuetypes;


}