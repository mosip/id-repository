package io.mosip.idrepository.identity.httpfilter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.httpfilter.BaseIdRepoFilter;

/**
 * Servlet filter for identity HTTP APIs; extends {@link BaseIdRepoFilter} with identity-specific response shaping.
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.httpfilter.BaseIdRepoFilter
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
