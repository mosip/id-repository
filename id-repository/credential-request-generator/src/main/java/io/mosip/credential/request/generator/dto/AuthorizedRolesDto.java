package io.mosip.idrepo.core.common.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.idrepo.credentialrequest")
@Getter
@Setter
public class AuthorizedRolesDTO {

//Credential request genrator controller   
   private List<String> postrequestgenerator;
    
   private List<String> getcancelrequestid;

   private List<String> getgetrequestid;
	
   private List<String> getgetrequestids;
   
   private List<String> putretriggerrequestid;
	
}