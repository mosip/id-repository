package io.mosip.idrepository.identity.httpfilter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.httpfilter.BaseIdRepoFilter;

/**
 * The Class IdRepoFilter.
 *
 * @author Manoj SP
 */
@Component
public final class IdRepoFilter extends BaseIdRepoFilter {

	/* (non-Javadoc)
	 * @see io.mosip.idrepository.core.httpfilter.BaseIdRepoFilter#buildResponse(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected final String buildResponse(HttpServletRequest request) {
		return null;
	}

}
