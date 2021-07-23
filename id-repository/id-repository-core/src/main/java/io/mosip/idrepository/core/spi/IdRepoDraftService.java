package io.mosip.idrepository.core.spi;

import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * @author Manoj SP
 *
 * @param <REQUEST>
 * @param <RESPONSE>
 */
public interface IdRepoDraftService<REQUEST, RESPONSE> {

	public RESPONSE createDraft(String registrationId, String uin) throws IdRepoAppException;
	
	public RESPONSE updateDraft(String registrationId, REQUEST request) throws IdRepoAppException;
	
	public RESPONSE publishDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE discardDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE hasDraft(String registrationId) throws IdRepoAppException;
	
	public RESPONSE getDraft(String registrationId) throws IdRepoAppException;
}
