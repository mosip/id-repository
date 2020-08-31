package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;

/**
 * 
 * @author Dinesh Karuppiah.T
 *
 */
@Data
public class AuthtypeRequestDto {

	String individualId;
	String individualIdType;
	List<AuthtypeStatus> authtypes;

}
