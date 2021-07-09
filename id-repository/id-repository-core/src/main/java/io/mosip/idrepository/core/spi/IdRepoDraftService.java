package io.mosip.idrepository.core.spi;

import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * @author Manoj SP
 *
 * @param <REQUEST>
 * @param <RESPONSE>
 */
public interface IdRepoDraftService<REQUEST, RESPONSE> {

	public RESPONSE createDraft(REQUEST request) throws IdRepoAppException;
	
	public RESPONSE updateDraft(REQUEST request) throws IdRepoAppException;
	
	public RESPONSE publishDraft(String regId) throws IdRepoAppException;
	
	public RESPONSE discardDraft(String regId) throws IdRepoAppException;
	
	public RESPONSE hasDraft(String regId) throws IdRepoAppException;
	
	public RESPONSE getDraft(String regId) throws IdRepoAppException;
}
